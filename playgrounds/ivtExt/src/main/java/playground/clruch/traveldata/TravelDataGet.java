/**
 * 
 */
package playground.clruch.traveldata;

/**
 * @author Claudio Ruch
 *
 */
import java.io.File;
import java.util.Properties;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.ScenarioOptions;

public enum TravelDataGet {
    ;

    public static TravelData readDefault(VirtualNetwork<Link> virtualNetwork) {
        GlobalAssert.that(virtualNetwork!=null);
        Properties simOptions = ScenarioOptions.getDefault();
        final File travelDataFile = new File(simOptions.getProperty("virtualNetworkDir"), //
                simOptions.getProperty("travelDataName"));
        System.out.println("loading travelData from " + travelDataFile.getAbsoluteFile());
        try {
            return TravelDataIO.fromByte(virtualNetwork, travelDataFile);
        } catch (Exception e) {
            System.err.println("cannot load default " + travelDataFile);
            e.printStackTrace();
        }
        return null;
    }

}
