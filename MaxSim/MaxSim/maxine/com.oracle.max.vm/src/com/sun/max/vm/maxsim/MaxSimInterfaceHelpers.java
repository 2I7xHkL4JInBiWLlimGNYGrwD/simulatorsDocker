/*
 * Copyright (c) 2017, Andrey Rodchenko, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package com.sun.max.vm.maxsim;

import com.sun.max.annotate.FOLD;
import com.sun.max.annotate.INLINE;

public class MaxSimInterfaceHelpers {

    @FOLD
    static public boolean isMaxSimEnabled() {
        return MaxSimInterface.MaxSimConfig.getDefaultInstance().getIsMaxSimEnabled();
    }

    @FOLD
    static public boolean isTaggingEnabled() {
        return isMaxSimEnabled() && (MaxSimInterface.MaxSimConfig.getDefaultInstance().getPointerTaggingType() !=
            MaxSimInterface.PointerTaggingType.NO_TAGGING);
    }

    @FOLD
    static public int getLayoutScaleFactor() {
        return MaxSimInterface.MaxSimConfig.getDefaultInstance().getLayoutScaleFactor();
    }

    @FOLD
    static public int getLayoutScaleRefFactor() {
        return MaxSimInterface.MaxSimConfig.getDefaultInstance().getLayoutScaleRefFactor();
    }

    @FOLD
    static public boolean isClassIDTagging() {
        return MaxSimInterface.MaxSimConfig.getDefaultInstance().getPointerTaggingType() ==
            MaxSimInterface.PointerTaggingType.CLASS_ID_TAGGING;
    }

    @INLINE
    static public boolean isClassIDTagging(MaxSimInterface.PointerTaggingType pointerTaggingType) {
        return pointerTaggingType == MaxSimInterface.PointerTaggingType.CLASS_ID_TAGGING;
    }

    @FOLD
    static public boolean isAllocationSiteIDTagging() {
        return MaxSimInterface.MaxSimConfig.getDefaultInstance().getPointerTaggingType() ==
            MaxSimInterface.PointerTaggingType.ALLOC_SITE_ID_TAGGING;
    }

    @INLINE
    static public boolean isAllocationSiteIDTagging(MaxSimInterface.PointerTaggingType pointerTaggingType) {
        return pointerTaggingType == MaxSimInterface.PointerTaggingType.ALLOC_SITE_ID_TAGGING;
    }

    @INLINE
    public static boolean isAggregateTag(short tag) {
        return (MaxSimInterface.PointerTag.TAG_AGGREGATE_LO_VALUE <= tag) &&
            (tag <= MaxSimInterface.PointerTag.TAG_AGGREGATE_HI_VALUE);
    }

    @INLINE
    public static boolean isGeneralPurposeTag(short tag) {
        return (MaxSimInterface.PointerTag.TAG_GP_LO_VALUE <= tag) ||
            (tag <= MaxSimInterface.PointerTag.TAG_GP_HI_VALUE);
    }

    @INLINE
    public static boolean isUndefinedGeneralPurposeTag(short tag) {
        return tag == MaxSimInterface.PointerTag.TAG_UNDEFINED_GP_VALUE;
    }

    @INLINE
    public static boolean isUndefinedTag(short tag) {
        return tag == MaxSimInterface.PointerTag.TAG_UNDEFINED_VALUE;
    }
}
