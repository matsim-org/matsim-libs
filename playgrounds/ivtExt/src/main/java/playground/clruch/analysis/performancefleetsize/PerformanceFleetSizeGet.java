/**
 * 
 */
package playground.clruch.analysis.performancefleetsize;

import java.io.File;

/**
 * @author Claudio Ruch
 *
 */
public enum PerformanceFleetSizeGet {
    ;
    ;

    public static PerformanceFleetSizeCalculator readDefault() {
        final File performanceFleetSizeFile = new File("virtualNetwork/performanceFleetSizeCalculator");
        System.out.println("" + performanceFleetSizeFile.getAbsoluteFile());
        try {
            return PerformanceFleetSizeIO.fromByte(performanceFleetSizeFile);
        } catch (Exception e) {
            System.err.println("cannot load minimum fleet size calculation  " + performanceFleetSizeFile);
            System.out.println("");
            return null;
        }

    }


}
