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

import com.oracle.graal.api.meta.*;
import com.oracle.graal.replacements.Snippet.DefaultSnippetInliningPolicy;
import com.oracle.max.vm.ext.graal.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;

/**
 * Slow path calls in methods (transitively) referenced from snippets should be annotated with {@link NEVER_INLINE} or {@link SNIPPET_SLOWPATH}.
 * For the former, it is important to check the {@link Actor#NEVER_INLINE} flag (implicitly via {@link ResolvedJavaMethod#canBeInlined()}
 * because an entire class (e.g. {@link com.sun.max.vm.Log} can be annotated and checking just the method annotation will fail.
 */
public class MaxSnippetInliningPolicy extends DefaultSnippetInliningPolicy {

    public MaxSnippetInliningPolicy() {
        super(MaxGraal.runtime());
    }

    @Override
    public boolean shouldInline(ResolvedJavaMethod method, ResolvedJavaMethod caller) {
        if (!method.canBeInlined() || method.getAnnotation(SNIPPET_SLOWPATH.class) != null) {
            return false;
        }
        MethodActor ma = (MethodActor) MaxResolvedJavaMethod.getRiResolvedMethod(method);
        if (MethodActor.isDeclaredFoldable(ma.flags())) {
            return false;
        }
        return super.shouldInline(method, caller);
    }

}
