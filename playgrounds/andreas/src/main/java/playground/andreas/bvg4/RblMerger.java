package playground.andreas.bvg4;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class RblMerger {
	
	private static final Logger log = Logger.getLogger(RblMerger.class);

	public static void main(String[] args) {
		String networkFile ="D:/berlin_bvg3/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/network.final.xml.gz";
		String transitScheduleInFile = "D:/berlin_bvg3/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/transitSchedule.xml.gz";
		String fahrtData187 = "F:/bvg4/input/01_03-05/fahrt_ist_187_478847894790csv.lst";
		String fahrtDataM41 = "F:/bvg4/input/01_03-05/fahrt_ist_M41_478847894790csv.lst"; 
		String fahrzeitData187 = "F:/bvg4/input/01_03-05/fahrzeit_ist_187_478847894790csv.lst"; 
		String fahrzeitDataM41 = "F:/bvg4/input/01_03-05/fahrzeit_ist_M41_478847894790csv.lst"; 
		
		
		RblMerger rblMerger = new RblMerger();		
		rblMerger.init(networkFile, transitScheduleInFile);
		rblMerger.readRbl(fahrtData187, fahrtDataM41, fahrzeitData187, fahrzeitDataM41);
		rblMerger.validate(rblMerger.fahrzeitData187);
		rblMerger.validate(rblMerger.fahrzeitDataM41);
	}

	private ScenarioImpl scenario;
	private LinkedList<FahrtEvent> fahrtData187;
	private LinkedList<FahrtEvent> fahrtDataM41;
	private LinkedList<FahrzeitEvent> fahrzeitData187;
	private LinkedList<FahrzeitEvent> fahrzeitDataM41;
	
	private void readRbl(String fahrtData187, String fahrtDataM41, String fahrzeitData187, String fahrzeitDataM41) {
		try {
			this.fahrtData187 = ReadRBLfahrt.readFahrtEvents(fahrtData187);
			this.fahrtDataM41 = ReadRBLfahrt.readFahrtEvents(fahrtDataM41);
			this.fahrzeitData187 = ReadRBLfahrzeit.readFahrzeitEvents(fahrzeitData187);
			this.fahrzeitDataM41 = ReadRBLfahrzeit.readFahrzeitEvents(fahrzeitDataM41);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	private void init(String networkFile, String transitScheduleInFile) {
		Config config = ConfigUtils.createConfig();
		config.scenario().setUseTransit(true);
		config.network().setInputFile(networkFile);
		config.transit().setTransitScheduleFile(transitScheduleInFile);
		this.scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);;
	}

	private void validate(LinkedList<FahrzeitEvent> fahrzeitData) {
		Set<String> stopIdsFromRbl = new TreeSet<String>();
		Set<String> stopIdsFromSchedule = new TreeSet<String>();
		
		for (FahrzeitEvent fahrzeitEvent : fahrzeitData) {
			stopIdsFromRbl.add(fahrzeitEvent.getStopId().toString());
		}
		
		int stopsFound = 0;
		int stopsNotFound = 0;
		for (TransitStopFacility stopFacility : this.scenario.getTransitSchedule().getFacilities().values()) {
			stopIdsFromSchedule.add(stopFacility.getId().toString());
		}
		
		for (String stopId : stopIdsFromRbl) {
			if(stopIdsFromSchedule.contains(stopId)){
				stopsFound++;
			} else {
				stopsNotFound++;
			}
		}
		
		log.info("Found " + stopsFound + " stop ids from rbl in schedule");
		
	}
	
}
