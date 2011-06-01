package playground.michalm.vrp.offline;

import java.io.*;
import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.controler.*;
import org.matsim.core.router.util.*;


public class SimLauncherWithCheckFIFO
{
    public static void main(String... args)
        throws IOException
    {
        String dirName;
        String cfgFileName;
        String vrpDirName;
        String vrpStaticFileName;
        String vrpArcTimesFileName;
        String vrpArcCostsFileName;
        String vrpArcPathsFileName;

        if (args.length == 1 && args[0].equals("test")) {// for testing
             dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec1\\";
             cfgFileName = dirName + "config-verB.xml";
             vrpDirName = dirName + "dvrp\\";
             vrpStaticFileName = "A101.txt";

//             dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec2\\";
//             cfgFileName = dirName + "config-verB.xml";
//             vrpDirName = dirName + "dvrp\\";
//             vrpStaticFileName = "A102.txt";

//             dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\Paj\\";
//             cfgFileName = dirName + "config-verB.xml";
//             vrpDirName = dirName + "dvrp\\";
//             vrpStaticFileName = "C101.txt";

//            dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\NSE\\";
//            cfgFileName = dirName + "config-verB.xml";
//            vrpDirName = dirName + "dvrp\\";
//            vrpStaticFileName = "C102.txt";

            vrpArcTimesFileName = vrpDirName + "arc_times.txt";
            vrpArcCostsFileName = vrpDirName + "arc_costs.txt";
            vrpArcPathsFileName = vrpDirName + "arc_paths.txt";
        }
        else if (args.length == 7) {
            dirName = args[0];
            cfgFileName = dirName + args[1];
            vrpDirName = dirName + args[2];
            vrpStaticFileName = args[3];
            vrpArcTimesFileName = vrpDirName + args[4];
            vrpArcCostsFileName = vrpDirName + args[5];
            vrpArcPathsFileName = vrpDirName + args[6];
        }
        else {
            throw new IllegalArgumentException("Incorrect program arguments: "
                    + Arrays.toString(args));
        }

        Controler controler = new Controler(new String[] { cfgFileName });
        controler.setOverwriteFiles(true);
        
        controler.run();

        checkFIFOProperty(controler);
    }


    private static void checkFIFOProperty(Controler controler)
    {
        TravelTime travelTime = controler.getTravelTimeCalculator();

        Network network = controler.getScenario().getNetwork();

        for (Link link : network.getLinks().values()) {
            double prevTime = travelTime.getLinkTravelTime(link, 0);

            for (int i = 10 * 60; i < 24 * 60 * 60; i += 10 * 60) {
                double currTime = travelTime.getLinkTravelTime(link, i);

                if (prevTime - currTime > 10 * 60) {
                    System.err
                            .println("Warning!!! FIFO property was broken! : " + (prevTime - currTime));
                    
                    
                    travelTime.getLinkTravelTime(link, i);
                }
                
                prevTime = currTime;
            }
        }
    }
}
