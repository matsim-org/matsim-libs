/**
 * 
 */
package playground.clruch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import playground.clruch.data.LocationSpec;

/** @author Claudio Ruch */
public enum DefaultOptions {
    ;

    /** "av_config_full.xml" is a MATSim config file containing references for the full population and network which
     * are not yet cut.
     * "waitForClients" boolean value false / true that indicates if the server should wait before starting for a
     * viewer to connect.
     * 
     * @return */
    public static Properties getPreparerDefault() {
        Properties returnP = new Properties();
        returnP.setProperty("av_config_full.xml", "/home/clruch/Simulations/2017_07_13_Sioux_TestBed/av_config_full.xml");
        returnP.setProperty("maxPopulationSize", "5000");
        returnP.setProperty("numVirtualNodes", "40");
        returnP.setProperty("dtTravelData", "900");
        returnP.setProperty("completeGraph", "true");

        returnP.setProperty("LocationSpec", "SIOUXFALLS_CITY");
        returnP.setProperty("populationeliminateFreight", "false");
        returnP.setProperty("populationeliminateWalking", "false");
        returnP.setProperty("populationchangeModeToAV", "true");
        return returnP;
    }

    public static void savePreparerDefault() throws IOException {
        saveProperties(getPreparerDefault(), "DefaultPreparerOptions");
    }

    /** "av_config.xml" is a MATSim config file
     * "waitForClients" boolean value false / true that indicates if the server should wait before starting for a
     * viewer to connect.
     * 
     * @return */
    public static Properties getServerDefault() {
        Properties returnP = new Properties();
        returnP.setProperty("av_config.xml", "/home/clruch/Simulations/2017_07_13_Sioux_TestBed/av_config.xml");
        returnP.setProperty("waitForClients", "false");
        return returnP;
    }

    public static void saveServerDefault() throws IOException {
        saveProperties(getServerDefault(), "DefaultServerOptions");
    }

    /** "av_config.xml" is a MATSim config file
     * "ReferenceFrame" indicates the coordinate system, reference frame to be used, e.g. SWITZERLAND, SIOUXFALLS,...
     * 
     * @return */
    public static Properties getViewerDefault() {
        Properties returnP = new Properties();
        returnP.setProperty("av_config.xml", "/home/clruch/Simulations/2017_07_13_Sioux_TestBed/av_config.xml");
        returnP.setProperty("ReferenceFrame", "SIOUXFALLS");
        return returnP;
    }

    public static void saveViewerDefault() throws IOException {
        saveProperties(getViewerDefault(), "DefaultViewerOptions");
    }

    private static void saveProperties(Properties prop, String filename) throws IOException {
        File defaultFile = new File(filename);
        FileOutputStream ostream = new FileOutputStream(defaultFile);
        prop.store(ostream, "This is a sample config file.");
    }

}
