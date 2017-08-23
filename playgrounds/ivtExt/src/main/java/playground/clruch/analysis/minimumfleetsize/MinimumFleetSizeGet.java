/**
 * 
 */
package playground.clruch.analysis.minimumfleetsize;

import java.io.File;

/** @author Claudio Ruch */
public enum MinimumFleetSizeGet {
    ;

    public static MinimumFleetSizeCalculator readDefault() {
        final File minimumFleetSizeFile = new File("virtualNetwork/minimumFleetSizeCalculator");
        System.out.println("" + minimumFleetSizeFile.getAbsoluteFile());
        try {
            return MinimumFleetSizeIO.fromByte(minimumFleetSizeFile);
        } catch (Exception e) {
            System.out.println("cannot load minimum fleet size calculation " + minimumFleetSizeFile);
            throw new RuntimeException();
        }
    }

}
