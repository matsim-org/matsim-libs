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
public enum DefaultOptions {
    ;
    
    public static final String OPTIONSFILENAME = "IDSCOptions.properties";

    public static Properties getDefault() {
        Properties returnP = new Properties();
        returnP.setProperty("fullConfig", "av_config_full.xml");
        returnP.setProperty("simuConfig", "av_config.xml");
        returnP.setProperty("maxPopulationSize", "2000");
        returnP.setProperty("numVirtualNodes", "10");
        returnP.setProperty("dtTravelData", "3600");
        returnP.setProperty("completeGraph", "true");
        returnP.setProperty("LocationSpec", "SIOUXFALLS_CITY");
        returnP.setProperty("populationeliminateFreight", "false");
        returnP.setProperty("populationeliminateWalking", "false");
        returnP.setProperty("populationchangeModeToAV", "true");
        returnP.setProperty("waitForClients", "false");
        returnP.setProperty("ReferenceFrame", "SIOUXFALLS"); /** see (@link ReferenceFrame) class */
        returnP.setProperty("virtualNetworkDir", "virtualNetwork");
        returnP.setProperty("virtualNetworkName", "virtualNetwork");
        returnP.setProperty("travelDataName", "travelData");
        return returnP;
    }

    public static void saveDefault() throws IOException {
        saveProperties(getDefault(), OPTIONSFILENAME);
    }

    private static void saveProperties(Properties prop, String filename) throws IOException {
        File defaultFile = new File(filename);
        FileOutputStream ostream = new FileOutputStream(defaultFile);
        prop.store(ostream, "This is a sample config file.");
    }
    
    
    public static Properties load(File workingDirectory) throws IOException{
        System.out.println("working in directory " + workingDirectory.getCanonicalFile());

        Properties simOptions = new Properties(getDefault());
        File simOptionsFile = new File(workingDirectory, OPTIONSFILENAME);
        if (simOptionsFile.exists()) {
            simOptions.load(new FileInputStream(simOptionsFile));
        }else DefaultOptions.saveDefault();

        return simOptions;
    }

    

}
