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
package com.oracle.max.vm.ext.graal.phases;

import java.util.*;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.replacements.*;
import com.oracle.max.vm.ext.graal.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.runtime.*;

/**
 * Maxine boot image compilation needs {@link @Fold} support, which is normally
 * restricted to snippets in Graal. We can't just use the snippets {@link NodeIntrinsification}
 * phase because there can be unresolved {@link HOSTED_ONLY} methods in the graph.
 *
 * Also Maxine tags some methods as foldable (e.g. hidden SWITCH_TABLE methods) as foldable,
 * but they are not annotated, so we need a Maxine-specific check.
 *
 * N.B. Not all foldable methods fold successfully, which is ok, they just get compiled.
 */
public class MaxFoldPhase extends NodeIntrinsificationPhase {

    public MaxFoldPhase(MetaAccessProvider runtime) {
        super(runtime);
    }

    @Override
    protected void run(StructuredGraph graph) {
        // Identical to NodeIntrinsificationPhase.run, save for the check on unresolved methods and
        // Maxine-specific fold check.
        ArrayList<Node> cleanUpReturnList = new ArrayList<>();
        for (MethodCallTargetNode node : graph.getNodes(MethodCallTargetNode.class)) {
            if (node.isResolved()) {
                // Always true for snippets
                try {
                    tryIntrinsify(node, cleanUpReturnList);
                } catch (Throwable ex) {
                    FatalError.breakpoint();
                }
            }
        }

        for (Node node : cleanUpReturnList) {
            cleanUpReturnCheckCast(node);
        }
    }

    @Override
    protected boolean isFoldable(ResolvedJavaMethod method) {
        MethodActor cma = (MethodActor) MaxResolvedJavaMethod.getRiResolvedMethod(method);
        return MethodActor.isDeclaredFoldable(cma.flags()) || super.isFoldable(method);
    }

}
