package playground.mmoyo.analysis.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import playground.mmoyo.utils.DataLoader;

/** Removes stations whose max volume counts does not reach a given number. */
public class StationFilter {
	private static final Logger log = Logger.getLogger(StationFilter.class);
	
	public void filterStations(Counts counts, int maxVolumeValue){
		int originalSize = counts.getCounts().size();
		List<Id> removableStationsList = new ArrayList<Id>(); //
		for(Map.Entry <Id,Count> entry: counts.getCounts().entrySet()){
			if (entry.getValue().getMaxVolume().getValue() < maxVolumeValue){
				removableStationsList.add(entry.getKey());
			}
		}
		for (Id id : removableStationsList){
			counts.getCounts().remove(id);	
		}
		
		log.warn("stations were filtered: " + (originalSize - counts.getCounts().size()) + " out of "  + originalSize + " were removed ");
	}
	
	public static void main(String[] args) {
		String countsFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/chen/counts_occupancy_M44_344.xml";
		Counts counts = new DataLoader().readCounts(countsFile);
		new StationFilter().filterStations(counts, 50);
		
		CountsWriter countsWriter = new CountsWriter(counts);
		String filteredCountsFile = "../playgrounds/mmoyo/output/filteredCounts.xml";
		countsWriter.write(filteredCountsFile);
	}

}