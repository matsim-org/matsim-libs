/**
 * 
 */
package playground.clruch;

import java.io.File;
import java.net.MalformedURLException;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;

/** @author Claudio Ruch */
public class SequentialScenarioServer {

    public static void main(String[] args) throws MalformedURLException, Exception {
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        /** Delete current content in outputfolder, DO NOT MODIFY THIS, POTENTIALLY VERY DANGEROUS. */
        SequentialScenarioTools.emptyOutputFolder(workingDirectory);

        int iterations = 3;
        if (iterations % 2 == 0) {
            iterations = iterations - 1;
        }

        // double[] fareRatios = { 0.1, 0.5, 1.0, 2.0, 10.0 };

        double factorPlus = 1.5;
        double[] fareRatios = SequentialScenarioTools.fareRatioCreator(iterations, factorPlus);
        int[] vehicleNumbers = new int[] { 10, 500, 2000 };

        // copy the raw folder name including changed settings
        for (int i = 0; i < iterations; ++i) {
            System.out.println("working in the directory " + workingDirectory.getAbsolutePath());

            // modify the av.xml file
            // changeFareRatioTo(fareRatios[i], rawFolder);
            SequentialScenarioTools.changeVehicleNumberTo(vehicleNumbers[i], workingDirectory);

            // set the output-directory correctly
            SequentialScenarioTools.changeOutputDirectoryTo("output/" + String.format("%04d", i), workingDirectory);

            // simulate
            ScenarioServer.simulate();

        }

    }

}