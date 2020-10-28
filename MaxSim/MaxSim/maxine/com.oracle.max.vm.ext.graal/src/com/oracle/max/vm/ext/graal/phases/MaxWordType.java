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

import com.oracle.graal.api.meta.*;
import com.oracle.graal.debug.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.java.MethodCallTargetNode.InvokeKind;
import com.oracle.graal.nodes.type.*;
import com.oracle.graal.nodes.util.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.tiers.*;
import com.oracle.graal.phases.util.*;
import com.oracle.max.vm.ext.graal.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.*;


public class MaxWordType {

   /**
     * Sloppy programming an result in an "==" on {@code Word} types, which creates an {@link ObjectEqualsNode}.
     * If we rewrite the arguments, the node will then not verify. We could convert the node in {@link KindRewritePhase}, but
     * we'd prefer to force the code to be cleaned up.
     */
    @HOSTED_ONLY
    public static class CheckWordObjectEqualsPhase extends com.oracle.graal.phases.Phase {
        @Override
        protected void run(StructuredGraph graph) {
            for (Node n : GraphOrder.forwardGraph(graph)) {
                if (n instanceof ObjectEqualsNode) {
                    ObjectEqualsNode on = (ObjectEqualsNode) n;
                    if (isWord(on.x()) || isWord(on.y())) {
                        assert false : "== used on Word types, replace with Word.equals";
                    }
                }
            }
        }
    }


    /**
     * Removes the actual type of {@link Word} subclass instances and converts them to {@link #wordkind}.
     * This can only be done when the object'ness, e.g., method invocation, of the instance is no longer needed.
     * {@link PhiNode} instances that self-refer but otherwise have Word values are a problem as {@link PhiNode#inferPhiStamp()}
     * includes itself in the computation, causing a {@link Kind} mismatch, so they have to be treated specially.
     */
    @HOSTED_ONLY
    public static class KindRewriterPhase extends BasePhase<PhaseContext> {

        @Override
        protected void run(StructuredGraph graph, PhaseContext context) {
            for (Node n : GraphOrder.forwardGraph(graph)) {
                if (n instanceof ValueNode) {
                    ValueNode vn = (ValueNode) n;
                    if (isWord(vn)) {
                        changeToWord(vn, context);
                        for (Node usage : vn.usages()) {
                            if (usage instanceof PhiNode) {
                                PhiNode pn = (PhiNode) usage;
                                if (pn.stamp().kind() == Kind.Object && ((ObjectStamp) pn.stamp()).type() == null) {
                                    pn.setStamp(StampFactory.forKind(MaxGraal.runtime().maxTargetDescription().wordKind));
                                }
                            }
                        }
                    }
                }
            }
        }

        private void changeToWord(ValueNode valueNode, PhaseContext context) {
            if (valueNode.isConstant() && valueNode.asConstant().getKind() == Kind.Object) {
                StructuredGraph graph = valueNode.graph();
                ConstantNode constantNode = (ConstantNode) valueNode;
                Object asObject = constantNode.value.asObject();
                Constant constant;
                if (asObject instanceof WordUtil.WrappedWord) {
                    constant = ConstantMap.toGraal(((WordUtil.WrappedWord) asObject).archConstant());
                } else {
                    // TODO figure out how unwrapped Word constants can appear
                    assert MaxineVM.isHosted();
                    constant = Constant.forLong(((Word) asObject).value);
                }
                graph.replaceFloating(constantNode, ConstantNode.forConstant(constant, context.getRuntime(), graph));
            } else {
                valueNode.setStamp(StampFactory.forKind(MaxGraal.runtime().maxTargetDescription().wordKind));
            }
        }

    }

    /**
     * Remove unnecessary checks for {@code null} on {@link Word} subclasses
     * Also remove null checks on access to {@link Hub} and subclasses.
     * It is only called during snippet generation and boot image compilation.
     * These generally arise from inlining a method, where the JVM spec requires
     * an explicit null check to happen.
     *
     * One special case is after an inlining of method annotated with {@link INLINE}.
     * The null check is always removed as using {@link INLINE} is a
     * statement that no null check should be generated.
     */
    @HOSTED_ONLY
    public static class MaxNullCheckRewriterPhase extends Phase {

        /**
         * When non-null, indicates that the check is being run after inlining a method,
         * which typically introduces a null check as per the JVM spec. This is only
         * set in {@link MaxReplacements}.
         */
        Node invokePredecessor;

        public MaxNullCheckRewriterPhase setInvokePredecessor(Node invokePredecessor) {
            this.invokePredecessor = invokePredecessor;
            return this;
        }

        @Override
        protected void run(StructuredGraph graph) {
            for (Node n : GraphOrder.forwardGraph(graph)) {
                if (isGuardingPiNullCheck(n)) {
                    removeNullCheck(graph, (GuardingPiNode) n);
                }
            }
        }

        private boolean isGuardingPiNullCheck(Node n) {
            if (n instanceof GuardingPiNode) {
                GuardingPiNode fn = (GuardingPiNode) n;
                return fn.condition() instanceof IsNullNode;
            }
            return false;
        }

        private void removeNullCheck(StructuredGraph graph, GuardingPiNode guardingPiNode) {
            IsNullNode isNullNode = (IsNullNode) guardingPiNode.condition();
            ValueNode object = isNullNode.object();
            boolean removed;
            boolean typedRemove = false;
            boolean removeByInline = cameFromInlineMethod(graph, guardingPiNode);
            if (!removeByInline) {
                typedRemove = canRemove(object);
            }
            if (removeByInline || typedRemove) {
                assert guardingPiNode.object() == isNullNode.object();
                guardingPiNode.replaceAtUsages(isNullNode.object());
                graph.removeFixed(guardingPiNode);
                // If the IsNullNode is only used by this GuardingPiNode node we can also delete that.
                // N.B. The previous call will have removed the GuardingPiNode node from the IsNullNode usages
                if (isNullNode.usages().isEmpty()) {
                    GraphUtil.killWithUnusedFloatingInputs(isNullNode);
                }
                removed = true;
            } else {
                removed = false;
            }
            ResolvedJavaType objectType = ObjectStamp.typeOrNull(object);
            Debug.log("%s: nullCheck %s removed on %s (%s)", graph.method(), removed ? "" : "not", objectType == null ? object : objectType.getName(),
                            removed ? (removeByInline ? "INLINE" : "TYPE") : "");
        }

        private boolean cameFromInlineMethod(StructuredGraph graph, GuardingPiNode guardingPiNode) {
            if (invokePredecessor == null) {
                return false;
            }
            if (guardingPiNode.predecessor() == invokePredecessor) {
                return true;
            }
            return false;
        }

        /**
         * Check if it is safe to remove the null check.
         * @param object the object that is the subject of the check
         * @return
         */
        private boolean canRemove(ValueNode object) {
            ResolvedJavaType objectType = ObjectStamp.typeOrNull(object);
            // Check for a Word object, a Reference object, or a Hub or a nonNull object (can arise from runtime call rewrites)
            if (ObjectStamp.isObjectNonNull(object) || (objectType != null && (isWordOrReference(objectType) || MaxResolvedJavaType.getHubType().isAssignableFrom(objectType)))) {
                return true;
            } else if (object instanceof LoadFieldNode && ((LoadFieldNode) object).object() != null) {
                // field loads from Hub objects are guaranteed non-null
                ResolvedJavaType fieldObjectType = ObjectStamp.typeOrNull(((LoadFieldNode) object).object());
                return fieldObjectType ==  null ? false : MaxResolvedJavaType.getHubType().isAssignableFrom(fieldObjectType);
            } else if (isWordPhiReally(object)) {
                return true;
            } else {
                return false;
            }

        }

        /**
         * Checks for the special case of a PhiNode that refers to itself and has an indeterminate type, but all its
         * values are Word types. Have to handle about non-trivial cycles, unfortunately.
         * @param n
         * @return {@code true} iff all values (other than {@code this} are {@code Word} types.
         */
        private boolean isWordPhiReally(ValueNode n) {
            if (!(n instanceof ProxyNode || n instanceof PhiNode)) {
                return false;
            }
            visited = new NodeBitMap(n.graph());
            return isWordPhiReallyX(n);
        }

        private boolean isWordPhiReallyX(ValueNode n) {
            if (visited.isMarked(n)) {
                return false;
            } else {
                visited.mark(n);
            }
            if (n instanceof ProxyNode) {
                return isWordPhiReallyX(((ProxyNode) n).value());
            } else if (n instanceof PhiNode) {
                PhiNode pn = (PhiNode) n;
                if (pn.stamp().kind() == Kind.Object && ((ObjectStamp) pn.stamp()).type() == null) {
                    for (ValueNode vn : pn.values()) {
                        if (pn != vn && (isWord(vn) || isWordPhiReallyX(vn))) {
                            return true;
                        }
                    }
                }
                return false;
            } else {
                return false;
            }
        }

        private NodeBitMap visited;


    }

    /**
     * Finds {@link AccessNode} instances that have a chain of {@link MaxUnsafeCastNode}s starting with a
     * {@link Word} type that lead to a node whose type is {@link java.lang.Object}. Such nodes arise from the
     * explicit lowering in Maxine's reference scheme, object access logic. Not only are they redundant but
     * after the {@link Word} type nodes are rewritten as {@code long}, the objectness is lost, which can
     * cause problems with GC refmaps in the LIR stage.
     */
    @HOSTED_ONLY
    public static class MaxUnsafeAccessRewriterPhase extends Phase {

        @Override
        protected void run(StructuredGraph graph) {
            for (Node n : GraphOrder.forwardGraph(graph)) {
                if (n instanceof AccessNode) {
                    ValueNode object = ((AccessNode) n).object();
                    if (object instanceof MaxUnsafeCastNode && object.kind() == Kind.Object) {
                        MaxUnsafeCastNode mn = (MaxUnsafeCastNode) object;
                        if (MaxResolvedJavaType.getWordType().isAssignableFrom(ObjectStamp.typeOrNull(mn))) {
                            ValueNode endChain = findEndChainOfMaxUnsafeCastNode(mn);
                            if (endChain.kind() == Kind.Object && ObjectStamp.typeOrNull(endChain) == MaxResolvedJavaType.getJavaLangObject()) {
                                deleteChain(graph, mn, endChain);
                            }
                        }
                    }

                }
            }
        }

        private ValueNode findEndChainOfMaxUnsafeCastNode(MaxUnsafeCastNode n) {
            MaxUnsafeCastNode nn = n;
            while (nn.object() instanceof MaxUnsafeCastNode) {
                nn = (MaxUnsafeCastNode) nn.object();
            }
            return nn.object();
        }

        private void deleteChain(StructuredGraph graph, MaxUnsafeCastNode n, ValueNode end) {
            if (n.object() != end) {
                deleteChain(graph, (MaxUnsafeCastNode) n.object(), end);
            }
            graph.replaceFloating(n, n.object());
        }

    }

    /**
     * Ensures that all non-final methods on {@link Word} subclasses are treated as final (aka {@link InvokeKind.Special}),
     * to ensure that they are inlined by {@link SnippetInstaller}.
     */
    @HOSTED_ONLY
    public static class MakeWordFinalRewriterPhase extends com.oracle.graal.phases.Phase {

        @Override
        protected void run(StructuredGraph graph) {
            // make sure all Word method invokes are Special else SnippetInstaller won't inline them
            for (MethodCallTargetNode callTargetNode : graph.getNodes(MethodCallTargetNode.class)) {
                if (callTargetNode.invokeKind() == InvokeKind.Virtual && callTargetNode.arguments().get(0) != null && isWord(callTargetNode.arguments().get(0))) {
                    Debug.log("changing invoke kind for %s to Special", callTargetNode.targetMethod());
                    callTargetNode.setInvokeKind(InvokeKind.Special);
                }
            }
        }
    }

    /**
     * Replaces any invokes of {@link Accessor} methods with the appropriate resolved method.
     * N.B. This can only happen when we actually know the concrete receiver type, so typically after
     * the invoke has been inlined into a caller using either a {@link Pointer} or {@link Reference} type receiver.
     */
    @HOSTED_ONLY
    public static class ReplaceAccessorPhase extends com.oracle.graal.phases.Phase {

        @Override
        protected void run(StructuredGraph graph) {
            for (MethodCallTargetNode callTargetNode : graph.getNodes(MethodCallTargetNode.class)) {
                if (callTargetNode.invokeKind() == InvokeKind.Interface && callTargetNode.isResolved() && callTargetNode.targetMethod().getDeclaringClass() == MaxResolvedJavaType.getAccessorType()) {
                    ObjectStamp stamp = (ObjectStamp) callTargetNode.receiver().stamp();
                    if (stamp.type() != MaxResolvedJavaType.getAccessorType()) {
                        ResolvedJavaMethod resolvedMethod = stamp.type().resolveMethod(callTargetNode.targetMethod());
                        assert resolvedMethod != null;
                        callTargetNode.setTargetMethod(resolvedMethod);
                        callTargetNode.setInvokeKind(InvokeKind.Special);
                        Debug.log("resolved Accessor method %s to %s", callTargetNode.targetMethod(), resolvedMethod);
                    }
                }
            }
        }
    }

    public static boolean isWord(ValueNode node) {
        if (node.stamp() == StampFactory.forWord()) {
            return true;
        }
        if (node.kind() == Kind.Object && ObjectStamp.typeOrNull(node) != null) {
            ResolvedJavaType type = ObjectStamp.typeOrNull(node);
            if (node instanceof ConstantNode && type == MaxResolvedJavaType.getWrappedWordType()) {
                return true;
            } else {
                return isMaxineWordType(type);
            }
        }
        return false;
    }

    private static boolean isMaxineWordType(ResolvedJavaType type) {
        RiType riType = MaxResolvedJavaType.getRiType(type);
        if (!(riType instanceof ClassActor)) {
            return false;
        }
        ClassActor actor = (ClassActor) riType;
        return actor.kind == com.sun.max.vm.type.Kind.WORD;
    }

    public static Kind checkWord(JavaType javaType) {
        if (javaType instanceof ResolvedJavaType && isMaxineWordType((ResolvedJavaType) javaType)) {
            return Kind.Long;
        } else {
            return javaType.getKind();
        }
    }

    public static boolean isWordOrReference(ResolvedJavaType objectType) {
        return objectType == MaxResolvedJavaType.getReferenceType() ||
                        objectType == MaxResolvedJavaType.getCodePointerType() ||
                        MaxResolvedJavaType.getWordType().isAssignableFrom(objectType);
    }


}
