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
    void checkNetworkConsistency();

}
