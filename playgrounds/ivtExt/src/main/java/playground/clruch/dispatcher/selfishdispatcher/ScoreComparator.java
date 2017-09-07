/**
 * 
 */
package playground.clruch.dispatcher.selfishdispatcher;

import java.util.Comparator;
import java.util.Map.Entry;

import playground.clruch.netdata.VirtualNode;

/** @author Claudio Ruch */
public class ScoreComparator implements Comparator<Entry<VirtualNode, Double>> {

    /** Compares its two arguments for order. Returns a negative integer,
     * zero, or a positive integer as the first argument is less than, equal
     * to, or greater than the second.
     * <p>
    */
    @Override
    public int compare(Entry<VirtualNode, Double> e1, Entry<VirtualNode, Double> e2) {
        if (e1.getValue().equals(e2.getValue()))
            return 0;
        if (e1.getValue() < e2.getValue())
            return -1;
        else {
            return 1;
        }
    }
}
