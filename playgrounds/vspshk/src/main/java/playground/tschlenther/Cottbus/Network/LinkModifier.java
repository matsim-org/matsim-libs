/**
 * 
 */
package playground.tschlenther.Cottbus.Network;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.demandde.counts.TSBASt2Count;

/**
 * @author Tille
 *
 */
public class LinkModifier {

	private static String NETWORK = "C:/Users/Tille/WORK/Cottbus/Cottbus-pt/Demand_input/network_pt_modified_removed.xml";
	private static String NETOUTPUT = "C:/Users/Tille/WORK/Cottbus/Cottbus-pt/Demand_input/network_pt_cap60.xml";
	private Scenario scenario;
	private Network net;
	private static final Logger logger = Logger.getLogger(LinkModifier.class);

	/**
	 * 
	 */
	public LinkModifier(Scenario scenario) {
		this.scenario = scenario;
		this.net = scenario.getNetwork();
	}
	
	public void modifyLinkCapacity(Id<Link> linkId, double newCap){
		Link link = net.getLinks().get(linkId);
		if (link == null){
			logger.error("coulndt find link " + linkId + " in the network");
			return;
		}
		logger.info("set capacity of link " + linkId + " to " + newCap);
		link.setCapacity(newCap);
	}
	
	public void writeNetwork(String path){
		logger.info("write network file to " + path);
		NetworkWriter writer = new NetworkWriter(net);
		writer.write(path);
	}
	
	public void modifyToNode(Id<Link> linkId, Id<Node> newToNode){
		Link link = net.getLinks().get(linkId);
		if (link == null){
			logger.error("coulndt find link " + linkId + " in the network");
			return;
		}
		logger.info("set toNode of link " + linkId + " to " + newToNode);
		Node toNode = net.getNodes().get(newToNode);
		if (toNode == null){
			logger.error("coulndt find node " + toNode + " in the network");
			return;
		}
		link.setToNode(toNode);
	}
	
	public void modifyFromNode(Id<Link> linkId, Id<Node> newFromNode){
		Link link = net.getLinks().get(linkId);
		if (link == null){
			logger.error("coulndt find link " + linkId + " in the network");
			return;
		}
		logger.info("set fromNode of link " + linkId + " to " + newFromNode);
		Node fromNode = net.getNodes().get(newFromNode);
		if (fromNode == null){
			logger.error("coulndt find node " + fromNode + " in the network");
			return;
		}
		link.setFromNode(fromNode);
	}
	
	public Network getNetwork(){
		return this.net;
	}
	
	public static void main(String[] args){
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(NETWORK);
		Scenario scen = ScenarioUtils.loadScenario(config);
		LinkModifier mod = new LinkModifier(scen);
		
		for(Id<Link> id : mod.getNetwork().getLinks().keySet()){
			if(id.toString().startsWith("pt")){
				mod.modifyLinkCapacity(id, 60);
			}
		}
		
		NetworkWriter writer = new NetworkWriter(scen.getNetwork());
		writer.write(NETOUTPUT);
	}
	
	
}
