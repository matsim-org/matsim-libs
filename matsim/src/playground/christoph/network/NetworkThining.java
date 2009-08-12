package playground.christoph.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.misc.Time;

/*
 * Find links that are not used in an empty Network because they
 * are too expensive. Removing them should speed up creating the
 * List of known Nodes of a Person. The Knowledge should not be
 * influenced by this because it contains only Nodes and not Links.
 */

public class NetworkThining {

	private static final Logger log = Logger.getLogger(NetworkThining.class);
	
	private NetworkLayer network;
		
	private Map<Id, NodeImpl> nodesToTransform;

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
		
		NetworkThining ntfd = new NetworkThining();
		ntfd.setNetwork(nw);
		
		//ntfd.findNodesMultiThread();
		ntfd.findNodes();
	}
	
	public void setNetwork(NetworkLayer network)
	{
		this.network = network;
	}
	
	public NetworkLayer getNetwork()
	{
		return this.network;
	}
	
	public void findNodesMultiThread()
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
		
		// get Nodes that can be transformed from the Threads
		nodesToTransform = new HashMap<Id, NodeImpl>();
		for (ThinningThread thinningThread : thinningThreads) 
		{
			nodesToTransform.putAll(thinningThread.getTransformableNodes());
		}
		
		log.info("Network has " + network.getLinks().size() + " Links.");
		log.info("Not used links: " + nodesToTransform.size());
	}
	
	public void findNodes()
	{
		nodesToTransform = new HashMap<Id, NodeImpl>();
		
		int nodeCount = 0;
		
		// for every Node of the Network
		for (NodeImpl node : network.getNodes().values())
		{
			nodeCount++;
			if (nodeCount % 1000 == 0)
			{
				log.info("NodeCount: " + nodeCount + ", transformable Nodes: " + nodesToTransform.size());
			}
			
			Map<Id, NodeImpl> inNodes = new TreeMap<Id, NodeImpl>(node.getInNodes());
			Map<Id, NodeImpl> outNodes = new TreeMap<Id, NodeImpl>(node.getOutNodes());
			
			if (inNodes.size() != outNodes.size()) continue;
			if (inNodes.size() != 3) continue;

			boolean transformable = true;
			for (NodeImpl inNode : inNodes.values())
			{
				if (!outNodes.containsKey(inNode.getId()))
				{
					transformable = false;
					break;
				}
			}
			
			// check whether the point is outside the triangle defined by the other three nodes
			NodeImpl[] angleNodes = (NodeImpl[]) inNodes.values().toArray(new NodeImpl[3]);
			int inTriangle = calcInTriangle(angleNodes[0], angleNodes[1], angleNodes[2], node);
			if (inTriangle == -1) transformable = false;

			if (subNodesConnected(angleNodes[0], angleNodes[1], angleNodes[2])) transformable = false;
			
			// check Angles			
//			double a1 = calcAngle(node, angleNodes[0], angleNodes[1]);
//			double a2 = calcAngle(node, angleNodes[0], angleNodes[2]);
//			double a3 = calcAngle(node, angleNodes[1], angleNodes[2]);
//						
//			double angleSum = a1 + a2 + a3;
//			if (angleSum < 2 * Math.PI)
//			{
//				// Sum of angles is < 360° - maybe a rounding error? 
//				
//				// Rounding errors won't be bigger than 1°
//				double factor = 179 * Math.PI / 180;
//			
//				if (angleSum * 1.01 > 2 * Math.PI && (a1 < factor && a2 < factor && a3 < factor))
//				{
//					
//				}
//				else continue;
//			}
			
			if (transformable)
			{
				nodesToTransform.put(node.getId(), node);
			}
		}
		
		log.info("Transformable Nodes: " + nodesToTransform.size());
	}
	
//	/*
//	 * Calculates the Angle between the vectors ab and ac.
//	 */
//	private double calcAngle(NodeImpl a, NodeImpl b, NodeImpl c)
//	{
//		double ABx = b.getCoord().getX() - a.getCoord().getX();
//		double ABy = b.getCoord().getY() - a.getCoord().getY();	
//
//		double ACx = c.getCoord().getX() - a.getCoord().getX();
//		double ACy = c.getCoord().getY() - a.getCoord().getY();
//		
//		double cosAlpha = (ABx * ACx + ABy * ACy) / (Math.sqrt(ABx*ABx + ABy*ABy) * Math.sqrt(ACx*ACx + ACy*ACy));
//		
//		return Math.acos(cosAlpha);
//	}
	
	private boolean subNodesConnected(NodeImpl a, NodeImpl b, NodeImpl c)
	{
		for(NodeImpl node : a.getOutNodes().values())
		{
			if (node.equals(b) || node.equals(c)) return true;
		}

		for(NodeImpl node : b.getOutNodes().values())
		{
			if (node.equals(a) || node.equals(c)) return true;
		}
		
		for(NodeImpl node : c.getOutNodes().values())
		{
			if (node.equals(a) || node.equals(b)) return true;
		}
		
		return false;
	}
	
	/*
	 * returns 1 if the point is inside the triangle
	 * returns 0 if the point is on one border of the triangle
	 * returns -1 if the point is outside the triangle
	 */
	private int calcInTriangle(NodeImpl a, NodeImpl b, NodeImpl c, NodeImpl point)
	{
		double det1 = calcDeterminant(a, b, point);
		double det2 = calcDeterminant(b, c, point);
		double det3 = calcDeterminant(c, a, point);
		
		if (det1 == 0.0 || det2 == 0.0 || det3 == 0.0) return 0;
		
		if (det1 > 0 && det2 > 0 && det3 > 0) return 1;
		if (det1 < 0 && det2 < 0 && det3 < 0) return 1;
		
		return -1;
	}
	/*
	 * used by calcInTriangle(...)
	 */
	private double calcDeterminant(NodeImpl start, NodeImpl end, NodeImpl point)
	{
		double[] row1 = new double[]{start.getCoord().getX(), start.getCoord().getY(), 1};
		double[] row2 = new double[]{end.getCoord().getX(),   end.getCoord().getY(),   1};
		double[] row3 = new double[]{point.getCoord().getX(), point.getCoord().getY(), 1};
		
		double det = row1[0]*row2[1]*row3[2] + row1[1]*row2[2]*row3[0] + row1[2]*row2[0]*row3[1] -
					 row1[2]*row2[1]*row3[0] - row1[1]*row2[0]*row3[2] - row1[0]*row2[2]*row3[1];
		                                                                                      
		return det;
	}
	
	/**
	 * The thread class that really handles the persons.
	 */
	private static class ThinningThread extends Thread
	{
		private NetworkLayer network;
		
		private Map<Id, NodeImpl> nodesToTransform;
		private List<NodeImpl> nodes;
		
		private double time = Time.UNDEFINED_TIME;
		private int thread;
		
		private static int threadCounter = 0;
		
		public ThinningThread(NetworkLayer network, List<NodeImpl> nodes)
		{
			this.network = network;
			this.nodes = nodes;
			this.thread = threadCounter++;
		}

		@Override
		public void run()
		{
			findNodes();	
		}
		
		public Map<Id, NodeImpl> getTransformableNodes()
		{
			return this.nodesToTransform;
		}
		
		private void findNodes()
		{
			nodesToTransform = new HashMap<Id, NodeImpl>();
			
			int nodeCount = 0;
			
			// for every Node of the given List
			for (NodeImpl node : nodes)
			{
				List<NodeImpl> outNodes = new ArrayList<NodeImpl>();
				
				for (LinkImpl outLink : node.getOutLinks().values())
				{
					outNodes.add(outLink.getToNode());
				}
								
				nodeCount++;
				if (nodeCount % 1000 == 0)
				{
					log.info("Thread: " + thread + ", NodeCount: " + nodeCount + ", not used Links: " + nodesToTransform.size());
				}
			}
		}
	}
}
