/**
 * 
 */
package playground.clruch.traveldata;

/**
 * @author Claudio Ruch
 *
 */
import java.io.File;

import playground.clruch.netdata.VirtualNetwork;

public enum TravelDataGet {
 ;

 public static TravelData readDefault(VirtualNetwork virtualNetwork) {
     final File travelDataFile = new File("virtualNetwork/travelData");
     System.out.println("" + travelDataFile.getAbsoluteFile());
     try {
         return TravelDataIO.fromByte(virtualNetwork, travelDataFile);
     } catch (Exception e) {
         System.out.println("cannot load default " + travelDataFile);
         e.printStackTrace();
     }
     return null;
 }

}


