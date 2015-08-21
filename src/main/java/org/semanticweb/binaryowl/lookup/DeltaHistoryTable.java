package org.semanticweb.binaryowl.lookup;/**
 * Created by ses on 8/10/15.
 */

import com.google.common.base.MoreObjects;
import gnu.trove.map.hash.TLongLongHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class DeltaHistoryTable {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(DeltaHistoryTable.class);
    private final int baseIdWidth;
    private final int idHistory[];
    private final int prevId[];
    private final long lastUsed[];
    private long now = 0;
    private final int positiveClose;
    private final int negativeClose;
    private final int baseIdMask;
    TLongLongHashMap positiveDeltaCountMap = new TLongLongHashMap();
    TLongLongHashMap negativeDeltacountMap = new TLongLongHashMap();

    public DeltaHistoryTable(int baseIdWidth, int maxValue) {
        this(baseIdWidth, maxValue, -defaultClose(baseIdWidth),defaultClose(baseIdWidth));
    }

    public DeltaHistoryTable(int baseIdWidth, int maxValue, int negativeClose,int positiveClose) {

        this.baseIdWidth = baseIdWidth;
        int size = 1 << baseIdWidth;
        idHistory = new int[size];
        lastUsed = new long[size];
        prevId = new int[size];
        baseIdMask = (size) - 1;
        int initSlice = maxValue / size;
        int initValue=0;

        for(int i=0;i<idHistory.length;i++) {
            idHistory[i] = initValue;
            prevId[i] = -1;
            initValue += initSlice;
            //idHistory[i] = 0;
        }
        this.positiveClose = positiveClose;
        this.negativeClose= negativeClose;

    }

    private static int defaultClose(int baseIdWidth) {
        return 1 << Math.max(0, 12 - baseIdWidth);
    }


    public long getCodedDelta(int id) {
        logger.debug("Before: delta table = {}",this);
        int minDeltaIndex = findClosestMatch(id);
        int delta = id - idHistory[minDeltaIndex];
        logger.trace("Table-{} {}: {} ({}) + {}", this.hashCode(), id, idHistory[minDeltaIndex], minDeltaIndex, delta);
        long codedDelta = delta << baseIdWidth | minDeltaIndex;
        updateTable(id, minDeltaIndex, delta);
        if (delta >= 0) {
            positiveDeltaCountMap.adjustOrPutValue(delta, 1, 1);
        } else {
            negativeDeltacountMap.adjustOrPutValue(-delta, 1, 1);
        }
        logger.debug("After: History table = {}",this);
        logger.debug("id = {},codedDelta={} (delta={},baseId={})",id,codedDelta, delta, minDeltaIndex);

        return codedDelta;
    }

    private int findClosestMatch(int id) {
        int minDelta = Integer.MAX_VALUE;
        int minDeltaIndex = 0;
        for (int i = 0; i < idHistory.length; i++) {
            int delta = Math.abs(id - idHistory[i]);
            if (delta < minDelta) {
                minDelta = delta;
                minDeltaIndex = i;
                if (delta == 0) {
                    break;
                }
            }
        }
        return minDeltaIndex;
    }

    private void updateTable(int id, int minDeltaIndex, int delta) {
        if (delta == 0) {
            handleHit(id, delta, minDeltaIndex);
        } else if (isCloseMatch(delta)) {
            handleClose(id, delta, minDeltaIndex);
        } else {
            handleMiss(id,delta,minDeltaIndex);
        }
    }

    private boolean isCloseMatch(int delta) {
        return delta >= negativeClose && delta <= positiveClose;
    }

    private void handleHit(int id, int delta, int hitIndex) {
        lastUsed[hitIndex] = ++now;
    }

    private void handleClose(int id, int delta, int minDeltaIndex) {
        if (prevId[minDeltaIndex] >=0) {
            int prevDelta = idHistory[minDeltaIndex] - prevId[minDeltaIndex];
            logger.trace("delta is {}, prevDelta is {}", delta,prevDelta);
            if(delta == prevDelta) {
                for (int i = 0; i < idHistory.length; i++) {
                    if (i != minDeltaIndex && idHistory[i] == id + prevDelta ) {
                        logger.info("wiping entry {} as {} is too close to {} - {}", i, idHistory[i], id, prevDelta);
                        idHistory[i] = 0;
                        prevId[i] = -1;
                        lastUsed[i] = -1;
                    }
                }
            }
        }
        prevId[minDeltaIndex] = idHistory[minDeltaIndex];
        idHistory[minDeltaIndex] = id;
        lastUsed[minDeltaIndex] = ++now;
    }


    private void handleMiss( int nonMatchId, int delta,int minDeltaIndex) {
        logger.info("Miss for {}",nonMatchId);
        long lruTime = Long.MAX_VALUE;
        int lruIndex = -1;
        for (int i = 0; i < idHistory.length; i++) {
            if (lastUsed[i] < lruTime) {
                lruTime = lastUsed[i];
                lruIndex = i;
            }
        }
        if(prevId[lruIndex] != -1) {
            logger.info("LRU discarding stream @ {}, {},{} ",lruIndex,prevId[lruIndex],idHistory[lruIndex]);
        }  else {
            logger.info("LRU discarding non-stream @ {}, {}",lruIndex,idHistory[lruIndex]);
        }
        idHistory[lruIndex] = nonMatchId;
        prevId[lruIndex] = -1;
        lastUsed[lruIndex] = ++now;
    }

    private void shuffleHit(int id, int delta, int minDeltaIndex) {
           for(int i = minDeltaIndex; i >0;i--) {
               int tmp = idHistory[i-1];
               idHistory[i-1] = idHistory[i];
               idHistory[i] = tmp;
           }
    }

    private void shuffleMiss(int id) {
           System.arraycopy(idHistory,0,idHistory,1,idHistory.length-1);
           idHistory[0] = id;
       }

    public void resetCounts() {
        positiveDeltaCountMap = new TLongLongHashMap();
        negativeDeltacountMap = new TLongLongHashMap();
    }

    public void dumpCounts() {
        long total=0;
        logger.info("Table size: {}, near-miss = {}  - 0 -  {} ",idHistory.length,negativeClose,positiveClose);
        logger.info("Negative Deltas");
        total += dumpCounts(negativeDeltacountMap, true);
        logger.info("Positive Deltas");
        total += dumpCounts(positiveDeltaCountMap, false);
        logger.info("Grand total: {}", pp(total));
    }

    private long dumpCounts(TLongLongHashMap countMap, boolean reverse) {
        long keyArray[] = countMap.keys(new long[countMap.size()]);
        Arrays.sort(keyArray);
        long firstOnes[] = new long[64];
        int smalls=0;
        int bytes =0;
        long total =0;
        for(int i=0; i < keyArray.length;i++) {
            long key = keyArray[i];
            int firstOne = 64 - Long.numberOfLeadingZeros(key);
            long count = countMap.get(key);
            firstOnes[firstOne] += count;
            total += count;
        }

        long cumSums[] = new long[64];
        int cumSum=0;
        for(int i= 0; i < 64;i++)  {
           cumSum += firstOnes[i];
            cumSums[i] = cumSum;
        }
        if (!reverse) {
            for (int i = 0; i < 64; i++) {
                long firstOne = firstOnes[i];
                if (firstOne != 0) {
                    logDelta(i, firstOne,cumSums[i],total);
                }
            }
        } else {
            for (int i = 63; i >= 0; i--) {
                long firstOne = firstOnes[i];
                cumSum += firstOne;
                if (firstOne != 0) {
                    logDelta(i, firstOne,cumSums[i],total);
                }
            }

        }
        logger.info(" Sub-total: {}", pp(total));
        return total;
    }

    private void logDelta(int i, long firstOne, long cumSum, long total) {
        int lower = i == 0 ? 0 : 1 << i - 1;
        int upper = 1 << i;
        logger.info("\t[{}, {}): {} ({} / {} - {})", lower, upper, pp(firstOne),pp(cumSum),pp(total),ppPercent(cumSum,total));
    }

    private String pp(long firstOne) {
        return String.format("%,d", firstOne);
    }
    private String ppPercent(double num, double denom) {
        return String.format("%,.2f%%", (num * 100) / denom);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("idHistory", Arrays.toString(idHistory))
                .toString();
    }

    public long decodeDelta(long codedDelta) {
        return codedDelta >> baseIdWidth;
    }

    public long decodeDeltaBase(long codedDelta) {
        return codedDelta & baseIdMask;
    }
}
