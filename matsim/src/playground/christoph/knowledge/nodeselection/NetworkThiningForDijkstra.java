package playground.christoph.knowledge.nodeselection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

	private int numOfThreads = 2;
	
	public static void main(String[] args)
	{
		// create Config
		Config config = new Config();
		config.addCoreModules();
		config.checkConsistency();
		Gbl.setConfig(config);
		
		// load Network
		//String networkFile = "mysimulations/kt-zurich/input/network.xml";
		String networkFile = "mysimulations/kt-zurich-cut/network.xml";
		NetworkLayer nw = new NetworkLayer();
		new MatsimNetworkReader(nw).readFile(networkFile);
		
		log.info("Network has " + nw.getLinks().size() + " Links.");
		log.info("Network has " + nw.getNodes().size() + " Nodes.");
		
		NetworkThiningForDijkstra ntfd = new NetworkThiningForDijkstra();
		ntfd.setNetwork(nw);
		
		ntfd.findLinksMultiThread();
		//ntfd.findLinks();	
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

	public void findLinksMultiThread()
	{
		ThinningThread[] thinningThreads = new ThinningThread[numOfThreads];
		
		// split up the Nodes to distribute the workload between the threads
		List<List<NodeImpl>> nodeLists = new ArrayList<List<NodeImpl>>();
		for (int i = 0; i < numOfThreads; i++)
		{
			nodeLists.add(new ArrayList<NodeImpl>());
		}
		
		int i = 0;
		for (NodeImpl node : this.network.getNodes().values())
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
		notUsedLinks = new HashMap<Id, LinkImpl>();
		for (ThinningThread thinningThread : thinningThreads) 
		{
			notUsedLinks.putAll(thinningThread.getNotUsedLinks());
		}
		
		log.info("Network has " + network.getLinks().size() + " Links.");
		log.info("Not used links: " + notUsedLinks.size());
	}
	
	public void findLinks()
	{
		notUsedLinks = new HashMap<Id, LinkImpl>();
		
		int nodeCount = 0;
		
		// for every Node of the Network
		for (NodeImpl node : network.getNodes().values())
		{
			List<NodeImpl> outNodes = new ArrayList<NodeImpl>();
			
			for (LinkImpl outLink : node.getOutLinks().values())
			{
				outNodes.add(outLink.getToNode());
			}
			
			dijkstra.executeRoute(node, outNodes);
			
			Map<NodeImpl, Double> travelCostsMap = dijkstra.getMinDistances();
			
			for (LinkImpl outLink : node.getOutLinks().values())
			{
				NodeImpl outNode = outLink.getToNode();
				
				// calculate costs for OutLink
				double linkCosts = costCalculator.getLinkTravelCost(outLink, time);
				
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
		private NetworkLayer network;
		
		// CostCalculator for the Dijkstra Algorithm
		private TravelCost costCalculator = new FreespeedTravelTimeCost();
		private DijkstraForSelectNodes dijkstra;
		
		private Map<Id, LinkImpl> notUsedLinks;
		private List<NodeImpl> nodes;
		
		private double time = Time.UNDEFINED_TIME;
		private int thread;
		
		private static int threadCounter = 0;
		
		public ThinningThread(NetworkLayer network, List<NodeImpl> nodes)
		{
			this.network = network;
			this.nodes = nodes;
			this.thread = threadCounter++;
			
			this.dijkstra = new DijkstraForSelectNodes(network);
		}

		@Override
		public void run()
		{
			findLinks();	
		}
		
		public Map<Id, LinkImpl> getNotUsedLinks()
		{
			return this.notUsedLinks;
		}
		
		private void findLinks()
		{
			notUsedLinks = new HashMap<Id, LinkImpl>();
			
			int nodeCount = 0;
			
			// for every Node of the given List
			for (NodeImpl node : nodes)
			{
				List<NodeImpl> outNodes = new ArrayList<NodeImpl>();
				
				for (LinkImpl outLink : node.getOutLinks().values())
				{
					outNodes.add(outLink.getToNode());
				}
				
				dijkstra.executeRoute(node, outNodes);
				
				Map<NodeImpl, Double> travelCostsMap = dijkstra.getMinDistances();
				
				for (LinkImpl outLink : node.getOutLinks().values())
				{
					NodeImpl outNode = outLink.getToNode();
					
					// calculate costs for OutLink
					double linkCosts = costCalculator.getLinkTravelCost(outLink, time);
					
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
