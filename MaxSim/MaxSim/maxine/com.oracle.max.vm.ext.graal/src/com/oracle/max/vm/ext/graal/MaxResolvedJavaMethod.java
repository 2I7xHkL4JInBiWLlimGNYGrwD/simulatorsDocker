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

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.phases.GraalOptions;
import com.sun.cri.ci.CiConstant;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.RuntimeCompiler;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.profile.MethodProfile;

/**
 * Likely temporary indirect between a {@link MethodActor} and a {@link ResolvedJavaMethod},
 * since {@code MethodActor} already implements the old {@link RiResolvedMethod} interface.
 */
public class MaxResolvedJavaMethod extends MaxJavaMethod implements ResolvedJavaMethod {

    protected MaxResolvedJavaMethod(RiResolvedMethod riResolvedMethod) {
        super(riResolvedMethod);
    }

    RiResolvedMethod riResolvedMethod() {
        return (RiResolvedMethod) riMethod;
    }

    public static MaxResolvedJavaMethod get(RiResolvedMethod riMethod) {
        return (MaxResolvedJavaMethod) MaxJavaMethod.get(riMethod);
    }

    public static RiResolvedMethod getRiResolvedMethod(ResolvedJavaMethod resolvedJavaMethod) {
        return (RiResolvedMethod) MaxJavaMethod.getRiMethod(resolvedJavaMethod);
    }

    @Override
    public byte[] getCode() {
        MethodActor ma = (MethodActor) riResolvedMethod();
        /*
         * Native methods may have been SUBSTITUTEd in which case we want to return the substituted
         * bytecodes and "code" handles that. However, in the mixed C1X/Graal world, a native method
         * also may be implemented by a bytecode stub which does not verify, so can't use ma.compilee() != ma
         * to detect substitution.
         */
        if (ma.isNative() && METHOD_SUBSTITUTIONS.Static.findSubstituteFor((ClassMethodActor) ma) == null) {
            return null;
        }
        return ma.code();
    }

    @Override
    public int getCodeSize() {
        return riResolvedMethod().codeSize();
    }

    @Override
    public int getCompiledCodeSize() {
        TargetMethod tm = ((ClassMethodActor) riResolvedMethod()).currentTargetMethod();
        if (tm == null) {
            return 0;
        } else {
            return tm.codeLength();
        }
    }

    @Override
    public ResolvedJavaType getDeclaringClass() {
        return MaxResolvedJavaType.get(riResolvedMethod().holder());
    }

    @Override
    public int getMaxLocals() {
        return riResolvedMethod().maxLocals();
    }

    @Override
    public int getMaxStackSize() {
        return riResolvedMethod().maxStackSize();
    }

    @Override
    public int getModifiers() {
        return riResolvedMethod().accessFlags();
    }

    @Override
    public boolean isClassInitializer() {
        return riResolvedMethod().isClassInitializer();
    }

    @Override
    public boolean isConstructor() {
        return riResolvedMethod().isConstructor();
    }

    @Override
    public boolean canBeStaticallyBound() {
        return riResolvedMethod().canBeStaticallyBound();
    }

    @Override
    public ExceptionHandler[] getExceptionHandlers() {
        RiExceptionHandler[] riExHandlers = riResolvedMethod().exceptionHandlers();
        ExceptionHandler[] exHandlers = new ExceptionHandler[riExHandlers.length];
        for (int i = 0; i < riExHandlers.length; i++) {
            RiExceptionHandler riEx = riExHandlers[i];
            exHandlers[i] = new ExceptionHandler(riEx.startBCI(), riEx.endBCI(), riEx.handlerBCI(),
                            riEx.catchTypeCPI(), MaxJavaType.get(riEx.catchType()));
        }
        return exHandlers;
    }

    @Override
    public StackTraceElement asStackTraceElement(int bci) {
        ClassMethodActor cma = (ClassMethodActor) riMethod;
        return new StackTraceElement(cma.format("%H"), riResolvedMethod().name(), cma.sourceFileName(),
                        cma.codeAttribute() == null ? -1 : cma.codeAttribute().lineNumberTable().findLineNumber(bci));
    }

    private final ThreadLocal<Boolean> ignoreProfilingInfo = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    public boolean isOptimizedMethodInBootCodeRegion() {
        ClassMethodActor cma = (ClassMethodActor) riResolvedMethod();
        TargetMethod tm = Compilations.currentTargetMethod(cma.compiledState, RuntimeCompiler.Nature.OPT);
        if (tm == null) {
            return false;
        }
        return tm.isInBootCodeRegion();
    }

    public void ignoreProfilingInfo() {
        ignoreProfilingInfo.set(true);
    }

    public void utilizeProfilingInfo() {
        ignoreProfilingInfo.set(false);
    }

    public boolean isProfilingInfoIgnored() {
        return ignoreProfilingInfo.get();
    }

    public MethodProfile getBaselineMethodProfilingInfo() {
        if (isProfilingInfoIgnored()) {
            return null;
        }
        ClassMethodActor cma = (ClassMethodActor) riResolvedMethod();
        TargetMethod tm = Compilations.currentTargetMethod(cma.compiledState, RuntimeCompiler.Nature.BASELINE);
        if (tm == null || tm.profile() == null || tm.profile().rawData() == null) {
            return null;
        }
        return tm.profile();
    }

    @Override
    public ProfilingInfo getProfilingInfo() {
        ClassMethodActor ma = (ClassMethodActor) riResolvedMethod();
        ProfilingInfo info;
        MethodProfile methodProfile = null;

        if (GraalOptions.UseProfilingInformation.getValue()) {
            methodProfile = getBaselineMethodProfilingInfo();
        }

        if (methodProfile == null) {
            info = DefaultProfilingInfo.get(ProfilingInfo.TriState.FALSE);
        } else {
            info = new MaxProfilingInfo(methodProfile, this);
        }
        return info;
    }

    private final Map<Object, Object> compilerStorage = new ConcurrentHashMap<Object, Object>();

    @Override
    public Map<Object, Object> getCompilerStorage() {
        return compilerStorage;
    }

    @Override
    public ConstantPool getConstantPool() {
        return MaxConstantPool.get(riResolvedMethod().getConstantPool());
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return riResolvedMethod().getAnnotation(annotationClass);
    }

    @Override
    public Annotation[][] getParameterAnnotations() {
        return riResolvedMethod().getParameterAnnotations();
    }

    @Override
    public Type[] getGenericParameterTypes() {
        return riResolvedMethod().getGenericParameterTypes();
    }

    @Override
    public boolean canBeInlined() {
        boolean result = true;
        // Maxine specific constraints on inlining
        ClassMethodActor ma = ((ClassMethodActor) riResolvedMethod()).compilee();
        assert ma != null;
        if (ma.isIntrinsic() || ma.isNeverInline()) {
            result = false;
        } else if (!MaxGraal.bootCompile() && (ma.isVM())) {
            // This can happen when we hit a JDK method that has been SUBSTITUTEd
            result = checkInline(ma);
        }
        return result;
    }

    /**
     * In a non-boot compilation, checks whether method {@code ma}, which {@link MethodActor#isVM()}, can be inlined.
     * @param ma
     * @return
     */
    private boolean checkInline(MethodActor ma) {
        if (MaxineVM.isHosted()) {
            // ma.isVM() == true even for tests when hosted, so this allows test methods to inline
            // return !ma.holder().name().startsWith("com");
            return false;
        } else {
            // Currently no VM methods can be inlined as they might require boot compiler phases.
            // This may not be the case for all, so we could list exceptions here.
            return false;
        }
    }

    @Override
    public String toString() {
        return riMethod.toString();
    }

    @RESET
    private static Map<RiMethod, LineNumberTable> lineNumberTableMap;

    private static Map<RiMethod, LineNumberTable>  getLineNumberTableMap() {
        if (lineNumberTableMap == null) {
            lineNumberTableMap = new HashMap<>();
        }
        return lineNumberTableMap;
    }

    private static class LineNumberTableImpl implements LineNumberTable {
        int[] lineNumberEntries;
        int[] bciEntries;
        com.sun.max.vm.classfile.LineNumberTable maxLnt;

        LineNumberTableImpl(com.sun.max.vm.classfile.LineNumberTable maxLnt) {
            this.maxLnt = maxLnt;
            com.sun.max.vm.classfile.LineNumberTable.Entry[] entries = maxLnt.entries();
            lineNumberEntries = new int[entries.length];
            bciEntries = new int[entries.length];
            int i = 0;
            for (com.sun.max.vm.classfile.LineNumberTable.Entry entry : entries) {
                lineNumberEntries[i] = entry.lineNumber();
                bciEntries[i] = entry.bci();
                i++;
            }
        }

        @Override
        public int[] getLineNumberEntries() {
            return lineNumberEntries;
        }

        @Override
        public int[] getBciEntries() {
            return bciEntries;
        }

        @Override
        public int getLineNumber(int bci) {
            return maxLnt.findLineNumber(bci);
        }

    }

    @Override
    public LineNumberTable getLineNumberTable() {
        LineNumberTable lnt = getLineNumberTableMap().get(riMethod);
        if (lnt == null) {
            ClassMethodActor cma = (ClassMethodActor) riResolvedMethod();
            com.sun.max.vm.classfile.LineNumberTable maxLnt = cma.codeAttribute().lineNumberTable();
            lnt = new LineNumberTableImpl(maxLnt);
            lineNumberTableMap.put(riMethod, lnt);
        }
        return lnt;
    }

    @RESET
    private static Map<RiMethod, LocalVariableTable> localVariableTableMap;

    private static Map<RiMethod, LocalVariableTable> getLocalVariableTableMap() {
        if (localVariableTableMap == null) {
            localVariableTableMap = new HashMap<>();
        }
        return localVariableTableMap;
    }

    private static class LocalVariableTableImpl implements LocalVariableTable {

        private class LocalImpl implements Local {

            com.sun.max.vm.classfile.LocalVariableTable.Entry entry;

            LocalImpl(com.sun.max.vm.classfile.LocalVariableTable.Entry entry) {
                this.entry = entry;
            }

            @Override
            public int getStartBCI() {
                return entry.startBCI();
            }

            @Override
            public int getEndBCI() {
                /// TODO
                MaxGraal.unimplemented("Local.getEndBCI");
                return 0;
            }

            @Override
            public int getSlot() {
                return entry.slot();
            }

            @Override
            public String getName() {
                return entry.name(constantPool).string;
            }

            @Override
            public ResolvedJavaType getType() {
                /// TODO
                MaxGraal.unimplemented("Local.getType");
                return null;
            }

        }

        private com.sun.max.vm.classfile.constant.ConstantPool constantPool;
        private com.sun.max.vm.classfile.LocalVariableTable maxLvt;

        LocalVariableTableImpl(com.sun.max.vm.classfile.constant.ConstantPool constantPool, com.sun.max.vm.classfile.LocalVariableTable maxLvt) {
            this.constantPool = constantPool;
            this.maxLvt = maxLvt;
        }

        @Override
        public Local[] getLocals() {
            com.sun.max.vm.classfile.LocalVariableTable.Entry[] entries = maxLvt.entries();
            LocalImpl[] result = new LocalImpl[entries.length];
            for (int i = 0; i < entries.length; i++) {
                result[i] = new LocalImpl(entries[i]);
            }
            return result;
        }

        @Override
        public Local[] getLocalsAt(int bci) {
            // TODO
            MaxGraal.unimplemented("LocalVariableTable.getLocalsAt");
            return null;
        }

        @Override
        public Local getLocal(int slot, int bci) {
            com.sun.max.vm.classfile.LocalVariableTable.Entry entry = maxLvt.findLocalVariable(slot, bci);
            if (entry == null) {
                return null;
            }
            return new LocalImpl(entry);
        }

    }

    @Override
    public LocalVariableTable getLocalVariableTable() {
        LocalVariableTable lvt = getLocalVariableTableMap().get(riMethod);
        if (lvt == null) {
            ClassMethodActor cma = (ClassMethodActor) riResolvedMethod();
            com.sun.max.vm.classfile.LocalVariableTable maxLvt = cma.codeAttribute().localVariableTable();
            lvt = new LocalVariableTableImpl(cma.codeAttribute().cp, maxLvt);
            localVariableTableMap.put(riMethod, lvt);
        }
        return lvt;
    }

    @Override
    public void reprofile() {
        // TODO Auto-generated method stub

    }

    @Override
    public Constant invoke(Constant receiver, Constant[] arguments) {
        Method javaMethod = ((MethodActor) riMethod).toJava();
        javaMethod.setAccessible(true);

        Object[] objArguments = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            objArguments[i] = arguments[i].asBoxedValue();
        }
        Object objReceiver = receiver != null ? receiver.asObject() : null;

        try {
            Object objResult = javaMethod.invoke(objReceiver, objArguments);
            return javaMethod.getReturnType() == void.class ? null : Constant.forBoxed(getSignature().getReturnKind(), objResult);

        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public Constant newInstance(Constant[] arguments) {
        Constructor javaConstructor = ((MethodActor) riMethod).toJavaConstructor();
        javaConstructor.setAccessible(true);

        Object[] objArguments = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            objArguments[i] = arguments[i].asBoxedValue();
        }

        try {
            Object objResult = javaConstructor.newInstance(objArguments);
            assert objResult != null;
            return Constant.forObject(objResult);

        } catch (IllegalAccessException | InvocationTargetException | InstantiationException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public Constant getEncoding() {
        CiConstant encoding = ((MethodActor) riResolvedMethod()).getEncoding();
        return ConstantMap.toGraal(encoding);
    }

    @Override
    public boolean isInVirtualMethodTable() {
        ClassMethodActor methodActor = (ClassMethodActor) riResolvedMethod();
        assert methodActor instanceof VirtualMethodActor;
        VirtualMethodActor virtualMethodActor = (VirtualMethodActor) methodActor;
        final int vtableIndex = virtualMethodActor.vTableIndex();
        return vtableIndex >= 0;
    }

    @Override
    public boolean isSynthetic() {
        MethodActor methodActor = (MethodActor) riMethod;
        return methodActor.isSynthetic();
    }

}
