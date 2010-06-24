package playground.mmoyo.analysis.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.CountsWriter;

import playground.mmoyo.utils.TransScenarioLoader;

/**reads counts from an scenario and removes stations whose max volume counts does not reach a given number. 
 * Writes a copy of the filtered stations in the output directory of the scenario config*/
public class StationFilter {
	private static final Logger log = Logger.getLogger(StationFilter.class);
	private double maxVolumeValue;
	private ScenarioImpl scenario;
	
	public StationFilter(ScenarioImpl scenario, double maxVolumeValue){
		this.scenario = scenario;
		this.maxVolumeValue = maxVolumeValue;
	}

	public void getStopsWithMinCount(){
		filterStations (this.scenario.getConfig(),"inputBoardCountsFile", "boarding");
		filterStations (this.scenario.getConfig(),"inputOccupancyCountsFile", "occupancy");
		filterStations (this.scenario.getConfig(),"inputAlightCountsFile", "alighting");
	}

	private void filterStations(Config config, String paramName, String type){
		File countFile = new File (config.findParam("ptCounts", paramName));
		
		if (countFile.exists()) {
			Counts counts = new Counts();
			MatsimCountsReader matsimCountsReader = new MatsimCountsReader(counts);
			matsimCountsReader.readFile(countFile.getPath());

			List<Id> removableStationsList = new ArrayList<Id>(); //
			for(Map.Entry <Id,Count> entry: counts.getCounts().entrySet()){
				if (entry.getValue().getMaxVolume().getValue() < this.maxVolumeValue){
					removableStationsList.add(entry.getKey());
				}
			}
			for (Id id : removableStationsList){
				counts.getCounts().remove(id);	
			}
			
			CountsWriter countsWriter = new CountsWriter(counts);
			String filteredCountsFile = config.controler().getOutputDirectory() + "/" + this.maxVolumeValue +  countFile.getName();
			countsWriter.write(filteredCountsFile);
			System.out.println("Filtered counts written in:" + filteredCountsFile);
		}else {
			log.warn(type + " counts not found");
		}
	}
	
	public static void main(String[] args) {
		String configFile = "../playgrounds/mmoyo/output/20/config_20plans_routed.xml";
		new StationFilter(new TransScenarioLoader().loadScenario(configFile),50).getStopsWithMinCount();
	}

}