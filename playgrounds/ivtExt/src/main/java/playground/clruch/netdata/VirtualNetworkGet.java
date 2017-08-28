// code by clruch
package playground.clruch.netdata;

import java.io.File;
import java.util.Properties;

import org.matsim.api.core.v01.network.Network;

import playground.clruch.ScenarioOptions;
import playground.clruch.utils.GlobalAssert;

public enum VirtualNetworkGet {
    ;

    public static VirtualNetwork readDefault(Network network) {
        Properties simOptions = ScenarioOptions.getDefault();
        final File virtualnetworkFile = new File(simOptions.getProperty("virtualNetworkDir"), //
                simOptions.getProperty("virtualNetworkName"));
        System.out.println("reading network from" + virtualnetworkFile.getAbsoluteFile());
        GlobalAssert.that(virtualnetworkFile.exists());
        try {
            return VirtualNetworkIO.fromByte(network, virtualnetworkFile);
        } catch (Exception e) {
            System.out.println("cannot load default " + virtualnetworkFile);

        }
        return null;
    }

}
