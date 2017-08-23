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

    public static Properties getViewerDefault() {
        Properties returnP = new Properties();
        returnP.setProperty("av_config.xml", "/home/clruch/Simulations/2017_07_13_Sioux_TestBed/av_config.xml");
        returnP.setProperty("ReferenceFrame", "SIOUXFALLS");
        return returnP;
    }

    
    public static void saveViewerDefault() throws IOException{
        File viewerDefaultFile = new File("ViewerDefaultOptions");
        FileOutputStream ostream = new FileOutputStream(viewerDefaultFile);
        getViewerDefault().store(ostream, "These is a sample config file for the viewer.");
    }
}
