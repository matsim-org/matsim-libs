package playground.toronto.transitnetworkutils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;


import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import org.matsim.core.config.ConfigUtils;


import org.matsim.pt.transitSchedule.api.TransitSchedule;

import GTFS2PTSchedule.GTFS2MATSimTransitSchedule;

import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.router.util.PreProcessDijkstra;;



/**
 * This is my in-development controller for processing the conversion of the Toronto transit network to MATSim format.
 * So far, all it does is make a few calls to the ScheduleConverter class, which parses the various formats of
 * transit schedules from the transit operators. I'm also working on some code to do a little bit of map-matching,
 * creating link-sequences that MATSim needs. 
 * 
 * Eventually, I'd like TODO a cleaner version of a Toronto MATSim controller, suitable for general use in Toronto.
 * 
 * @author pkucirek
 *
 */
public class ScheduleParserController {
	
	public static void main(final String[] args) throws IOException{
		
		//GO Files
		final String GOSCHEDULENAME = "C:\\Users\\Peter Work\\Desktop\\NETWORK DATA\\Parsing\\GO 2006 Weekday Schedule.csv";
		final String GOSTOPSNAME = "C:\\Users\\Peter Work\\Desktop\\NETWORK DATA\\Parsing\\gostops_withReferences.txt";
		//final String GOROUTESNAME = "C:\\Users\\Peter Admin\\Desktop\\NETWORK DATA\\Parsing\\goroutes.txt";
		
		//Other operator files
		final String BRAMPTONSCHEDULEFILENAME = "C:\\Users\\Peter Work\\Desktop\\NETWORK DATA\\OPERATOR DATA\\Brampton\\Brampton2006.csv";
		final String HSRSCHEDULENAME = "C:\\Users\\Peter Work\\Desktop\\NETWORK DATA\\Parsing\\Hamilton 2006 Weekday Schedule.csv";
		final String MISSISSAUGASCHEDULENAME = "C:\\Users\\Peter Work\\Desktop\\NETWORK DATA\\Parsing\\mississauga.txt";
		
		//Network Files
		final String NETWORKFILENAME = "C:/Users/Peter Work/Desktop/NETWORK DATA/MATSIM NETWORK/with turns/output_network.xml";
		final String GLOBALNETWORKFILENAME = "C:\\Users\\Peter Work\\Desktop\\NETWORK DATA\\MATSIM NETWORK\\toronto_globalNetwork_v1.xml";
		
		
		//Other files
		final String configname = "C:\\Users\\Peter Work\\Desktop\\NETWORK DATA\\MATSIM NETWORK\\testconfig.xml";	
		
		Config config = ConfigUtils.loadConfig(configname);
		
		/*
		Scenario s = ScenarioUtils.loadScenario(config);
		MATSim2EMME wtr = new MATSim2EMME(s.getNetwork());
		wtr.write("C:/Users/Peter Admin/Desktop/NETWORK DATA/EMME NETWORK/Toronto_Network/Database/test.211");*/
		
		/*
		config = ConfigUtils.loadConfig(configname);
		
		//ScheduleConverter convertHSR = new ScheduleConverter();
		ScheduleConverter converterGOTransit = new ScheduleConverter();
		//ScheduleConverter convertMississauga = new ScheduleConverter();

		converterGOTransit.ImportGOSchedule(GOSCHEDULENAME);
		//converterGOTransit.ExportStopList(GOSTOPSNAME, "GO Transit");
		//converterGOTransit.ExportRouteSummary(GOROUTESNAME, "GO Transit");
		//converterGOTransit.ExportDetailedRoutes("GO Transit", "C:\\Users\\Peter Admin\\Desktop\\NETWORK DATA\\Parsing\\routes");
		
		//convertMississauga.ImportHastusSchedule(MISSISSAUGASCHEDULENAME);
		
		//convertHSR.ImportGOSchedule(HSRSCHEDULENAME);
		
		converterGOTransit.ExportRouteSummary("C:\\Users\\Peter Admin\\Desktop\\NETWORK DATA\\Parsing\\routes\\summary.txt", "GO");
		
		
		MapMatching matcher = new MapMatching(configname);
		matcher.matchRoutes(GOSTOPSNAME, converterGOTransit.getRoutes(), "C:\\Users\\Peter Admin\\Desktop\\NETWORK DATA\\Parsing\\routes");
		
		//System.out.println("done.");
		
		*/
		
	}
		

}


