package playground.tschlenther.parkingSearch.analysis;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.jbischoff.csberlin.evaluation.ParkingSearchEventsReader;

public class RunSingleAnalysis {
	
	private static String runDir = "C:/Users/Work/Bachelor Arbeit/RUNS/SERIOUS/Memory/Berlin/changeEvents/RUN_110417_09.58_MEMORY_RANDOM_IF_ALL_KNOWN/ITERS/it.10/";
	private static String pathToZoneFileOne = "C:/Users/Work/Bachelor Arbeit/input/Berlin/Klausener.txt";
	private static String pathToZoneFileTwo = "C:/Users/Work/Bachelor Arbeit/input/Berlin/Mierendorff.txt";
	private static String eventsFile = runDir + "10.events.xml.gz";
	
	public static void main(String[] args) {
		
		
		String zoneOne = pathToZoneFileOne.substring(pathToZoneFileOne.lastIndexOf("/")+1, pathToZoneFileOne.lastIndexOf("."));
		String zoneTwo = pathToZoneFileTwo.substring(pathToZoneFileTwo.lastIndexOf("/")+1, pathToZoneFileTwo.lastIndexOf("."));
		
		EventsManager events = EventsUtils.createEventsManager();
		
		SearchTimeEvaluator handlerOne = new SearchTimeEvaluator(readLinks(pathToZoneFileOne));
		SearchTimeEvaluator handlerTwo = new SearchTimeEvaluator(readLinks(pathToZoneFileTwo));

		events.addHandler(handlerOne);
		events.addHandler(handlerTwo);
		
		
		
		new ParkingSearchEventsReader(events).readFile(eventsFile);
		
		handlerOne.writeStats(runDir + zoneOne + "Stats_VStd_Single.csv");
		handlerOne.writeLinkTimeStamps(runDir + zoneOne + "TimeStamps_Single.csv");


		handlerTwo.writeStats(runDir + zoneTwo + "Stats_VStd_Single.csv");
		handlerTwo.writeLinkTimeStamps(runDir + zoneTwo + "TimeStamps_Single.csv");
		
		
		Logger.getLogger(RunSingleAnalysis.class).info("FINISHED SINGLE ANALYSIS");
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
