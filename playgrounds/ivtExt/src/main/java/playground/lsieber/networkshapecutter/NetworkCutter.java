/**
 * 
 */
package playground.lsieber.networkshapecutter;

import java.io.IOException;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.network.Network;

/** @author Claudio Ruch */
public interface NetworkCutter {

    Network filter(Network network) throws MalformedURLException, IOException;

    void printCutSummary();

    void checkNetworkConsistency();

}

