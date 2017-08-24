/**
 * 
 */
package playground.clruch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/** @author Claudio Ruch */
public enum DefaultOptions {
    ;

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
        saveProperties(getViewerDefault(), "ViewerDefaultOptions");
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
        saveProperties(getServerDefault(), "ServerDefaultOptions");
    }

    private static void saveProperties(Properties prop, String filename) throws IOException {
        File defaultFile = new File(filename);
        FileOutputStream ostream = new FileOutputStream(defaultFile);
        prop.store(ostream, "These is a sample config file.");
    }

}
