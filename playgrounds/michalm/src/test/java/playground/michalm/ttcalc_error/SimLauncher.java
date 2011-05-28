package playground.michalm.ttcalc_error;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.controler.*;
import org.matsim.core.router.util.*;


public class SimLauncher
{
    public static void main(String[] args)
    {
        String cfg = "src/test/java/playground/michalm/ttcalc_error/error_1/config.xml";
        //cfg = "src/test/java/playground/michalm/ttcalc_error/error_2/config.xml";

        Controler controler = new Controler(new String[] { cfg });
        controler.setOverwriteFiles(true);
        controler.run();
        
        TravelTime travelTime = controler.getTravelTimeCalculator();

        Map<Id, Link> links = controler.getNetwork().getLinks();
        Id idB = controler.getScenario().createId("B");
        Link linkB = links.get(idB);
        
        System.out.println("\ndeparture time [min] : travel time [s]\n");
        
        for (int i = 0; i < 2 * 60 * 60; i += 5*60) {//each 5 minutes during the first 2 hours
            int m = i/60;
            System.out.println(m + " : " + travelTime.getLinkTravelTime(linkB, i));
        }
    }
}
