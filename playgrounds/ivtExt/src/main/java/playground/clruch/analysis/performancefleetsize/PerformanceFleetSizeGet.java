/**
 * 
 */
package playground.clruch.analysis.performancefleetsize;

import java.io.File;
import java.io.IOException;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import playground.clruch.options.ScenarioOptions;

/** @author Claudio Ruch */
public enum PerformanceFleetSizeGet {
    ;

    public static PerformanceFleetSizeCalculator readDefault() throws IOException {
        ScenarioOptions scenarioOptions = ScenarioOptions.load(MultiFileTools.getWorkingDirectory());
        final File performanceFleetSizeFile = new File(scenarioOptions.getVirtualNetworkName(),//
                scenarioOptions.getPerformFleetName());
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
