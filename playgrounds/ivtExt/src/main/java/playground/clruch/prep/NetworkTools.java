package playground.clruch.prep;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkScenarioCut;

/**
 * 
 * @author clruch
 *
 */

public class NetworkTools {

    private static final Logger log = Logger.getLogger(NetworkTools.class);

    public static Network elminateOutsideRadius(Network network, Coord center, double radius) {

        log.info("All network elements which are more than " + radius + " [m] away from Coord " + center.toString() + " removed.");
        
        System.out.println("network size before operation: " + network.getNodes().size());
        NetworkScenarioCut networkScenarioCut = new NetworkScenarioCut(center, radius);
        networkScenarioCut.run(network);
        System.out.println("network size after cutting: " + network.getNodes().size());
        NetworkCleaner networkCleaner = new NetworkCleaner();
        networkCleaner.run(network);
        System.out.println("network size after cleaning: " + network.getNodes().size());

        return network;

    }
}
