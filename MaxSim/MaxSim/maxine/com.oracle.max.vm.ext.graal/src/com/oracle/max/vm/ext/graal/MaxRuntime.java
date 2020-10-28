/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal;

import static com.oracle.max.vm.ext.graal.MaxGraal.unimplemented;
import static com.sun.max.vm.MaxineVM.*;

import java.lang.reflect.*;
import java.util.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.code.CodeUtil.RefMapFormatter;
import com.oracle.graal.api.code.CompilationResult.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.printer.*;
import com.oracle.max.vm.ext.graal.snippets.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.object.*;

public class MaxRuntime implements GraalCodeCacheProvider {

    private final Map<Class< ? extends Node>, LoweringProvider> lowerings = new HashMap<>();

    private final MaxTargetDescription maxTargetDescription;

    MaxRuntime(MaxTargetDescription maxTargetDescription) {
        this.maxTargetDescription = maxTargetDescription;
    }

    public Map<Class< ? extends Node>, LoweringProvider> lowerings() {
        return lowerings;
    }

    public MaxTargetDescription maxTargetDescription() {
        return maxTargetDescription;
    }

    @Override
    public InstalledCode addMethod(ResolvedJavaMethod method, CompilationResult compResult) {
        unimplemented("addMethod");
        return null;
    }

    @Override
    public String disassemble(CompilationResult tm, InstalledCode installedCode) {
        byte[] code = installedCode == null ? Arrays.copyOf(tm.getTargetCode(), tm.getTargetCodeSize()) : installedCode.getCode();
        long start = installedCode == null ? 0L : installedCode.getStart();
        HexCodeFile hcf = new HexCodeFile(code, start, maxTargetDescription.arch.getName(), maxTargetDescription.wordSize * 8);
        if (tm != null) {
            HexCodeFile.addAnnotations(hcf, tm.getAnnotations());
            addExceptionHandlersComment(tm, hcf);
            Register fp = RegisterMap.toGraal(vm().registerConfigs.standard.frame);
            RefMapFormatter slotFormatter = new RefMapFormatter(maxTargetDescription.arch, maxTargetDescription.wordSize, fp, 0);
            for (Infopoint infopoint : tm.getInfopoints()) {
                if (infopoint instanceof Call) {
                    Call call = (Call) infopoint;
                    if (call.debugInfo != null) {
                        hcf.addComment(call.pcOffset + call.size, CodeUtil.append(new StringBuilder(100), call.debugInfo, slotFormatter).toString());
                    }
                } else {
                    if (infopoint.debugInfo != null) {
                        hcf.addComment(infopoint.pcOffset, CodeUtil.append(new StringBuilder(100), infopoint.debugInfo, slotFormatter).toString());
                    }
                    addOperandComment(hcf, infopoint.pcOffset, "{safepoint}");
                }
            }
            for (DataPatch site : tm.getDataReferences()) {
                addOperandComment(hcf, site.pcOffset, "{" + site.constant + "}");
            }
        }
        return hcf.toEmbeddedString();
    }

    private static void addExceptionHandlersComment(CompilationResult tm, HexCodeFile hcf) {
        if (!tm.getExceptionHandlers().isEmpty()) {
            String nl = HexCodeFile.NEW_LINE;
            StringBuilder buf = new StringBuilder("------ Exception Handlers ------").append(nl);
            for (CompilationResult.ExceptionHandler e : tm.getExceptionHandlers()) {
                buf.append("    ").append(e.pcOffset).append(" -> ").append(e.handlerPos).append(nl);
                hcf.addComment(e.pcOffset, "[exception -> " + e.handlerPos + "]");
                hcf.addComment(e.handlerPos, "[exception handler for " + e.pcOffset + "]");
            }
            hcf.addComment(0, buf.toString());
        }
    }

    private static void addOperandComment(HexCodeFile hcf, int pos, String comment) {
        String oldValue = hcf.addOperandComment(pos, comment);
        if (oldValue != null) {
            hcf.addOperandComment(pos, oldValue + " " + comment);
        }
    }

    @Override
    public RegisterConfig lookupRegisterConfig() {
        // Maxine has different register configs for VM_ENTRY_POINT methods and "standard" methods,
        // which must be addressed when Graal is used to compile the boot image.
        ClassMethodActor cma = MaxGraal.methodBeingCompiled();
        if (cma == null) {
            return MaxRegisterConfig.get(vm().registerConfigs.standard);
        } else {
            return MaxRegisterConfig.get(vm().registerConfigs.getRegisterConfig(cma));
        }
    }

    @Override
    public int getMinimumOutgoingSize() {
        return maxTargetDescription.wordSize; // for deopt
    }

    @Override
    public ForeignCallLinkage lookupForeignCall(ForeignCallDescriptor descriptor) {
        return MaxForeignCallsMap.get(descriptor);
    }

    @Override
    public int encodeDeoptActionAndReason(DeoptimizationAction action, DeoptimizationReason reason) {
        unimplemented("encodeDeoptActionAndReason");
        return 0;
    }

    @Override
    public boolean needsDataPatch(Constant constant) {
        // TODO when might this be true?
        return false;
    }

    @Override
    public ResolvedJavaType lookupJavaType(Class< ? > clazz) {
        return MaxResolvedJavaType.get(ClassActor.fromJava(clazz));
    }

    @Override
    public ResolvedJavaMethod lookupJavaMethod(Method reflectionMethod) {
        return MaxResolvedJavaMethod.get(MethodActor.fromJava(reflectionMethod));
    }

    @Override
    public ResolvedJavaField lookupJavaField(Field reflectionField) {
        return MaxResolvedJavaField.get(FieldActor.fromJava(reflectionField));
    }

    @Override
    public ResolvedJavaType lookupJavaType(Constant constant) {
        if (constant.getKind() == Kind.Object) {
            Object o = constant.asObject();
            if (o != null) {
                // StaticTuples (which are constants) are a special case.
                if (MaxineVM.isHosted()) {
                   // When running isHosted, StaticTuple is HOSTED_ONLY
                   // so shows up as null, which causes an assertion failure later
                    Class<?> klass = o.getClass();
                    if (klass == StaticTuple.class) {
                        return MaxResolvedJavaType.getJavaLangObject();
                    } else {
                        return MaxResolvedJavaType.get(ClassActor.fromJava(klass));
                    }
                } else {
                    // Maxine's getClass() on a static tuple returns the class corresponding to the static tuple.
                    // If the class happens to be abstract, then ObjectStamp will object when the ConstantNode
                    // is created (e.g. when lowering an AccessFieldNode). The type really doesn't matter anyway.
                    Hub hub = ObjectAccess.readHub(o);
                    if (hub instanceof StaticHub) {
                        return MaxResolvedJavaType.getJavaLangObject();
                    } else {
                        return MaxResolvedJavaType.get(hub.classActor);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean constantEquals(Constant x, Constant y) {
        return x.equals(y);
    }

    @Override
    public ResolvedJavaMethod lookupJavaConstructor(Constructor reflectionConstructor) {
        unimplemented("lookupJavaConstructor");
        return null;
    }

    @Override
    public int lookupArrayLength(Constant array) {
        return Array.getLength(array.asObject());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void lower(Node n, LoweringTool tool) {
        LoweringProvider lowering = lowerings.get(n.getClass());
        if (lowering == null) {
            missingLowering(n);
        }
        lowering.lower(n, tool);
    }

    @NEVER_INLINE
    private static void missingLowering(Node n) {
        ProgramError.unexpected("missing lowering for node: " + n.getClass().getSimpleName());
    }

    @Override
    public Signature parseMethodDescriptor(String methodDescriptor) {
        MaxGraal.unimplemented("parseMethodDescriptor");
        return null;
    }

    @Override
    public TargetDescription getTarget() {
        return maxTargetDescription;
    }

    @Override
    public Constant readUnsafeConstant(Kind kind, Object base, long displacement, boolean compressedPointer) {
        // TODO
        MaxGraal.unimplemented("MaxRuntime.readUnsafeConstant");
        return null;
    }

    @Override
    public InstalledCode addMethod(ResolvedJavaMethod method, CompilationResult compResult, Graph graph) {
        MaxGraal.unimplemented("MaxRuntime.addMethod");
        return null;
    }

    @Override
    public boolean isReexecutable(ForeignCallDescriptor descriptor) {
        return MaxForeignCallsMap.get(descriptor).isReexecutable();
    }

    @Override
    public LocationIdentity[] getKilledLocations(ForeignCallDescriptor descriptor) {
        return MaxForeignCallsMap.get(descriptor).getKilledLocations();
    }

    @Override
    public boolean canDeoptimize(ForeignCallDescriptor descriptor) {
        return MaxForeignCallsMap.get(descriptor).canDeoptimize();
    }

    public ValueNode reconstructArrayIndex(LocationNode location) {
        MaxGraal.unimplemented("MaxRuntime.reconstructArrayIndex");
        return null;
    }

    public Register heapBaseRegister() {
        // Only used for compressed pointers, which Maxine does not support
        return null;
    }

}
