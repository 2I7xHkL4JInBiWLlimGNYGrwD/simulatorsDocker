/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.max.vm.ext.graal.snippets;

import java.lang.reflect.*;
import java.util.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.debug.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.java.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.common.*;
import com.oracle.graal.phases.tiers.*;
import com.oracle.graal.replacements.*;
import com.oracle.max.vm.ext.graal.*;
import com.oracle.max.vm.ext.graal.phases.*;
import com.sun.max.program.*;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.actor.member.*;

import static com.sun.max.vm.MaxineVM.vm;

/**
 * Customized snippet building for Maxine.
  */
public class MaxReplacementsImpl extends ReplacementsImpl {
    public static class MaxSnippetGraphBuilderConfiguration extends GraphBuilderConfiguration {

        boolean ignoreHostOnlyError;

        public MaxSnippetGraphBuilderConfiguration() {
            super(true, true);
        }

        @Override
        public boolean unresolvedIsError() {
            // This prevents an assertion error in GraphBuilderPhase when we return an unresolved field
            return false;
        }

    }

    private MaxSnippetGraphBuilderConfiguration maxSnippetGraphBuilderConfiguration;
    Map<Class< ? extends Node>, LoweringProvider> lowerings;

    private final PhaseContext phaseContext;

    MaxReplacementsImpl(MetaAccessProvider runtime, Assumptions assumptions, TargetDescription target,
                    MaxSnippetGraphBuilderConfiguration maxSnippetGraphBuilderConfiguration) {
        super(runtime, assumptions, target);
        this.maxSnippetGraphBuilderConfiguration = maxSnippetGraphBuilderConfiguration;
        this.lowerings = ((MaxRuntime) runtime).lowerings();
        this.phaseContext = new PhaseContext(runtime, assumptions, this);
    }

    public StructuredGraph getSnippet(ResolvedJavaMethod method) {
        assert !Modifier.isAbstract(method.getModifiers()) && !Modifier.isNative(method.getModifiers()) : "Snippet must not be abstract or native";

        StructuredGraph graph = graphs.get(method);
        if (graph == null) {
            graphs.putIfAbsent(method, makeGraph(method, null, inliningPolicy(method)));
            graph = graphs.get(method);
        }
        return graph;
    }

    public void installAndRegisterSnippets(Class< ? extends SnippetLowerings> clazz) {
        // assumption is that it is ok to register the lowerings incrementally
        try {
            Constructor< ? > cons = clazz.getDeclaredConstructor(MetaAccessProvider.class, Replacements.class, TargetDescription.class, Map.class);
            Object[] args = new Object[] {runtime, this, target, lowerings};
            SnippetLowerings snippetLowerings = (SnippetLowerings) cons.newInstance(args);
            snippetLowerings.registerLowerings(runtime, this, target, lowerings);
        } catch (Exception ex) {
            ProgramError.unexpected("MaxReplacementsImpl: problem: " + ex + " in: " + clazz);
        }
    }

    public Map<Class< ? extends Node>, LoweringProvider> lowerings() {
        return lowerings;
    }

    @Override
    protected GraphMaker createGraphMaker(ResolvedJavaMethod substitute, ResolvedJavaMethod original) {
        return new MaxGraphMaker(substitute, original);
    }

    protected class MaxGraphMaker extends ReplacementsImpl.GraphMaker {

        protected MaxGraphMaker(ResolvedJavaMethod substitute, ResolvedJavaMethod original) {
            super(substitute, original);
        }

        @Override
        protected StructuredGraph buildInitialGraph(final ResolvedJavaMethod method) {
            final StructuredGraph graph = new StructuredGraph(method);

            GraphBuilderPhase graphBuilder = new GraphBuilderPhase(runtime, maxSnippetGraphBuilderConfiguration, OptimisticOptimizations.NONE);
            graphBuilder.apply(graph);

            Debug.dump(graph, "%s: %s", method.getName(), GraphBuilderPhase.class.getSimpleName());

            if (vm().phase != MaxineVM.Phase.RUNNING) {
                new MaxWordType.MakeWordFinalRewriterPhase().apply(graph);
            }
            new MaxFoldPhase(runtime).apply(graph);
            // need constant propagation for folded methods
            new CanonicalizerPhase.Instance(runtime, assumptions, MaxGraal.canonicalizeReads).apply(graph);
            new MaxIntrinsicsPhase().apply(graph);

            return graph;
        }

        @Override
        protected Object beforeInline(MethodCallTargetNode callTarget, StructuredGraph calleeGraph) {
            MethodActor callee = (MethodActor) MaxResolvedJavaMethod.getRiResolvedMethod(callTarget.targetMethod());
            if (callee.isInline()) {
                return callTarget.invoke().predecessor();
            } else {
                return null;
            }
        }

        @Override
        public void afterInline(StructuredGraph caller, StructuredGraph callee, Object beforeInlineData) {
            if (vm().phase != MaxineVM.Phase.RUNNING) {
                new MaxWordType.MaxNullCheckRewriterPhase().setInvokePredecessor((Node) beforeInlineData).apply(caller);
                new MaxWordType.ReplaceAccessorPhase().apply(caller);
            }
            new CanonicalizerPhase.Instance(runtime, assumptions, MaxGraal.canonicalizeReads).apply(caller);
            new MaxIntrinsicsPhase().apply(caller);
        }

        @Override
        protected void finalizeGraph(StructuredGraph graph) {
            new MaxFoldPhase(runtime).apply(graph);

            if (vm().phase != MaxineVM.Phase.RUNNING) {
                // These only get done once right at the end
                new MaxWordType.MaxUnsafeAccessRewriterPhase().apply(graph);
                new MaxSlowpathRewriterPhase(runtime).apply(graph);
                new MaxWordType.MaxNullCheckRewriterPhase().apply(graph);

                // The replaces all Word based types with target.wordKind
                new MaxWordType.KindRewriterPhase().apply(graph, phaseContext);
            }
            // Remove UnsafeCasts rendered irrelevant by previous phase
            new CanonicalizerPhase.Instance(runtime, assumptions, MaxGraal.canonicalizeReads).apply(graph);

            new SnippetFrameStateCleanupPhase().apply(graph);
            new DeadCodeEliminationPhase().apply(graph);

            new InsertStateAfterPlaceholderPhase().apply(graph);
        }

        @Override
        public void afterInlining(StructuredGraph graph) {
            new MaxFoldPhase(runtime).apply(graph);

            new DeadCodeEliminationPhase().apply(graph);
            new CanonicalizerPhase.Instance(runtime, assumptions, MaxGraal.canonicalizeReads).apply(graph);

        }
    }

}
