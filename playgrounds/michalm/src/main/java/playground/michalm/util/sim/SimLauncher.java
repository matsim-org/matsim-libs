package playground.michalm.util.sim;

import java.util.*;

import org.matsim.core.controler.*;


public class SimLauncher
{
    public static void main(String[] args)
    {
        String dirName;
        String cfgFileName;

        if (args.length == 1 && args[0].equals("test")) {// for testing
            dirName = "d:\\PP-rad\\taxi\\orig-mielec-nowe-OD\\";
            cfgFileName = "siec-config.xml";
        }
        else if (args.length == 2) {
            dirName = args[0];
            cfgFileName = args[1];
        }
        else {
            throw new IllegalArgumentException("Incorrect program arguments: "
                    + Arrays.toString(args));
        }

        Controler controler = new Controler(new String[] { dirName + cfgFileName });
        controler.setOverwriteFiles(true);
        controler.run();
    }
}
