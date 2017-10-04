/**
 * 
 */
package playground.clruch;

import java.io.File;
import java.net.MalformedURLException;

import org.apache.commons.io.FileUtils;

/** @author Claudio Ruch */
public class SequentialScenarioServer {

    public static void main(String[] args) throws MalformedURLException, Exception {

        final int iterations = 5;
        String rawFolderName = "2017_10_04_SiouxFareDataRaw";
        File rawFolder = new File(rawFolderName);

        // copy the raw folder name including changed settings
        for (int i = 1; i <= iterations; ++i) {
            File simFolder = new File(rawFolderName + "_Iteration_" + Integer.toString(i));

            // copy the raw folder
            FileUtils.copyDirectory(rawFolder, simFolder);

            // change the respective setting
            

            // simulate the folder
            // ScenarioServer.simulate();
        }

    }

}
