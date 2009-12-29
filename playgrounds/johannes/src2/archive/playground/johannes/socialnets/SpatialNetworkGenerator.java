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

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
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
import org.matsim.utils.collections.Tuple;
import org.matsim.utils.geometry.Coord;

import playground.johannes.graph.Edge;
import playground.johannes.graph.GraphStatistics;
import playground.johannes.graph.Vertex;
import playground.johannes.graph.io.PajekVisWriter;
import playground.johannes.socialnet.Ego;
import playground.johannes.socialnet.SocialNetwork;
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
		

		logger.info("Initializing threads step 1...");
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
		
		dumpStats(socialNet, args[1], 0);
		
		for (int k = 0; k < 0; k++) {
			logger.info("Initializing threads setp "+(k+2)+"...");

			Step2Thread.totalEgos = socialNet.getVertices().size();
			Step2Thread[] threads2 = new Step2Thread[count];
			for (int i = 0; i < count; i++) {
				threads2[i] = new Step2Thread(socialNet, new Random(i*(k+1)));
			}
			for (Thread thread : threads2) {
				thread.start();
			}
			for (Thread thread : threads2) {
				thread.join();
			}

			dumpStats(socialNet, args[1], k+1);
		}
	}
	
	private static void dumpStats(SocialNetwork socialNet, String histfile, int it) throws FileNotFoundException, IOException {
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
		WeightedStatistics.writeHistogram(wstats.absoluteDistribution(), "/Users/fearonni/vsp-work/socialnets/data-analysis/socialnetgenerator/"+it+".degreehist.txt");
		
		double clustering = GraphStatistics.getClusteringStatistics(socialNet).getMean();
		logger.info(String.format("Mean clustering coefficient is %1$s.", clustering));
		
		double mutuality = GraphStatistics.getMutuality(socialNet);
		logger.info(String.format("Mutuality is %1$s.", mutuality));
		
		double dcorrelation = GraphStatistics.getDegreeCorrelation(socialNet);
		logger.info(String.format("Degree correlation is %1$s.", dcorrelation));
		
//		logger.info(String.format("Closeness is %1$s.", GraphStatistics.getCentrality(socialNet).getGraphCloseness()));
		
		WeightedStatistics stats3 = new WeightedStatistics();
		double binsize = 1000;
		for (Ego ego : socialNet.getVertices()) {
			TDoubleDoubleHashMap hist = new TDoubleDoubleHashMap();
			Coord c1 = ego.getPerson().getSelectedPlan().getFirstActivity()
			.getCoord();
			for (Ego p2 : socialNet.getVertices()) {
				
				Coord c2 = p2.getPerson().getSelectedPlan().getFirstActivity()
						.getCoord();
				double d = c1.calcDistance(c2);
				double bin = Math.floor(d / binsize);
				double val = hist.get(bin);
				val++;
				hist.put(bin, val);
			}
//			egoHist.put(ego, hist);
			
			for(Vertex n : ego.getNeighbours()) {
				Coord c2 = ((Ego) n).getPerson().getSelectedPlan().getFirstActivity().getCoord();
				double dist = c1.calcDistance(c2);
				stats3.add(dist, 1/hist.get(Math.floor(dist / binsize)));
			}
		}
//		for(Object o : socialNet.getEdges()) {
//			Edge e = (Edge)o;
//			Ego e1 = (Ego) e.getVertices().getFirst();
//			Ego e2 = (Ego) e.getVertices().getSecond();
//			Coord c1 = e1.getPerson().getSelectedPlan().getFirstActivity().getCoord();
//			Coord c2 = e2.getPerson().getSelectedPlan().getFirstActivity().getCoord();
//			double dist = c1.calcDistance(c2);
//			stats3.add(dist);
//		}
		WeightedStatistics.writeHistogram(stats3.absoluteDistribution(1000), "/Users/fearonni/vsp-work/socialnets/data-analysis/socialnetgenerator/"+it+".edgelength.txt");
		
		PajekVisWriter pWriter = new PajekVisWriter();
		pWriter.write(socialNet, "/Users/fearonni/vsp-work/socialnets/data-analysis/socialnetgenerator/"+it+".socialnet.net");
		
		WeightedStatistics stats4 = new WeightedStatistics();
		for(Object o : socialNet.getEdges()) {
			Edge e = (Edge)o;
			Ego e1 = (Ego) e.getVertices().getFirst();
			Ego e2 = (Ego) e.getVertices().getSecond();
			int age1 = e1.getPerson().getAge();
			int age2 = e2.getPerson().getAge();
			double dAge = 0;
			if(age1 > age2)
				dAge = age1/(double)age2;
			else
				dAge = age2/(double)age1;
			stats4.add(dAge);
		}
		WeightedStatistics.writeHistogram(stats4.absoluteDistribution(0.05), "/Users/fearonni/vsp-work/socialnets/data-analysis/socialnetgenerator/"+it+".agedist.txt");
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
//			UnweightedDijkstra<Ego> dijkstra = new UnweightedDijkstra<Ego>(socialNet);
			Ego e;
			while((e = egos.poll()) != null) {
//				HashMap<Ego, TDoubleDoubleHashMap> egoHist = new HashMap<Ego, TDoubleDoubleHashMap>();
//				double binsize = 1000;
////				for(Ego ego : socialNet.getVertices()) {
//					TDoubleDoubleHashMap hist = new TDoubleDoubleHashMap();
//					for(Ego p2 : socialNet.getVertices()) {
//						Coord c1 = e.getPerson().getSelectedPlan().getFirstActivity().getCoord();
//						Coord c2 = p2.getPerson().getSelectedPlan().getFirstActivity().getCoord();
//						double d = c1.calcDistance(c2);
//						double bin = Math.floor(d/binsize);
//						double val = hist.get(bin);
//						val++;
//						hist.put(bin, val);
//					}
////					egoHist.put(ego, hist);
////				}
				
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
					
					int age1 = e.getPerson().getAge();
					int age2 = alter.getPerson().getAge();
					double dAge = 0;
					if(age1 > age2)
						dAge = age1/(double)age2;
					else
						dAge = age2/(double)age1;
					
//					double beta1 = 0.001;
//					double beta2 = 10;
//					double dist = Math.sqrt(Math.pow(beta1 * geoDist, 2) + Math.pow(beta2 * dAge, 2));
//					double p = alpha1 / Math.pow(1 + dist, gamma);
//					double p = alpha1 * 1/(Math.pow(1 + geoDist, gamma));
//					double p = alpha1 * 1/(Math.pow(1 + geoDist * hist.get(Math.floor(geoDist/binsize)), gamma));
					double p = alpha1 * 1/Math.pow(1 + geoDist, 1);// + alpha2 * 1/Math.pow(dAge, 1000);
					
					if(random.nextDouble() <= p) {
//						synchronized(this) {
							if(!e.getNeighbours().contains(alter)) {
								socialNet.addEdge(e, alter);
//							}
						}
					}
				}
				processedEgos++;
				if(processedEgos % 100 == 0)
					logger.info(String.format("Processed %1$s of %2$s egos (%3$s).", processedEgos, totalEgos, processedEgos/(double)totalEgos));
			}
		}
	}
	
	private static final class Step2Thread extends Thread {
		
		private static int totalEgos;
		
		private static int processedEgos;
		
		private SocialNetwork socialNet;
		
		private Random random;
		
		public Step2Thread(SocialNetwork socialNet, Random random) {
			this.socialNet = socialNet;
			this.random = random;
		}
		
		public void run() {
			LinkedList<Tuple<Ego, Ego>> edges = new LinkedList<Tuple<Ego, Ego>>();
			LinkedList<Ego> egos = new LinkedList<Ego>(socialNet.getVertices());
			Ego e;
			while((e = egos.poll()) != null) {
				TObjectDoubleHashMap<Ego> secondNeighbours = new TObjectDoubleHashMap<Ego>();
				for(Vertex n1 : e.getNeighbours()) {
					for(Vertex n2 : n1.getNeighbours()) {
						if(n2 != e) {
							Coord egoHome = e.getPerson().getSelectedPlan().getFirstActivity().getCoord();
							Coord alterHome = ((Ego) n2).getPerson().getSelectedPlan().getFirstActivity().getCoord();
							double geoDist = egoHome.calcDistance(alterHome);
							double p1 = 10 * alpha1 * 1/Math.pow(geoDist, 2);
							double p2 = 0;//10 * secondNeighbours.get((Ego) n2);
							secondNeighbours.put((Ego)n2, (p2+p1) - (p1*p2));
//							if(random.nextDouble() <= p) {
//								edges.add(new Tuple<Ego, Ego>(e, (Ego) n2));
//							}
						}
					}
				}
				
				for(Object alter : secondNeighbours.keys()) {
					if(random.nextDouble() <= secondNeighbours.get((Ego) alter)) {
						edges.add(new Tuple<Ego, Ego>(e, (Ego) alter));
					}
				}
				processedEgos++;
				if(processedEgos % 100 == 0)
					logger.info(String.format("Processed %1$s of %2$s egos (%3$s).", processedEgos, totalEgos, processedEgos/(double)totalEgos));
			
			}
			
			for(Tuple<Ego, Ego> t : edges) {
				e = t.getFirst();
				Ego alter = t.getSecond();
				if(!e.getNeighbours().contains(alter)) {
					socialNet.addEdge(e, alter);
				}
			}
			
		}
	}
}
