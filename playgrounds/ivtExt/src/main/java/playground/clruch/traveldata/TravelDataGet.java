/**
 * 
 */
package playground.clruch.traveldata;

/**
 * @author Claudio Ruch
 *
 */
import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.options.ScenarioOptions;

public enum TravelDataGet {
    ;

    public static TravelData readDefault(VirtualNetwork<Link> virtualNetwork) throws IOException {
        GlobalAssert.that(virtualNetwork != null);
        ScenarioOptions scenarioOptions = ScenarioOptions.load(MultiFileTools.getWorkingDirectory());
        final File travelDataFile = new File(scenarioOptions.getVirtualNetworkName(), //
                scenarioOptions.getTravelDataName());
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
