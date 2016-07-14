package playground.singapore.springcalibration.preprocess;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class NetworkCorrector {
	
	private static final Logger log = Logger.getLogger(NetworkCorrector.class);
	
	private Scenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	
	public static void main(String[] args) {
		NetworkCorrector cleaner = new NetworkCorrector();
		cleaner.run(args[0], args[1]);
	}
		
	public void run(String networkInputfile, String networkOutputfile) {	
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkInputfile);
		
		int adaptedLinks = 0;
		// find all overlayed links and add to both the sum of their previous values
		for (Node origin : scenario.getNetwork().getNodes().values()) {
			for (Node destination : scenario.getNetwork().getNodes().values()) {
				List<Link> overlayedLinks = this.getOverlayedLinks(origin, destination);
				
				if (overlayedLinks.size() > 1) {
					adaptedLinks += this.adaptOverlayedLinks(overlayedLinks);
				}
				
			}
		}
		NetworkWriter writer = new NetworkWriter(scenario.getNetwork());
		writer.write(networkOutputfile);
		log.info("Writting network file to " + networkOutputfile + " " + adaptedLinks + " links adapted");
	}
	
	private int adaptOverlayedLinks(List<Link> overlayedLinks) {
		double sumCapacity = 0.0;
		for (Link link : overlayedLinks) {
			sumCapacity += link.getCapacity();
		}
		
		for (Link link : overlayedLinks) {
			double previousCapacity = link.getCapacity();
			link.setCapacity(sumCapacity);
			log.info("Changed capacity of link " + link.getId().toString() + " from " + previousCapacity + " to " + link.getCapacity());
		}	
		return overlayedLinks.size() - 1;
	}
	
	
	private List<Link> getOverlayedLinks(Node origin, Node destination) {
		List<Link> overlayedLinks = new ArrayList<Link>();
		Map<Id<Link>, ? extends Link> outLinks = origin.getOutLinks();
		Map<Id<Link>, ? extends Link> inLinks = destination.getInLinks();
		
		for (Id<Link> outId : outLinks.keySet()) {
			if (inLinks.containsKey(outId)) {
				Link link = scenario.getNetwork().getLinks().get(outId);
				overlayedLinks.add(link);
			}
		}		
		
		return overlayedLinks;
	}
	
}
