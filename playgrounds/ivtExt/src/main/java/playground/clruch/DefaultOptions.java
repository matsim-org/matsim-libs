/**
 * 
 */
package playground.clruch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

//TODO comment this file
/** @author Claudio Ruch */
public enum DefaultOptions {
    ;

    public static Properties getDefault() {
        Properties returnP = new Properties();
        returnP.setProperty("fullConfig", "av_config_full.xml");
        returnP.setProperty("simuConfig", "av_config.xml");
        returnP.setProperty("maxPopulationSize", "5000");
        returnP.setProperty("numVirtualNodes", "40");
        returnP.setProperty("dtTravelData", "900");
        returnP.setProperty("completeGraph", "true");
        returnP.setProperty("LocationSpec", "SIOUXFALLS_CITY");
        returnP.setProperty("populationeliminateFreight", "false");
        returnP.setProperty("populationeliminateWalking", "false");
        returnP.setProperty("populationchangeModeToAV", "true");
        returnP.setProperty("waitForClients", "false");
        returnP.setProperty("av_config.xml", "/home/clruch/Simulations/2017_07_13_Sioux_TestBed/av_config.xml");
        returnP.setProperty("ReferenceFrame", "SIOUXFALLS");
        return returnP;
    }

    public static void saveDefault() throws IOException {
        saveProperties(getDefault(), "DefaultPreparerOptions");
    }

    private static void saveProperties(Properties prop, String filename) throws IOException {
        File defaultFile = new File(filename);
        FileOutputStream ostream = new FileOutputStream(defaultFile);
        prop.store(ostream, "This is a sample config file.");
    }

}
