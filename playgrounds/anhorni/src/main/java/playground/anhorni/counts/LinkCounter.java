package playground.anhorni.counts;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordImpl;

public class LinkCounter {
	
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	Counts counts;
	private double radius;
	private String nodeId;
	
	private final static Logger log = Logger.getLogger(LinkCounter.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LinkCounter counter = new LinkCounter();
		if (args.length == 2 ) {
			counter.init(args[0], args[1], -1.0, "");
			counter.count();
			
		}
		else if (args.length == 4 ) {
			counter.init(args[0], args[1], Double.parseDouble(args[2]), args[3]);
			counter.count();
		}
		else {
			log.info("please provide the correct arguments!");
		}
		
	}
	
	
	public void init(String networkFile, String countsFile, double radius, String nodeId) {
		this.radius = radius;
		this.nodeId = nodeId;
		
		log.info("read netork ...");
		new MatsimNetworkReader(scenario).readFile(networkFile);
		
		log.info("read counts ..."); 
		this.counts = new Counts();
		MatsimCountsReader countsReader = new MatsimCountsReader(counts);
		countsReader.readFile(countsFile);
	}
	
	public void count() {
		int astraCounter = 0;
		int otherCounter = 0;
		int counter = 0;
		int nextMsg = 1;
		
		for (Id countId : this.counts.getCounts().keySet()) {
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" count # " + counter);
			}
			Count count = this.counts.getCounts().get(countId);
			if (checkInside(count)) {
				if (count.getCsId().startsWith("ASTRA")) astraCounter++;
				else otherCounter++;
			}			
		}
		log.info("ASTRA links: " + astraCounter);
		log.info("other links: " + otherCounter);
	}
	
	private boolean checkInside(Count count) {
		if (this.radius < 0.0) return true;
		else {	
			Node centerNode = this.scenario.getNetwork().getNodes().get(new IdImpl(this.nodeId));
			Link link = this.scenario.getNetwork().getLinks().get(count.getLocId());
			
			if (link == null) {
				log.info("Link not found " + count.getLocId().toString() + " station " + count.getCsId().toString());
				return false;
			}
			Coord coordLink = link.getCoord();
			
			if (((CoordImpl)centerNode.getCoord()).calcDistance(coordLink) < this.radius) return true;
			else return false;
		}
	}	
}
