/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialNetworkGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.johannes.socialnets;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.population.Route;
import org.matsim.router.Dijkstra;

import playground.johannes.graph.GraphStatistics;
import playground.johannes.graph.SparseEdge;
import playground.johannes.graph.SparseGraph;
import playground.johannes.graph.SparseVertex;

/**
 * @author illenberger
 *
 */
public class SpatialNetworkGenerator {
	
	private final static Logger logger = Logger.getLogger(SpatialNetworkGenerator.class);

	private final static double y0 = 0.000017;
	
	private final static double A = 0.0008;
	
	private final static double alpha = -0.00124;
	
	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		Config config = Gbl.createConfig(args);
		logger.info("Loading network and plans...");
		ScenarioData data = new ScenarioData(config);
		final NetworkLayer network = data.getNetwork();
		final Population population = data.getPopulation();
		
		logger.info("Creating egos...");
		SocialNetwork socialNet = new SocialNetwork();
		ConcurrentLinkedQueue<Ego> egos = new ConcurrentLinkedQueue<Ego>();
		for(Person person : population) {
			egos.add(socialNet.addEgo(person));
		}
		

		logger.info("Initializing threads...");
		double scale = Double.parseDouble(args[1]);
		InternalThread.totalEgos = egos.size();
		int count = Runtime.getRuntime().availableProcessors();
		InternalThread[] threads = new InternalThread[count]; 
		for(int i = 0; i < count; i++) {
			threads[i] = new InternalThread(network, socialNet, egos, scale, i);
		}
		for(Thread thread : threads) {
			thread.start();
		}
		for(Thread thread : threads) {
			thread.join();
		}
		
		int numEdges = socialNet.getEdges().size();
		int numVertices = socialNet.getVertices().size();
		logger.info(String.format("Loaded graph: %1$s vertices, %2$s edges.", numVertices, numEdges));

		double meanDegree = GraphStatistics.getDegreeStatistics(socialNet).getMean();
		logger.info(String.format("Mean degree is %1$s.", meanDegree));
		
		double clustering = GraphStatistics.getClusteringStatistics(socialNet).getMean();
		logger.info(String.format("Mean clustering coefficient is %1$s.", clustering));
		
		double mutuality = GraphStatistics.getMutuality(socialNet);
		logger.info(String.format("Mutuality is %1$s.", mutuality));
		
		double dcorrelation = GraphStatistics.getDegreeCorrelation(socialNet);
		logger.info(String.format("Degree correlation is %1$s.", dcorrelation));
	}
	
	private static final class InternalThread extends Thread {
		
		private static int totalEgos;
		
		private static int processedEgos;
		
		private final NetworkLayer network;
		
		private final SocialNetwork socialNet;
		
		private final ConcurrentLinkedQueue<Ego> egos;
		
		private final double scale;
		
		private final Random random;
		
		private static SparseGraph<Integer> ttGraph = new SparseGraph<Integer>();
		
		private static Map<Node, SparseVertex> nodeMap = new HashMap<Node, SparseVertex>(); 
		
		public static final ReentrantLock lock = new ReentrantLock();
		
		public InternalThread(NetworkLayer network, SocialNetwork socialNet, ConcurrentLinkedQueue<Ego> egos, double scale, long seed) {
			this.network = network;
			this.socialNet = socialNet;
			this.egos = egos;
			this.scale = scale;
			random = new Random(seed);
		}
		
		public void run() {
			FreespeedTravelTime freett = new FreespeedTravelTime();
			Dijkstra router = new Dijkstra(network, freett, freett);
			Ego e;
			while((e = egos.poll()) != null) {
				for(Ego alter : egos) {
					Link egoHome = e.getPerson().getSelectedPlan().getFirstActivity().getLink();
					Link alterHome = alter.getPerson().getSelectedPlan().getFirstActivity().getLink();
					Node from = egoHome.getToNode();
					Node to = alterHome.getFromNode();
					SparseVertex fromVertex = nodeMap.get(from);
					SparseVertex toVertex = nodeMap.get(to);
					Integer tt = null;
					if(fromVertex != null && toVertex != null) {
						while(lock.isLocked()) {
						}
						lock.lock();
						for(SparseEdge<Integer> edge : fromVertex.getEdges()) {
							if(edge.getOpposite(fromVertex) == toVertex)
								tt = edge.getAttribute();
						}
						lock.unlock();
						
					}
					if(tt == null) {
						if(fromVertex == null) {
							fromVertex = ttGraph.addVertex();
							nodeMap.put(from, fromVertex);
						}
						if(toVertex == null) {
							toVertex = ttGraph.addVertex();
							nodeMap.put(to, toVertex);
						}
						Route route = router.calcLeastCostPath(from, to, 0);
						
						while(lock.isLocked()) {
						}
						lock.lock();
						SparseEdge<Integer> ttEdge = ttGraph.addEdge(fromVertex, toVertex);
						ttEdge.setAttribute((int) route.getTravTime());
						lock.unlock();
						
						tt = (int)route.getTravTime();
					}
					
					double p = y0 + A * scale * Math.exp(alpha * tt);
					if(random.nextDouble() <= p) {
						synchronized(this) {
							if(!e.getNeighbours().contains(alter)) {
								socialNet.addEdge(e, alter);
							}
						}
					}
				}
				processedEgos++;
//				if(processedEgos % 100 == 0)
					logger.info(String.format("Processed %1$s of %2$s egos (%3$s).", processedEgos, totalEgos, processedEgos/(double)totalEgos));
			}
		}
	}
}
