package playground.christoph.knowledge.nodeselection;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.utils.misc.Time;

/*
 * Find links that are not used in an empty Network because they
 * are too expensive. Removing them should speed up creating the
 * List of known Nodes of a Person. The Knowledge should not be
 * influenced by this because it contains only Nodes and not Links.
 */

public class NetworkThiningForDijkstra {

	private static final Logger log = Logger.getLogger(NetworkThiningForDijkstra.class);
	
	private NetworkLayer network;
	private double time = Time.UNDEFINED_TIME;
	
	// CostCalculator for the Dijkstra Algorithm
	private TravelCost costCalculator = new FreespeedTravelTimeCost();
	private DijkstraForSelectNodes dijkstra;
	
	private Map<Id, LinkImpl> notUsedLinks;

	public static void main(String[] args)
	{
		// create Config
		Config config = new Config();
		config.addCoreModules();
		config.checkConsistency();
		Gbl.setConfig(config);
		
		// load Network
		String networkFile = "mysimulations/kt-zurich/input/network.xml";
		//String networkFile = "mysimulations/kt-zurich-cut/network.xml";
		NetworkLayer nw = new NetworkLayer();
		new MatsimNetworkReader(nw).readFile(networkFile);
		
		log.info("Network has " + nw.getLinks().size() + " Links.");
		log.info("Network has " + nw.getNodes().size() + " Nodes.");
		
		NetworkThiningForDijkstra ntfd = new NetworkThiningForDijkstra();
		ntfd.setNetwork(nw);
		ntfd.findLinks();		
	}
	
	public void setNetwork(NetworkLayer network)
	{
		this.network = network;
		dijkstra = new DijkstraForSelectNodes(network);
	}
	
	public NetworkLayer getNetwork()
	{
		return this.network;
	}
	
	public void setCostCalculator(TravelCost costCalculator)
	{
		this.costCalculator = costCalculator;
	}
	
	public TravelCost getCostCalculator()
	{
		return this.costCalculator;
	}
	
	public void findLinks()
	{
		notUsedLinks = new HashMap<Id, LinkImpl>();
		
		// for every Node of the Network
		for (NodeImpl node : network.getNodes().values())
		{
			// for all OutLinks
			for (LinkImpl outLink : node.getOutLinks().values())
			{
				NodeImpl outNode = outLink.getToNode();
				
//				SelectNodesDijkstra snd = new SelectNodesDijkstra(network, network.getNode("545"), network.getNode("4058"), 1.0);
				// calculate costs for OutLink
				double linkCosts = costCalculator.getLinkTravelCost(outLink, time);
				
				// calculate costs to ToNode
				dijkstra.executeRoute(node, outNode);
				Map<NodeImpl, Double> travelCostsMap = dijkstra.getMinDistances();
				double minTravelCosts = travelCostsMap.get(outNode);
				
				/*
				 * Is there a cheaper Route to the ToNode than
				 * via the OutLink? If yes, we don't need the
				 * OutLink. 
				 */
				if (linkCosts > minTravelCosts)
				{
					notUsedLinks.put(outLink.getId(), outLink);
				}
			}							
		}
		
		log.info("Network has " + network.getLinks().size() + " Links.");
		log.info("Not used ones: " + notUsedLinks.size());
	}
}
