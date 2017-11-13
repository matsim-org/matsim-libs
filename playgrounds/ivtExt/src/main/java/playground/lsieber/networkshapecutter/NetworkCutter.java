/**
 * 
 */
package playground.lsieber.networkshapecutter;

import org.matsim.api.core.v01.network.Network;

/** @author Claudio Ruch */
public interface NetworkCutter {

    // TODO can we do it as an abstract function? 
    Network filter(Network network);
    void printCutSummary();
    default void checkNetworkConsistency() {
        // TODO jan added implementation to precent compile error
    }

}
