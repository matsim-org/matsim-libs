package playground.michalm.poznan;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.run.Controler;


public class PoznanControler
{
    private static Logger log = Logger.getLogger(PoznanControler.class);


    public static void main(final String[] args)
    {
        //        String configFile = "d:/bartekp/poznan/input/poznan/22_11/matsim/poznan_22_11.xml";
        String configFile = "d:/PP-rad/poznan/test/config.xml";

        //MatsimRandom.reset(43);
        Controler controler = new Controler(configFile);
        controler.setOverwriteFiles(true);
        controler.run();

        Config cf = controler.getScenario().getConfig();
        String dir = cf.controler().getOutputDirectory();
        log.warn("Output is in " + dir);
    }
}
