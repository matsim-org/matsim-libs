package playground.lsieber.networkshapecutter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Network;

/** @author Claudio Ruch */
public interface NetworkCutter {

    Network filter(Network network, HashSet<String> modes) throws MalformedURLException, IOException;

    void printCutSummary();

    void checkNetworkConsistency();

}
