/**
 * 
 */
package playground.clruch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

//TODO comment this file
/** @author Claudio Ruch */
public enum ScenarioOptions {
    ;

    private static final String OPTIONSFILENAME = "IDSCOptions.properties";

    public static Properties getDefault() {
        Properties returnP = new Properties();
        returnP.setProperty("fullConfig", "av_config_full.xml");
        returnP.setProperty("simuConfig", "av_config.xml");
        returnP.setProperty("maxPopulationSize", "2000");
        returnP.setProperty("numVirtualNodes", "10");
        returnP.setProperty("dtTravelData", "3600");
        returnP.setProperty("completeGraph", "true");
        returnP.setProperty("populationeliminateFreight", "false");
        returnP.setProperty("populationeliminateWalking", "false");
        returnP.setProperty("populationchangeModeToAV", "true");
        returnP.setProperty("waitForClients", "false");
        returnP.setProperty("virtualNetworkDir", "virtualNetwork");
        returnP.setProperty("virtualNetworkName", "virtualNetwork");
        returnP.setProperty("travelDataName", "travelData");
        returnP.setProperty("calculatePerformanceFleetSize", "false");
        returnP.setProperty("minimumFleetSizeFileName", "minimumFleetSizeCalculator");
        returnP.setProperty("performanceFleetSizeFileName", "performanceFleetSizeCalculator");
        returnP.setProperty("centerNetwork","false");
        return returnP;
    }

    public static void saveDefault() throws IOException {
        saveProperties(getDefault(), OPTIONSFILENAME);
    }

    private static void saveProperties(Properties prop, String filename) throws IOException {
        File defaultFile = new File(filename);
        try (FileOutputStream ostream = new FileOutputStream(defaultFile)){
            prop.store(ostream, "This is a sample config file.");            
        }
    }

    /** @param workingDirectory with simulation data an dan IDSC.Options.properties file
     * @return Properties object with default options and any options found in folder
     * @throws IOException */
    public static Properties load(File workingDirectory) throws IOException {
        System.out.println("working in directory \n" + workingDirectory.getCanonicalFile());

        Properties simOptions = new Properties(getDefault());
        File simOptionsFile = new File(workingDirectory, OPTIONSFILENAME);
        if (simOptionsFile.exists()) {
            simOptions.load(new FileInputStream(simOptionsFile));
        } else
            ScenarioOptions.saveDefault();

        return simOptions;
    }

}
