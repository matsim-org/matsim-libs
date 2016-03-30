package playground.singapore.springcalibration.preprocess;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Volume;

public class CountsCleaner {
	
	private static final Logger log = Logger.getLogger(CountsCleaner.class);
	
	private Scenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
	public static void main(String[] args) {
		CountsCleaner cleaner = new CountsCleaner();
		cleaner.run(args[0], args[1], args[2]);
	}
	
	
	public void run(String countsfile, String networkfile, String countsFileOut) {
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkfile);
		Counts<Link> counts = new Counts<>();
		new MatsimCountsReader(counts).readFile(countsfile);
		
		
		Network network = scenario.getNetwork();
		
		for (Link link : network.getLinks().values()) {
			if (link.getId().toString().contains("_HW")) {
				String originalkey = link.getId().toString().replace("_HW", "");
				Link originallink = network.getLinks().get(Id.createLinkId(originalkey));
				
				double capacity = link.getCapacity();
				double originalcapacity = originallink.getCapacity();
				double totalcapacity = capacity + originalcapacity;
				
				Count<Link> originalcount = counts.getCount(originallink.getId());
				
				// check if there is a count station
				if (originalcount != null) {
					log.info("HW link:       " + link.getId().toString() + " capacity: " + capacity);
					log.info("original link: " + originallink.getId().toString() + " capacity: " + originalcapacity);
					HashMap<Integer,Volume> volumes = originalcount.getVolumes();
					this.createHWCount(link.getId(), originalcount.getCsId(), volumes, counts, capacity / totalcapacity);
					this.adaptOriginalCount(originallink.getId(), originalcount.getCsId(), volumes, counts, originalcapacity / totalcapacity);
				}
				
				
			}
		}
		log.info("Writing counts file to " + countsFileOut);
		new CountsWriter(counts).write(countsFileOut);		
	}
		
	private void createHWCount(Id<Link> linkid, String csName, HashMap<Integer,Volume> volumes, Counts<Link> counts, double factor) {
		Count<Link> count = counts.createAndAddCount(linkid, csName);
		for (Integer h : volumes.keySet()) {
			double val = factor * volumes.get(h).getValue();
			count.createVolume(h, val);
		}		
	}
	
	private void adaptOriginalCount(Id<Link> linkid, String csName, HashMap<Integer,Volume> volumes, Counts<Link> counts, double factor) {
		Count<Link> count = counts.getCounts().get(linkid);
		for (Integer h : volumes.keySet()) {
			double val = factor * volumes.get(h).getValue();
			count.createVolume(h, val);
		}
	}

}
