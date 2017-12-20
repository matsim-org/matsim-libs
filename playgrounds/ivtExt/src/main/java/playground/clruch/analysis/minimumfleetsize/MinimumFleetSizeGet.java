/**
 * 
 */
package playground.clruch.analysis.minimumfleetsize;

import java.io.File;
import java.io.IOException;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import playground.clruch.options.ScenarioOptions;

/** @author Claudio Ruch */
public enum MinimumFleetSizeGet {
    ;

    public static MinimumFleetSizeCalculator readDefault() throws IOException {
        ScenarioOptions scenarioOptions = ScenarioOptions.load(MultiFileTools.getWorkingDirectory());
        final File minimumFleetSizeFile = new File(scenarioOptions.getVirtualNetworkName(),//
                scenarioOptions.getMinFleetName());
        System.out.println("" + minimumFleetSizeFile.getAbsoluteFile());
        try {
            return MinimumFleetSizeIO.fromByte(minimumFleetSizeFile);
        } catch (Exception e) {
            System.out.println("cannot load minimum fleet size calculation " + minimumFleetSizeFile);
            throw new RuntimeException();
        }
    }

}
