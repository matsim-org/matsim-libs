/**
 * 
 */
package cottbusAnalysis;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import playground.dgrether.DgPaths;

/**
 * @author tthunig
 * @deprecated
 */
public class TtAnalyseComTravelTimesAndRoutes {
	
	private static final Logger log = Logger.getLogger(TtAnalyseComTravelTimesAndRoutes.class);
	
	public static void main(String[] args) {
		
		int runNumber;
		// 1980 = inner city area, no time choice, opt50 signals
//		runNumber = 1980;
	    // 1995 = bb 5000, no time choice, opt50 signals
//		runNumber = 1995;
		 // 2023 = bb 5000, no time choice, opt50 signals with person ids of newer cplex opt
		runNumber = 2023;
	    int iteration = 1400;
		String eventFile = DgPaths.RUNSSVN + 
				"run"+runNumber+"/ITERS/it."+iteration+"/"+runNumber+"."+iteration+".events.xml.gz";
		String networkFile = DgPaths.RUNSSVN + 
				"run"+runNumber+"/"+runNumber+".output_network.xml.gz";
		String plansFile = DgPaths.RUNSSVN + 
				"run"+runNumber+"/"+runNumber+".output_plans.xml.gz";
		String xmlOutputFile = DgPaths.RUNSSVN + 
	    		"run"+runNumber+"/"+runNumber+"."+iteration+".comodityTravelTimes.xml";
		String shpOutputFile = DgPaths.RUNSSVN + 
	    		"run"+runNumber+"/"+runNumber+"."+iteration+".comodityRoutes.shp";
				
	    		
	    EventsManager eventsManager = EventsUtils.createEventsManager();
	    TtCalculateComTravelTimes travelTimesHandler = new TtCalculateComTravelTimes();
	    eventsManager.addHandler(travelTimesHandler);
	    TtDetermineComRoutes routesHandler = new TtDetermineComRoutes();
	    eventsManager.addHandler(routesHandler);
	 
	    // Connect a file reader to the EventsManager and read in the event file.
	    MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
	    reader.readFile(eventFile);
	    log.info("Events file read!");
	   
	    TtWriteXML.writeComTravelTimes(travelTimesHandler.getComTravelTimes(), xmlOutputFile);
	    log.info("travel times written as xml");
	    
	    TtWriteSHP shpWriter = new TtWriteSHP();
	    shpWriter.writeComRoutes(routesHandler, travelTimesHandler, networkFile, plansFile, shpOutputFile);
	    log.info("routes written as shp file");
	}

}
