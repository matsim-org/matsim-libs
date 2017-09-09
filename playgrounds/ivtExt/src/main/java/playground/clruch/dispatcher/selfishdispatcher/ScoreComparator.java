/**
 * 
 */
package playground.clruch.dispatcher.selfishdispatcher;

import java.util.Comparator;
import java.util.Map.Entry;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.queuey.core.networks.VirtualNode;

/** @author Claudio Ruch */
public class ScoreComparator implements Comparator<Entry<VirtualNode<Link>, Double>> {

    /** Compares its two arguments for order. Returns a negative integer,
     * zero, or a positive integer as the first argument is less than, equal
     * to, or greater than the second.
     * <p>
    */
    @Override
    public int compare(Entry<VirtualNode<Link>, Double> e1, Entry<VirtualNode<Link>, Double> e2) {
        if (e1.getValue().equals(e2.getValue()))
            return 0;
        if (e1.getValue() < e2.getValue())
            return -1;
        else {
            return 1;
        }
    }
}
