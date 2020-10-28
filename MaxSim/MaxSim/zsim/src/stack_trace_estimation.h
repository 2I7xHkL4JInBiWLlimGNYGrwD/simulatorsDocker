/** $lic$
 * Copyright (C) 2017 by Andrey Rodchenko, School of Computer Science,
 * The University of Manchester
 *
 * This file is part of zsim.
 *
 * zsim is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2.
 *
 * If you use this software in your research, we request that you reference
 * the zsim paper ("ZSim: Fast and Accurate Microarchitectural Simulation of
 * Thousand-Core Systems", Sanchez and Kozyrakis, ISCA-40, June 2013) as the
 * source of the simulator in any publications that use this software, and that
 * you send us a citation of your work.
 *
 * zsim is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

#ifndef SRC_STACK_TRACE_ESTIMATION_H
#define SRC_STACK_TRACE_ESTIMATION_H

#include <stdint.h>
#include "zsim.h"

#ifdef STACK_TRACE_ESTIMATION_ENABLED

class StackTraceEstimation {
    public:
        void pushReturnAddress(ThreadId_t tid, uint64_t returnAddress) {
            uint16_t csf = ++curStackFrame[tid];
            stackFrameRetAddr[tid][csf & MAX_STACK_FRAMES_MASK] = returnAddress;
        };

        void popReturnAddress(ThreadId_t tid) {
            curStackFrame[tid]--;
        };

        // Gets Nth return address from the top of the stack
        //
        uint64_t topNthReturnAddress(ThreadId_t tid, uint16_t n) {
            uint16_t csf = curStackFrame[tid];
            return stackFrameRetAddr[tid][(csf - n) & MAX_STACK_FRAMES_MASK];
        }

        // Finds the first frame no when predicate p returns true
        //
        template<typename P>
        uint16_t findFrameNoIf(ThreadId_t tid, P p) {
            uint16_t i = 0;
            while (i < MAX_STACK_FRAMES) {
                if (p(topNthReturnAddress(tid, i))) {
                    break;
                }
                i++;
            }
            return i;
        }

    private:
        // maximal number of estimated stack frames (should be power of 2)
        static const uint16_t LOG2_MAX_STACK_FRAMES = 4;
        static const uint16_t MAX_STACK_FRAMES = (1 << LOG2_MAX_STACK_FRAMES);
        static const uint16_t MAX_STACK_FRAMES_MASK = MAX_STACK_FRAMES - 1;

        // return addresses of stack frames
        uint64_t stackFrameRetAddr[MAX_THREADS][MAX_STACK_FRAMES];

        // current stack frame
        uint16_t curStackFrame[MAX_THREADS];

    public:
        static const uint16_t UNDEF_FRAME_NO = MAX_STACK_FRAMES;

    // Singleton part
    public:
        // Get instance
        //
        static StackTraceEstimation &getInst() {
            static StackTraceEstimation stackTraceEstimation;

            return stackTraceEstimation;
        }

        // Delete copy and move constructors and assign operators
        StackTraceEstimation(StackTraceEstimation const &) = delete;
        StackTraceEstimation(StackTraceEstimation &&) = delete;
        StackTraceEstimation &operator=(StackTraceEstimation const &) = delete;
        StackTraceEstimation &operator=(StackTraceEstimation &&) = delete;

    private:
        // Privatize constructor and destructor
        StackTraceEstimation() {}
        ~StackTraceEstimation() {}
};

#endif // STACK_TRACE_ESTIMATION_ENABLED

#endif //SRC_STACK_TRACE_ESTIMATION_H
