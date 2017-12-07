/**
 * 
 */
package playground.clruch;

import java.io.File;
import java.net.MalformedURLException;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import playground.clruch.options.ScenarioOptions;

/** @author Claudio Ruch */
public class SequentialScenarioServer {

    public static void main(String[] args) throws MalformedURLException, Exception {
        long startT = System.currentTimeMillis();
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        ScenarioOptions simOptions = ScenarioOptions.load(workingDirectory);

        /** Delete current content in outputfolder, DO NOT MODIFY THIS, POTENTIALLY VERY DANGEROUS. */
        SequentialScenarioTools.emptyOutputFolder(workingDirectory);

        int iterations = 13;
        if (iterations % 2 == 0) {
            iterations = iterations - 1;
        }

        double factorPlus = 1.9;
        double[] fareRatios = SequentialScenarioTools.fareRatioCreator(iterations, factorPlus);
        //double[] fareRatios = new double[]{0.0001, 1.0, 10000.0};
        

        // copy the raw folder name including changed settings
        for (int i = 0; i < iterations; ++i) {
            System.out.println("working in the directory " + workingDirectory.getAbsolutePath());

            // modify the av.xml file
            SequentialScenarioTools.changeFareRatioTo(fareRatios[i], workingDirectory);
            // SequentialScenarioTools.changeVehicleNumberTo(vehicleNumbers[i], workingDirectory);

            // set the output-directory correctly
            SequentialScenarioTools.changeOutputDirectoryTo("output/" + String.format("%04d", i), workingDirectory,//);
                    simOptions);

            // simulate
            ScenarioServer.simulate();

        }

        System.out.println("duration: " +  (System.currentTimeMillis()-startT) + " milliseconds");
    }

}