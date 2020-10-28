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
package com.oracle.max.vm.ext.graal.amd64;

import com.oracle.graal.api.code.*;
import com.oracle.graal.asm.amd64.*;
import com.oracle.graal.lir.*;
import com.oracle.graal.lir.amd64.*;
import com.oracle.graal.lir.asm.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.oracle.max.vm.ext.graal.nodes.MaxSafepointNode.Op;
import com.sun.max.vm.*;


@Opcode("SAFEPOINT")
public class MaxAMD64SafepointOp extends AMD64LIRInstruction {

    @State protected LIRFrameState state;
    private final MaxSafepointNode.Op op;

    public MaxAMD64SafepointOp(LIRFrameState state, MaxSafepointNode.Op op) {
        this.state = state;
        this.op = op;
    }

    @Override
    public void emitCode(TargetMethodAssembler tasm, AMD64MacroAssembler masm) {
        int pos = masm.codeBuffer.position();
        tasm.recordInfopoint(pos, state, InfopointReason.SAFEPOINT);
        if (op == Op.SAFEPOINT_POLL) {
            byte[] safepointCode = MaxineVM.vm().safepointPoll.code;
            masm.codeBuffer.emitBytes(safepointCode, 0, safepointCode.length);
        }
    }

}
