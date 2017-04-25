/**
 * 
 */
package playground.tschlenther.parkingSearch.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.parkingsearch.evaluation.ParkingSearchAndEgressTimeEvaluator;
import org.matsim.contrib.parking.parkingsearch.evaluation.ParkingSearchEvaluator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.jbischoff.csberlin.evaluation.ParkingSearchEventsReader;

/**
 * @author schlenther,jbischoff
 */
public class TSParkingSearchEvaluation {

	private HashMap<String,SearchTimeEvaluator> zoneHandlers;
	private static Logger log = Logger.getLogger(TSParkingSearchEvaluation.class);
	
	public TSParkingSearchEvaluation(String[] pathToZoneTxtFiles){
		this.zoneHandlers = new HashMap<String,SearchTimeEvaluator>();
		for(String pathToZoneFile : pathToZoneTxtFiles){
			String zone = pathToZoneFile.substring(pathToZoneFile.lastIndexOf("/")+1, pathToZoneFile.lastIndexOf("."));
			SearchTimeEvaluator handler = new SearchTimeEvaluator(readLinks(pathToZoneFile));
			this.zoneHandlers.put(zone, handler);
		}
	}
	
	public void analyseRun(String runDirectory, int highestIterationNumber){
		log.info("STARTING TO ANALYSE RUN AT " + runDirectory);
		
		EventsManager events = EventsUtils.createEventsManager();
		for(String zone : this.zoneHandlers.keySet()){
			events.addHandler(this.zoneHandlers.get(zone));
		}
		ParkingSearchEvaluator egressWalkStatistics = new ParkingSearchEvaluator();
		events.addHandler(egressWalkStatistics);
		
		for (int i = 0; i <= highestIterationNumber; i++ ){
			events.resetHandlers(i);
			String analysisDir = runDirectory + "/it." + i ;
			log.info("RUNNING ANALYSIS OF ITERATION " +i);
			if(!new File(analysisDir).exists()) throw new IllegalArgumentException("couldn't find " + analysisDir);
			analysisDir += "/";
			new ParkingSearchEventsReader(events).readFile(analysisDir + i + ".events.xml.gz");
			
			for(String zone : this.zoneHandlers.keySet()){
				zoneHandlers.get(zone).writeLinkTimeStamps(analysisDir + zone + "ParkStamps_it" + i + ".csv");
				zoneHandlers.get(zone).writeStats(analysisDir + zone + "ParkStats_it" + i + ".csv");
			}
			
			egressWalkStatistics.writeEgressWalkStatistics(analysisDir);
		}

		log.info("FINISHED ANALYSIS");
		
	}
	
	private Set<Id<Link>> readLinks(String fileName) {
		final Set<Id<Link>> links = new HashSet<>();
		TabularFileParserConfig config = new TabularFileParserConfig();
	    config.setDelimiterTags(new String[] {"\t"});
	    config.setFileName(fileName);
	    new TabularFileParser().parse(config, new TabularFileHandler() {
			@Override
			public void startRow(String[] row) {
				links.add(Id.createLinkId(row[0]));
			}
		});

		
		return links;
	}
	
	public static void main(String[] args){
		String[] zones = new String[2];
		zones[0] = "C:/Users/Work/Bachelor Arbeit/input/Berlin/Klausener.txt";
		zones[1] = "C:/Users/Work/Bachelor Arbeit/input/Berlin/Mierendorff.txt";
		TSParkingSearchEvaluation eval = new TSParkingSearchEvaluation(zones);
		
		String runDir = "C:/Users/Work/Bachelor Arbeit/RUNS/SERIOUS_BUGFIX/Benenson/Berlin/changeEvents/RUN_130417_22.14_1_3_1000_350_750_P.B.4.BUGFIX/ITERS";
		eval.analyseRun(runDir, 7);
		
	}
	
}
