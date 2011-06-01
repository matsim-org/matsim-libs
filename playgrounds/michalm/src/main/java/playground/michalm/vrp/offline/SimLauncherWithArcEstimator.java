package playground.michalm.vrp.offline;

import java.io.*;
import java.util.*;

import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.network.*;

import org.matsim.api.core.v01.*;
import org.matsim.core.controler.*;

import pl.poznan.put.vrp.dynamic.data.*;
import pl.poznan.put.vrp.dynamic.data.file.*;


public class SimLauncherWithArcEstimator
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

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec2\\";
            // cfgFileName = dirName + "config-verB.xml";
            // vrpDirName = dirName + "dvrp\\";
            // vrpStaticFileName = "A102.txt";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\Paj\\";
            // cfgFileName = dirName + "config-verB.xml";
            // vrpDirName = dirName + "dvrp\\";
            // vrpStaticFileName = "C101.txt";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\NSE\\";
            // cfgFileName = dirName + "config-verB.xml";
            // vrpDirName = dirName + "dvrp\\";
            // vrpStaticFileName = "C102.txt";

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

        Scenario scenario = controler.getScenario();
        VRPData vrpData = LacknerReader.parseStaticFile(vrpDirName, vrpStaticFileName,
                new MATSimVertexImpl.Builder(scenario));
        MATSimVRPData data = new MATSimVRPData(vrpData, scenario);

        ShortestPathsFinder spf = new ShortestPathsFinder(data);
        spf.findShortestPaths(controler);
        spf.writeShortestPaths(vrpArcTimesFileName, vrpArcCostsFileName, vrpArcPathsFileName);
    }
}
