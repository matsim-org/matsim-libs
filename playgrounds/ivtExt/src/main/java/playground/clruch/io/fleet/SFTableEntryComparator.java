/**
 * 
 */
package playground.clruch.io.fleet;

import java.util.Comparator;

/** @author Claudio Ruch */
public class SFTableEntryComparator implements Comparator<SFTableEntry> {

    /*
     * (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(SFTableEntry o1, SFTableEntry o2) {
        if (o1.c4 < o2.c4)
            return -1;
        if (o1.c4 > o2.c4)
            return 1;
        return 0;

    }

}
