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

#ifndef SRC_COMMON_H
#define SRC_COMMON_H

// Addresses are plain 64-bit uints. This should be kept compatible with PIN addrints
typedef uint64_t Address;

// Unused variable.
#define UNUSED_VAR(unused_var) do { (void)(unused_var); } while (0)

// Pointer tag type.
typedef uint16_t PointerTag_t;

// Memory Access (MA) size type.
typedef uint16_t MASize_t;

// Memory Access (MA) offset type.
typedef int32_t MAOffset_t;

// Thread id type.
typedef uint16_t ThreadId_t;

#endif //SRC_COMMON_H
