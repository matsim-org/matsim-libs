// code by clruch
package playground.clruch.netdata;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNetworkIO;
import playground.clruch.ScenarioOptions;

public enum VirtualNetworkGet {
    ;


/** @param network
 * @return null if file does not exist */
    public static VirtualNetwork<Link> readDefault(Network network) {

        Properties simOptions = ScenarioOptions.getDefault();
        final File virtualnetworkFile = new File(simOptions.getProperty("virtualNetworkDir"), //
                simOptions.getProperty("virtualNetworkName"));
        System.out.println("reading network from" + virtualnetworkFile.getAbsoluteFile());
        try {

            Map<String, Link> map = new HashMap<>();
            network.getLinks().entrySet().forEach(e -> map.put(e.getKey().toString(), e.getValue()));

            return (new VirtualNetworkIO<Link>()).fromByte(map, virtualnetworkFile);
        } catch (Exception e) {
            System.out.println("cannot load default " + virtualnetworkFile);

        }
        return null;
    }

}
