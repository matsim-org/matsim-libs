/**
 * 
 */
package playground.tschlenther.parkingSearch.analysis;

import java.io.File;
import java.util.HashSet;
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
	private final static String mierendorffLinks = "C:/Users/Work/Bachelor Arbeit/input/GridNet/Zonen/Zielzone.txt";
	private final static String klausenerLinks = "C:/Users/Work/Bachelor Arbeit/input/GridNet/Zonen/Homezone.txt";
	
	private static Logger log = Logger.getLogger(TSParkingSearchEvaluation.class);
	
	/**
	 * wenn analysisDir == null, dann wird analysis-output nach runDirectory + "/ANALYSIS/" geschrieben
	 * @param runDirectory
	 * @param analysisDir
	 */
	public static void analyseRun(String runDirectory, int highestIterationNumber){
		
		log.info("STARTING TO ANALYSE RUN AT " + runDirectory);
		
//		if (analysisDir == null){
//			analysisDir = runDirectory + "/ANALYSIS";
//			File f = new File(analysisDir); 
//			if(!f.exists()){
//				if(!f.mkdirs())	throw new RuntimeException("couldn't create analysis folder at " + analysisDir);
//			}
//		}
		
		
//		Network network = NetworkUtils.createNetwork();
//		new MatsimNetworkReader(network).readFile(runDirectory + "/output_network.xml.gz");
		
		
		EventsManager events = EventsUtils.createEventsManager();
	
//		ParkingSearchAndEgressTimeEvaluator mierendorffEval = new ParkingSearchAndEgressTimeEvaluator(readLinks("../../../shared-svn/projects/bmw_carsharing/data/gis/mierendorfflinks.txt"),network);	
//		ParkingSearchAndEgressTimeEvaluator klausEval = new ParkingSearchAndEgressTimeEvaluator(readLinks("../../../shared-svn/projects/bmw_carsharing/data/gis/klausnerlinks.txt"),network);	

		SearchTimeEvaluator klausEval = new SearchTimeEvaluator(readLinks(klausenerLinks));	
		SearchTimeEvaluator mierendorffEval = new SearchTimeEvaluator(readLinks(mierendorffLinks));	
//		
		ParkingSearchEvaluator egressWalkStatistics = new ParkingSearchEvaluator();
		events.addHandler(mierendorffEval);
		events.addHandler(klausEval);
		events.addHandler(egressWalkStatistics);
		
		for (int i = 0; i <= highestIterationNumber; i++ ){
			events.resetHandlers(i);
			String analysisDir = runDirectory + "/it." + i ;
			log.info("RUNNING ANALYSIS OF ITERATION " +i);
			if(!new File(analysisDir).exists()) throw new IllegalArgumentException("couldn't find " + analysisDir);
			
			new ParkingSearchEventsReader(events).readFile(analysisDir + "/" + i + ".events.xml.gz");
			
			mierendorffEval.writeStats(analysisDir+ "/zielZoneParkvorgaengeUndZeiten_it" + i + ".csv");
			mierendorffEval.writeLinkTimeStamps(analysisDir+"/zielZoneParkStamps_it" + i + ".csv");
			klausEval.writeStats(analysisDir+"/homeZoneParkvorgaengeUndZeiten_it" + i + ".csv");
			klausEval.writeLinkTimeStamps(analysisDir+"/homeZoneParkStamps_it" + i + ".csv");
			
			egressWalkStatistics.writeEgressWalkStatistics(analysisDir);
		}

		log.info("FINISHED ANALYSIS");
		
	}

	private static Set<Id<Link>> readLinks(String fileName) {
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
	
	
	
}
