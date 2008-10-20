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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.utils.geometry.Coord;

import playground.johannes.graph.GraphStatistics;
import playground.johannes.graph.UnweightedDijkstra;
import playground.johannes.graph.Vertex;
import playground.johannes.statistics.WeightedStatistics;

/**
 * @author illenberger
 *
 */
public class SpatialNetworkGenerator {
	
	private final static Logger logger = Logger.getLogger(SpatialNetworkGenerator.class);

	private static double alpha1;
	
	private static double alpha2;
	
	private static double gamma;
	
	/**
	 * @param args
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws InterruptedException, FileNotFoundException, IOException {
		Config config = Gbl.createConfig(args);
		
		alpha1 = Double.parseDouble(config.getParam("socialnets", "alpha1"));
		alpha2 = Double.parseDouble(config.getParam("socialnets", "alpha2"));
		gamma = Double.parseDouble(config.getParam("socialnets", "gamma"));
		logger.info(String.format("Parameters are: alpha1=%1$s, alpha2=%2$s, gamma=%3$s", alpha1, alpha2, gamma));
		
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
		double scale = 1;//Double.parseDouble(args[1]);
		InternalThread.totalEgos = egos.size();
		int count = 1;//Runtime.getRuntime().availableProcessors();
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
		logger.info(String.format("%1$s vertices, %2$s edges.", numVertices, numEdges));
		
		int isolates = 0;
		for(Vertex v : socialNet.getVertices())
			if(v.getEdges().size() == 0)
				isolates++;
		logger.info(String.format("%1$s isolates.", isolates));

		DescriptiveStatistics stats = GraphStatistics.getDegreeStatistics(socialNet); 
		double meanDegree = stats.getMean();
		logger.info(String.format("Mean degree is %1$s.", meanDegree));
		WeightedStatistics wstats = new WeightedStatistics();
		wstats.addAll(stats.getValues());
		WeightedStatistics.writeHistogram(wstats.absoluteDistribution(), args[1]);
		
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
		
//		private final NetworkLayer network;
		
		private final SocialNetwork socialNet;
		
		private final ConcurrentLinkedQueue<Ego> egos;
		
//		private final double scale;
		
		private final Random random;
		
//		private static SparseGraph<Integer> ttGraph = new SparseGraph<Integer>();
		
//		private static Map<Node, SparseVertex> nodeMap = new HashMap<Node, SparseVertex>(); 
		
//		public static final ReentrantLock lock = new ReentrantLock();
		
		public InternalThread(NetworkLayer network, SocialNetwork socialNet, ConcurrentLinkedQueue<Ego> egos, double scale, long seed) {
//			this.network = network;
			this.socialNet = socialNet;
			this.egos = egos;
//			this.scale = scale;
			random = new Random(seed);
		}
		
		public void run() {
//			FreespeedTravelTime freett = new FreespeedTravelTime();
//			Dijkstra router = new Dijkstra(network, freett, freett);
			UnweightedDijkstra<Ego> dijkstra = new UnweightedDijkstra<Ego>(socialNet);
			Ego e;
			while((e = egos.poll()) != null) {
				for(Ego alter : egos) {
//					Link egoHome = e.getPerson().getSelectedPlan().getFirstActivity().getLink();
//					Link alterHome = alter.getPerson().getSelectedPlan().getFirstActivity().getLink();
//					Node from = egoHome.getToNode();
//					Node to = alterHome.getFromNode();
//					SparseVertex fromVertex = nodeMap.get(from);
//					SparseVertex toVertex = nodeMap.get(to);
//					Integer tt = null;
//					if(fromVertex != null && toVertex != null) {
//						while(lock.isLocked()) {
//						}
//						lock.lock();
//						for(SparseEdge<Integer> edge : fromVertex.getEdges()) {
//							if(edge.getOpposite(fromVertex) == toVertex)
//								tt = edge.getAttribute();
//						}
//						lock.unlock();
//						
//					}
//					if(tt == null) {
//						if(fromVertex == null) {
//							fromVertex = ttGraph.addVertex();
//							nodeMap.put(from, fromVertex);
//						}
//						if(toVertex == null) {
//							toVertex = ttGraph.addVertex();
//							nodeMap.put(to, toVertex);
//						}
//						Route route = router.calcLeastCostPath(from, to, 0);
//						
//						while(lock.isLocked()) {
//						}
//						lock.lock();
//						SparseEdge<Integer> ttEdge = ttGraph.addEdge(fromVertex, toVertex);
//						ttEdge.setAttribute((int) route.getTravTime());
//						lock.unlock();
//						
//						tt = (int)route.getTravTime();
//					}
					Coord egoHome = e.getPerson().getSelectedPlan().getFirstActivity().getCoord();
					Coord alterHome = alter.getPerson().getSelectedPlan().getFirstActivity().getCoord();
					double geoDist = egoHome.calcDistance(alterHome);
					
//					dijkstra.run(e, alter);
//					List<Ego> path = dijkstra.getPath(e, alter);
//					int topoDist = Integer.MAX_VALUE;
//					if(path != null)
//						topoDist = path.size();
					
//					double p = y0 + A * scale * Math.exp(alpha * tt);
					double p = alpha1 * 1/geoDist;// * alpha2 * Math.pow((double)topoDist, gamma);
					
					if(random.nextDouble() <= p) {
						synchronized(this) {
							if(!e.getNeighbours().contains(alter)) {
								socialNet.addEdge(e, alter);
							}
						}
					}
				}
				processedEgos++;
				if(processedEgos % 100 == 0)
					logger.info(String.format("Processed %1$s of %2$s egos (%3$s).", processedEgos, totalEgos, processedEgos/(double)totalEgos));
			}
		}
	}
}
