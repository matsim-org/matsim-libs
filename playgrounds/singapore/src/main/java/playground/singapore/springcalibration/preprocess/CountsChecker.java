package playground.singapore.springcalibration.preprocess;

import java.text.DecimalFormat;

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
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Volume;

public class CountsChecker {
	
	private static final Logger log = Logger.getLogger(CountsChecker.class);
	private Counts<Link> counts = new Counts<>();
	private DecimalFormat df = new DecimalFormat("0.00");
	
	private Scenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
	public static void main(String[] args) {
		CountsChecker checker = new CountsChecker();
		checker.run(args[0], args[1]);
		log.info("finished ##############################################################");
	}
	
	
	public void run(String countsfile, String networkfile) {
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkfile);
		
		new MatsimCountsReader(counts).readFile(countsfile);
		Network network = scenario.getNetwork();
		
		int linkerrors = 0;
		
		for (Id<Link> linkId : this.counts.getCounts().keySet()) {
			String links = linkId.toString();
			
			Count<Link> count = this.counts.getCounts().get(linkId);
			
			Link link = scenario.getNetwork().getLinks().get(linkId);
			double capacity = link.getCapacity();
			
			Link hwlink = network.getLinks().get(Id.createLinkId(linkId + "_HW"));
			
			if (hwlink != null) {
				capacity += hwlink.getCapacity();
				links = links + ", " + linkId + "_HW";
			}
			
			Volume maxVolume = count.getMaxVolume();
			maxVolume.getValue();

			if (maxVolume.getValue() / capacity < 0.5 || maxVolume.getValue() / capacity > 1.5) {
				linkerrors++;
				log.error("|" + links + "|" + capacity + "|" + maxVolume.getValue() + "|" + df.format(maxVolume.getValue() / capacity)  + "|" + count.getCsId() +"| ========== ERROR");
			} else {
				log.info("|" + links + "|" + capacity + "|" + maxVolume.getValue() + "|" + df.format(maxVolume.getValue() / capacity) + "|" + count.getCsId());
			}
		}
		log.info("Number of errors : " + linkerrors + " of " + this.counts.getCounts().size() + " stations");
	}
	
}
