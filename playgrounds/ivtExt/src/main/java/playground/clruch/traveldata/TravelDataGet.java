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

import playground.clruch.DefaultOptions;
import playground.clruch.netdata.VirtualNetwork;

public enum TravelDataGet {
    ;

    public static TravelData readDefault(VirtualNetwork virtualNetwork) {
        Properties simOptions = DefaultOptions.getDefault();
        final File travelDataFile = new File(simOptions.getProperty("virtualNetworkDir") + "/" + //
                simOptions.getProperty("travelDataName"));
        System.out.println("loading travelData from " + travelDataFile.getAbsoluteFile());
        try {
            return TravelDataIO.fromByte(virtualNetwork, travelDataFile);
        } catch (Exception e) {
            System.out.println("cannot load default " + travelDataFile);
            e.printStackTrace();
        }
        return null;
    }

}
