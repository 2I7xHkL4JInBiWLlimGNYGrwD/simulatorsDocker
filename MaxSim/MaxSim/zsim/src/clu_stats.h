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

#ifndef SRC_CLU_STATS_H
#define SRC_CLU_STATS_H

#include "zsim.h"
#include "stats.h"

#ifdef CLU_STATS_ENABLED

typedef uint16_t CacheLineAccessMask_t;

extern uint32_t log2CacheLineUtilStatsChunksNum;
extern uint32_t log2CacheLineUtilStatsChunkSize;

#   define CLU_STATS_CHUNKS_DEF (8)

#   define CLU_STATS_CHUNKS_MAX (16) // Maximal number of chunks which is supported.
                                     // If this parameter is increased to > 16 then
                                     // access masks type size (uint16_t) should be increased respectively.

#   define CLU_STATS_ZERO_MASK (0)

// define to consider wrong path cache line fetch as utilization
#   define CLU_WRONG_PATH_FETCH_IS_UTILIZATION

/* Memory request type for statistics collection. */
typedef enum {
    LoadData,
    FetchRightPath,
    FetchWrongPath,
    StoreData,
    MAUndefined
} MemReqStatType_t;

/* Memory request attributes necessary for cache line utilization statistics collection. */
typedef struct MemReqCLUStatsAttrs_t {
    Address virtualAddress; // virtual address
    MASize_t memoryAccessSize; // memory access size
    MemReqStatType_t memoryAccessType; // memory access type
    Address replacedLineAddr; // replaced line address
    CacheLineAccessMask_t replacedLineAccessMask; // replaced cache line access mask
} MemReqCLUStatsAttrs_t;

inline CacheLineAccessMask_t cluStatsGetUtilizationMask(Address vAddr, MASize_t size, MemReqStatType_t memReqStatType) {
    switch (memReqStatType) {
        case LoadData:
        case StoreData:
        {
            uint32_t accessedChunksCeil = (size + (1 << log2CacheLineUtilStatsChunkSize) - 1) >> log2CacheLineUtilStatsChunkSize;
            uint32_t accessedChunksMaskShift = ((vAddr & lineMask) >> (lineBits - log2CacheLineUtilStatsChunksNum));
            CacheLineAccessMask_t accessedChunksMask = (1 << accessedChunksCeil) - 1;

            return accessedChunksMask << accessedChunksMaskShift;
        }
#   ifdef CLU_WRONG_PATH_FETCH_IS_UTILIZATION
        case FetchWrongPath:
#   endif
        case FetchRightPath:
        {
            return (1 << (1 << log2CacheLineUtilStatsChunksNum)) - 1;
        }
#   ifndef CLU_WRONG_PATH_FETCH_IS_UTILIZATION
        case FetchWrongPath:
#   endif
        case MAUndefined:
        {
            return 0;
        }
        default:
        {
            panic("Unsupported MemReqStatType_t!");
            return 0;
        }

    }
};

#endif // CLU_STATS_ENABLED

#endif //SRC_CLU_STATS_H
