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
    private final int baseIdMask;
    TLongLongHashMap countMap = new TLongLongHashMap();
    DeltaHistoryTable(int baseIdWidth) {

        this.baseIdWidth = baseIdWidth;
        idHistory = new int[1 << baseIdWidth];
        baseIdMask = (1 << baseIdWidth) -1;
        int initSlice = 1 <<(31 - baseIdWidth);
        int initValue=0;
        for(int i=0;i<idHistory.length;i++) {
            idHistory[i] = initValue;
            initValue += initSlice;
        }
    }


    public long getCodedDelta(int id) {
        logger.debug("Before: delta table = {}",this);
        int minDeltaIndex = findClosestMatch(id);
        int delta = id - idHistory[minDeltaIndex];
        long codedDelta = delta << baseIdWidth | minDeltaIndex;
        updateTable(id, minDeltaIndex, delta);
        countMap.adjustOrPutValue(codedDelta,1,1);
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
       if(delta == 0) {
           for(int i = minDeltaIndex; i >0;i--) {
               if(i == idHistory.length) {
                   logger.error("too high:)");
               }
               if(i == 0) {
                   logger.error("too low");
               }
               int tmp = idHistory[i-1];
               idHistory[i-1] = idHistory[i];
               idHistory[i] = tmp;
           }
       } else {
           System.arraycopy(idHistory,0,idHistory,1,idHistory.length-1);
           idHistory[0] = id;
       }
    }

    public void dumpCounts() {
        long keyArray[] = countMap.keys(new long[countMap.size()]);
        Arrays.sort(keyArray);
        int smalls=0;
        int bytes =0;
        int total =0;
        for(int i=0; i < keyArray.length;i++) {
            long key = keyArray[i];
            long count = countMap.get(key);
            total += count;
            if(key == (key & 0xff)) {
                bytes+= count;
            }
            if(count >3) {
                logger.info("key: {}, count: {}", key, count);
            } else {
                smalls++;
            }
        }
        logger.info("keys: {}, total count: {}",keyArray.length,total);
        logger.info("bytes {}, longtail {} <3 uses ",bytes,smalls);
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
