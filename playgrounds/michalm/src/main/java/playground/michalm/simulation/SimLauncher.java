package playground.michalm.simulation;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.controler.*;
import org.matsim.core.router.util.*;


public class SimLauncher
{
    public static void main(String[] args)
    {
        String dirName;
        String cfgFileName;

        if (args.length == 1 && args[0].equals("test")) {// for testing
            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec1\\";
            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec2\\";
            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\Paj\\";
            dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\NSE\\";
            cfgFileName = "config-verB.xml";

            dirName = "d:\\PP-dyplomy\\2010_11-mgr\\test_network\\";
            cfgFileName = "config-verB.xml"; 
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
        
        TravelTime travelTime = controler.getTravelTimeCalculator();

        Map<Id, Link> links = controler.getNetwork().getLinks();
        Id idB = controler.getScenario().createId("B");
        Link linkB = links.get(idB);
        
        for (int i = 0; i < 2 * 60 * 60; i += 5*60) {//each 5 minutes during the first 2 hours
            int m = i/60;
            System.out.println(m + " : " + travelTime.getLinkTravelTime(linkB, i));
        }
    }
}
