package playground.christoph.knowledge.nodeselection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.MatsimNetworkReader;
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
	
	private Network network;
	private double time = Time.UNDEFINED_TIME;
	
	// CostCalculator for the Dijkstra Algorithm
	private TravelCost costCalculator = new FreespeedTravelTimeCost(new PlanCalcScoreConfigGroup());
	private DijkstraForSelectNodes dijkstra;
	
	private Map<Id, Link> notUsedLinks;

	private int numOfThreads = 2;
	
	public static void main(String[] args)
	{
		Scenario scenario = new ScenarioImpl();
		
		// load Network
		//String networkFile = "mysimulations/kt-zurich/input/network.xml";
		String networkFile = "mysimulations/kt-zurich-cut/network.xml";
		Network nw = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFile);
		
		log.info("Network has " + nw.getLinks().size() + " Links.");
		log.info("Network has " + nw.getNodes().size() + " Nodes.");
		
		NetworkThiningForDijkstra ntfd = new NetworkThiningForDijkstra();
		ntfd.setNetwork(nw);
		
		ntfd.findLinksMultiThread();
		//ntfd.findLinks();	
	}
	
	public void setNetwork(Network network)
	{
		this.network = network;
		dijkstra = new DijkstraForSelectNodes(network);
	}
	
	public Network getNetwork()
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

	public void findLinksMultiThread()
	{
		ThinningThread[] thinningThreads = new ThinningThread[numOfThreads];
		
		// split up the Nodes to distribute the workload between the threads
		List<List<Node>> nodeLists = new ArrayList<List<Node>>();
		for (int i = 0; i < numOfThreads; i++)
		{
			nodeLists.add(new ArrayList<Node>());
		}
		
		int i = 0;
		for (Node node : this.network.getNodes().values())
		{
			nodeLists.get(i % numOfThreads).add(node);
			i++;
		}
		
		// init the Threads
		for (int j = 0; j < thinningThreads.length; j++)
		{
			thinningThreads[j] = new ThinningThread(network, nodeLists.get(j));
			thinningThreads[j].setName("ThinningThread#" + i);
		}
		
		// start the Threads
		for (ThinningThread thinningThread : thinningThreads)
		{
			thinningThread.start();
		}
		
		// wait until the Thread are finished
		try {
			for (ThinningThread thinningThread : thinningThreads) 
			{
				thinningThread.join();
			}
		} 
		catch (InterruptedException e)
		{
			log.error(e.getMessage());
		}
		
		// get not used Links from the Threads
		notUsedLinks = new HashMap<Id, Link>();
		for (ThinningThread thinningThread : thinningThreads) 
		{
			notUsedLinks.putAll(thinningThread.getNotUsedLinks());
		}
		
		log.info("Network has " + network.getLinks().size() + " Links.");
		log.info("Not used links: " + notUsedLinks.size());
	}
	
	public void findLinks()
	{
		notUsedLinks = new HashMap<Id, Link>();
		
		int nodeCount = 0;
		
		// for every Node of the Network
		for (Node node : network.getNodes().values())
		{
			List<Node> outNodes = new ArrayList<Node>();
			
			for (Link outLink : node.getOutLinks().values())
			{
				outNodes.add(outLink.getToNode());
			}
			
			dijkstra.executeRoute(node, outNodes);
			
			Map<Node, Double> travelCostsMap = dijkstra.getMinDistances();
			
			for (Link outLink : node.getOutLinks().values())
			{
				Node outNode = outLink.getToNode();
				
				// calculate costs for OutLink
				double linkCosts = costCalculator.getLinkGeneralizedTravelCost(outLink, time);
				
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
					
//			// for all OutLinks
//			for (LinkImpl outLink : node.getOutLinks().values())
//			{
//				NodeImpl outNode = outLink.getToNode();
//				
//				// calculate costs for OutLink
//				double linkCosts = costCalculator.getLinkTravelCost(outLink, time);
//				
//				// calculate costs to ToNode
//				dijkstra.executeRoute(node, outNode);
//				Map<NodeImpl, Double> travelCostsMap = dijkstra.getMinDistances();
//				double minTravelCosts = travelCostsMap.get(outNode);
//				
//				/*
//				 * Is there a cheaper Route to the ToNode than
//				 * via the OutLink? If yes, we don't need the
//				 * OutLink. 
//				 */
//				if (linkCosts > minTravelCosts)
//				{
//					notUsedLinks.put(outLink.getId(), outLink);
//				}
//			}
			
			nodeCount++;
			if (nodeCount % 1000 == 0)
			{
				log.info("NodeCount: " + nodeCount + ", not used Links: " + notUsedLinks.size());
			}
		}
		
		log.info("Network has " + network.getLinks().size() + " Links.");
		log.info("Not used links: " + notUsedLinks.size());
	}
	
	/**
	 * The thread class that really handles the persons.
	 */
	private static class ThinningThread extends Thread
	{
		
		// CostCalculator for the Dijkstra Algorithm
		private TravelCost costCalculator = new FreespeedTravelTimeCost(new PlanCalcScoreConfigGroup());
		private DijkstraForSelectNodes dijkstra;
		
		private Map<Id, Link> notUsedLinks;
		private List<Node> nodes;
		
		private double time = Time.UNDEFINED_TIME;
		private int thread;
		
		private static int threadCounter = 0;
		
		public ThinningThread(Network network, List<Node> nodes)
		{
			this.nodes = nodes;
			this.thread = threadCounter++;
			
			this.dijkstra = new DijkstraForSelectNodes(network);
		}

		@Override
		public void run()
		{
			findLinks();	
		}
		
		public Map<Id, Link> getNotUsedLinks()
		{
			return this.notUsedLinks;
		}
		
		private void findLinks()
		{
			notUsedLinks = new HashMap<Id, Link>();
			
			int nodeCount = 0;
			
			// for every Node of the given List
			for (Node node : nodes)
			{
				List<Node> outNodes = new ArrayList<Node>();
				
				for (Link outLink : node.getOutLinks().values())
				{
					outNodes.add(outLink.getToNode());
				}
				
				dijkstra.executeRoute(node, outNodes);
				
				Map<Node, Double> travelCostsMap = dijkstra.getMinDistances();
				
				for (Link outLink : node.getOutLinks().values())
				{
					Node outNode = outLink.getToNode();
					
					// calculate costs for OutLink
					double linkCosts = costCalculator.getLinkGeneralizedTravelCost(outLink, time);
					
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
				
				nodeCount++;
				if (nodeCount % 1000 == 0)
				{
					log.info("Thread: " + thread + ", NodeCount: " + nodeCount + ", not used Links: " + notUsedLinks.size());
				}
			}
		}
	}
}
