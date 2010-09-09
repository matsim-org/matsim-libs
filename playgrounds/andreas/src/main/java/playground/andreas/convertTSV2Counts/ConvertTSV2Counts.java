package playground.andreas.convertTSV2Counts;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

public class ConvertTSV2Counts {
	
	private static final Logger log = Logger.getLogger(ConvertTSV2Counts.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String workingDir = "F:\\cgtest\\";
		String startTag = "Di-Do";
		String countStationsFileName = workingDir + "DZS-Koordinaten.csv";
		String countsOutFile = workingDir + startTag + "_counts.xml";
		
		log.info("Reading count stations from " + countStationsFileName + " ...");
		List<CountStationDataBox> countStations = ReadCountStations.readCountStations(countStationsFileName);
		
		log.info("Building count station map by reading " + countStations.size() + " stations");
		HashMap<String, CountStationDataBox> countStationsMap = new HashMap<String, CountStationDataBox>();
		for (CountStationDataBox countStation : countStations) {
			if(countStationsMap.get(countStation.getShortName()) == null) {
				countStationsMap.put(countStation.getShortName(), countStation);
			} else {
				log.info("Duplicate count station found: " + countStation.toString());
			}
		}
		log.info("Final map contains " + countStationsMap.size() + " stations");
		
		log.info("Reading counts...");
		Counts counts = new Counts();
		// set some nonsense, cause writer allows for empty fields, but reader complains
		counts.setYear(2009);
		counts.setName("hab ich nicht");
		counts.setLayer("hab ich keinen");
		
				
		for (CountStationDataBox countStation : countStationsMap.values()) {
			counts.createCount(new IdImpl(countStation.getShortName()), countStation.getShortName());
			counts.getCount(new IdImpl(countStation.getShortName())).setCoord(countStation.getCoord());
			String filename = workingDir + "Wochenübersicht_" + countStation.getShortName() + ".tsv"; 
			ReadCountDataForWeek.readCountDataForWeek(filename, counts.getCount(new IdImpl(countStation.getShortName())), startTag);			
		}
		
		Set<Id> countIds = new TreeSet<Id>(counts.getCounts().keySet());
		for (Id countId : countIds) {
			if(counts.getCount(countId).getVolumes().isEmpty() == true){
				counts.getCounts().remove(countId);
			}
		}
				
		log.info("Converted counts data for " + counts.getCounts().size() + " stations");

		CountsWriter countsWriter = new CountsWriter(counts);
		countsWriter.write(countsOutFile);
		log.info("Counts written to " + countsOutFile);

		log.info("Finish...");

	}

}
