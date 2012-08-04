package playground.toronto.transitnetworkutils;

import java.io.IOException;



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
		
		String goScheduleFile = args[0];
		String GTFSfolder = args[1];
		String tempfile = args[2];
		
		
		//Config config = ConfigUtils.loadConfig(configname);
		
		
		ScheduleParser converterGOTransit = new ScheduleParser();
		converterGOTransit.ImportGOSchedule(goScheduleFile);
		converterGOTransit.ValidateSchedule(tempfile);
		converterGOTransit.ExportGtfsFiles(GTFSfolder);
		
		
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


