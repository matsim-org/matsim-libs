package playground.mmoyo.analysis.tools;

import java.io.File;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;

import playground.mmoyo.utils.TransScenarioLoader;

/**assigns the counts of a transitStop to the respective link*/
public class StopFacility2LinkConverter {
	private static final Logger log = Logger.getLogger(StopFacility2LinkConverter.class);
	private ScenarioImpl scenario;
	private Counts counts;
	private String countFileName;
	
	public StopFacility2LinkConverter(ScenarioImpl scenario){
		this.scenario = scenario;
		init();
	}

	private void init(){
		File countFile = new File (scenario.getConfig().findParam("ptCounts", "inputOccupancyCountsFile"));
		if (countFile.exists()) {
			this.countFileName = countFile.getName();
			this.counts = new Counts();
			MatsimCountsReader matsimCountsReader = new MatsimCountsReader(counts);
			matsimCountsReader.readFile(countFile.getPath());
		}else {
			log.warn(" counts file not found" + countFile.getPath());
		}
	}
	
	/**creates a new counts file with links id's instead of stopfacilities Id's */
	private void write(){
			Counts newCounts = new Counts();

			newCounts.setDescription(this.counts.getDescription());
			newCounts.setLayer(this.counts.getLayer());
			newCounts.setName(this.counts.getName());
			newCounts.setYear(this.counts.getYear());
			
			for(Entry<Id, Count> entry: this.counts.getCounts().entrySet()){
				Id key = entry.getKey();
				Count count = entry.getValue();
				
				Id linkId = this.scenario.getTransitSchedule().getFacilities().get(key).getLinkId();
				System.out.println(linkId.toString());
				//count.setLocId(linkId);  <--because of this does not work!
				newCounts.getCounts().put(linkId, count);
			}
			
			CountsWriter countsWriter = new CountsWriter(newCounts);
			String filteredCountsFile = scenario.getConfig().controler().getOutputDirectory() + "/linkBased_"  +  this.countFileName;
			countsWriter.write(filteredCountsFile);
			System.out.println("Filtered counts written in:" + filteredCountsFile);
	}
	
	/**shows in console the relation stopFacility-link*/
	private void println(){
		for(Id stopFacilityId : this.counts.getCounts().keySet()){		
			Id linkId = this.scenario.getTransitSchedule().getFacilities().get(stopFacilityId).getLinkId();
			System.out.println(stopFacilityId + " == "+ linkId.toString());
		}
	}
	
	public static void main(String[] args) {
		String configFile = "../playgrounds/mmoyo/output/best/1/config.xml";
		StopFacility2LinkConverter stopFacility2LinkConverter = new StopFacility2LinkConverter(new TransScenarioLoader().loadScenario(configFile));
		//stopFacility2LinkConverter.write();
		stopFacility2LinkConverter.println();
	}

}