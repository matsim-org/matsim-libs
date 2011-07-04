package playground.michalm.vrp.offline;

import java.io.*;
import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.network.*;
import org.matsim.core.population.*;
import org.matsim.core.scenario.*;
import org.matsim.core.utils.misc.*;


public class CombinePlansWithVRPSchedulePlans
{
    public static void main(String... args)
        throws IOException
    {
        String dirName;
        String networkFileName;
        String plansFileName;
        String vrpDirName;

        if (args.length == 1 && args[0].equals("test")) {// for testing
            dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec1\\";
            networkFileName = dirName + "network.xml";
            plansFileName = dirName + "\\output\\config-verB\\output_plans.xml.gz";
            vrpDirName = dirName + "dvrp\\";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec2\\";
            // networkFileName = dirName + "network.xml";
            // plansFileName = dirName + "\\output\\config-verB\\output_plans.xml.gz";
            // vrpDirName = dirName + "dvrp\\";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\Paj\\";
            // networkFileName = dirName + "network.xml";
            // plansFileName = dirName + "\\output\\config-verB\\output_plans.xml.gz";
            // vrpDirName = dirName + "dvrp\\";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\NSE\\";
            // networkFileName = dirName + "network.xml";
            // plansFileName = dirName + "\\output\\config-verB\\output_plans.xml.gz";
            // vrpDirName = dirName + "dvrp\\";
        }
        else if (args.length == 4) {
            dirName = args[0];
            networkFileName = dirName + args[1];
            plansFileName = dirName + args[2];
            vrpDirName = dirName + args[3];
        }
        else {
            throw new IllegalArgumentException("Incorrect program arguments: "
                    + Arrays.toString(args));
        }

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        new MatsimNetworkReader(scenario).readFile(networkFileName);
        
        new MatsimPopulationReader(scenario).readFile(plansFileName);

        String vrpOutDirName = vrpDirName + "\\output";
        new MatsimPopulationReader(scenario).readFile(vrpOutDirName + "\\vrpDriverPlans.xml");

        new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeV4(dirName
                + "\\combinedPlans.xml");
    }
}
