/**
 * 
 */
package playground.clruch.traveldata;

/**
 * @author Claudio Ruch
 *
 */
import java.io.File;

import org.matsim.api.core.v01.network.Network;

import playground.clruch.netdata.VirtualNetwork;

public enum TravelDataGet {
 ;

 public static TravelData readDefault(Network network, VirtualNetwork virtualNetwork) {
     final File travelDataFile = new File("virtualNetwork/travelData");
     System.out.println("" + travelDataFile.getAbsoluteFile());
     try {
         return TravelDataIO.fromByte(network,virtualNetwork, travelDataFile);
     } catch (Exception e) {
         // e.printStackTrace();
         System.out.println("cannot load default " + travelDataFile);
         // throw new RuntimeException();
     }
     return null;
 }

}


