package playground.toronto.transitnetworkutils;

import java.io.IOException;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.Nodes2ESRIShape;



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
		
		//TTC schedule files
		final String TTCSTREETCARSCHEDULE = "C:/Users/Peter Work/Desktop/NETWORK DATA/OPERATOR DATA/TTC/TTC GTFS Current/streetcar/streetcar_schedule.xml";
		final String TTCOUTPUTSCHEDULE = "C:/Users/Peter Work/Desktop/NETWORK DATA/OPERATOR DATA/TTC/TTC GTFS Current/streetcar/new_streetcar_schedule.xml";
		final String TTCGTFSFOLDER = "C:/Users/Peter Work/Desktop/NETWORK DATA/OPERATOR DATA/TTC/TTC GTFS Current/streetcar";
		final String TESTNETWORKFILE = "C:/Users/Peter Work/Desktop/NETWORK DATA/MATSIM NETWORK/GTFS/tempnTemp.xml";
		
		//Other operator files
		final String BRAMPTONSCHEDULEFILENAME = "C:\\Users\\Peter Work\\Desktop\\NETWORK DATA\\OPERATOR DATA\\Brampton\\Brampton2006.csv";
		final String HSRSCHEDULENAME = "C:\\Users\\Peter Work\\Desktop\\NETWORK DATA\\Parsing\\Hamilton 2006 Weekday Schedule.csv";
		final String MISSISSAUGASCHEDULENAME = "C:\\Users\\Peter Work\\Desktop\\NETWORK DATA\\Parsing\\mississauga.txt";
		
		//Network Files
		final String NETWORKFILENAME = "C:/Users/Peter Work/Desktop/NETWORK DATA/MATSIM NETWORK/with turns/output_network.xml";
		final String GLOBALNETWORKFILENAME = "C:\\Users\\Peter Work\\Desktop\\NETWORK DATA\\MATSIM NETWORK\\toronto_globalNetwork_v1.xml";
		
		
		//Other files
		final String configname = "C:\\Users\\Peter Work\\Desktop\\NETWORK DATA\\MATSIM NETWORK\\testconfig.xml";	
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(TESTNETWORKFILE);
		
		Nodes2ESRIShape f = new Nodes2ESRIShape(network, "C:/Users/Peter Work/Desktop/NETWORK DATA/MAPS/SHAPEFILES/streetcar_nodes.shp", TransformationFactory.WGS84);
		f.write();
		
		Links2ESRIShape e = new Links2ESRIShape(network, "C:/Users/Peter Work/Desktop/NETWORK DATA/MAPS/SHAPEFILES/streetcarnetwork.shp", TransformationFactory.WGS84);
		e.write();
		
		TransitScheduleUtils.PostProcessGTFS(TTCSTREETCARSCHEDULE, TTCGTFSFOLDER, TTCOUTPUTSCHEDULE);
		
		Config config = ConfigUtils.loadConfig(configname);
		
		//TransitScheduleUtils.AggregateDepartureTimes(schedule, outFile)
		
		
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


