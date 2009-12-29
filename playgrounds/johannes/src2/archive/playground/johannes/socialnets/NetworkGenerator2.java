/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkGenerator2.java
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
import gnu.trove.TIntIntHashMap;
import gnu.trove.TObjectIntHashMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.gbl.Gbl;
import org.matsim.population.Person;
import org.matsim.population.Population;
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
public class NetworkGenerator2 {
	
	private static final Logger logger = Logger.getLogger(NetworkGenerator2.class);

	private Random random;
	
	private final double alpha = 1;
	
	private final double gamma = 1;
	
//	private final int meanDegree = 10;
	
	private final double scale = 10;
	
	private final double scale2 = 100;
	
	private final double scale3 = 100000000;
	
	private static int rndLinkCount = 0;
	
	private List<Ego> egoList = new LinkedList<Ego>();
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Config config = Gbl.createConfig(args);
		logger.info("Loading network and plans...");
		ScenarioData data = new ScenarioData(config);
//		final NetworkLayer network = data.getNetwork();
		final Population population = data.getPopulation();
		
		NetworkGenerator2 g = new NetworkGenerator2();
		SocialNetwork net = g.generate(population);
		dumpStats(net, null, 1);
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
		logger.info("Calculating distance distribution...");
		WeightedStatistics stats3 = new WeightedStatistics();
//		HashMap<Ego, TDoubleDoubleHashMap> egoHist = new HashMap<Ego, TDoubleDoubleHashMap>();
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
//		WeightedStatistics stats3 = new WeightedStatistics();
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
		
		logger.info("Random link selection = "+rndLinkCount);
		
		logger.info("Number of components = " + GraphStatistics.getComponents(socialNet).size());
	}
	
	public SocialNetwork generate(Population population) {
		random = new Random();
		logger.info("Initializing social network...");
		SocialNetwork net = new SocialNetwork();
		
		for(Person p : population) {
			egoList.add(net.addEgo(p));
		}
		
		logger.info("Initializing degree distribution...");
		TObjectIntHashMap<Ego> stubsMap = initDegreeDistribution(net.getVertices());
//		try {
//			dumpStats(net, null, 0);
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		LinkedList<Ego> pendingNodes = new LinkedList<Ego>(net.getVertices());
		Collections.shuffle(pendingNodes);
		while(!pendingNodes.isEmpty()) {
			logger.info("Starting new component...");
			Ego v1 = pendingNodes.getFirst();
			Collection<Ego> nextWave = new HashSet<Ego>();
			int stubs = stubsMap.get(v1);
			for(int i = 0; i < stubs; i++) {
				Ego n1 = findNeighbour(v1, egoList, stubsMap, false);
				if(n1 == null) {
					pendingNodes.remove(v1);
					break;
				}
				if(!formConnection(v1, n1, net, stubsMap)) {
					/*
					 * Should never happen (?)
					 */
					int v2stubs = stubsMap.get(v1);
					int v3stubs = stubsMap.get(n1);
					System.err.println("The selected neighbour is not valid!" + " v2stubs="+v2stubs+", v3stubs="+v3stubs);
					System.exit(-1);
				} else {
					nextWave.add(n1);
					if(stubsMap.get(n1) == 0)
						pendingNodes.remove(n1);
				}
					
			}
			if(stubsMap.get(v1) == 0)
				pendingNodes.remove(v1);
			
			while(!nextWave.isEmpty()) {
				nextWave = closeTriads(nextWave, stubsMap, pendingNodes, net);
			}
		}
		
		int sum = 0;
		for(int val : stubsMap.getValues()) {
			sum += val;
		}
		System.err.println(sum + " stubs left!");
		return net;
	}

	private Collection<Ego> closeTriads(Collection<Ego> currentWave, TObjectIntHashMap<Ego> stubsMap, Collection<Ego> pendingNodes, SocialNetwork net) {
		Collection<Ego> nextWave = new HashSet<Ego>();
		
		for(Ego v1 : currentWave) {
			Set<Ego> potentialTriads = new HashSet<Ego>();
			if(stubsMap.get(v1) > 0)
				potentialTriads.add(v1);
			
			for(Vertex n1 : v1.getNeighbours()) {
				for(Vertex n2 : n1.getNeighbours()) {
					if(n2 != v1) {
						if(stubsMap.get((Ego) n2) > 0)
							potentialTriads.add((Ego) n2);
					}
				}
			}
			
			for(Ego v2 : potentialTriads) {
				int stubs = stubsMap.get(v2);
//				System.err.println("Num stubs before = " + stubs);
//				boolean formedConnection = false;
				for(int i = 0; i < stubs; i++) {
					List<Ego> potentialTriads2 = new LinkedList<Ego>(potentialTriads);
					Collections.shuffle(potentialTriads2);
					
					for (Ego v3 : potentialTriads2) {
						if (v2 != v3) {
							/*
							 * For now, constant clustering
							 */
							double p = getDyadProba(v2, v3, scale3);
							if (random.nextDouble() <= p) {

								if (formConnection(v2, v3, net, stubsMap)) {
//									formedConnection = true;

									if (stubsMap.get(v2) == 0)
										pendingNodes.remove(v2);
									if (stubsMap.get(v3) == 0)
										pendingNodes.remove(v3);

									break;
								}
							}
						}
					}
				}
				
				if (stubsMap.get(v2) > 0) {
					Ego v3 = findNeighbour(v2, egoList, stubsMap, true);
					if (v3 == null) {
						/*
						 * TODO: This is tricky! Obviously there are no nodes
						 * with open stubs left!
						 */
						System.err
								.println("Aborted triad closure. No stubs are left!");
						return nextWave;
					}
					if (formConnection(v2, v3, net, stubsMap)) {
						if (!currentWave.contains(v3))
							nextWave.add(v3);

						if (stubsMap.get(v2) == 0)
							pendingNodes.remove(v2);
						if (stubsMap.get(v3) == 0)
							pendingNodes.remove(v3);
					} else {
						int v2stubs = stubsMap.get(v2);
						int v3stubs = stubsMap.get(v3);
						System.err
								.println("The selected neighbour is not valid!"
										+ " v2stubs=" + v2stubs + ", v3stubs="
										+ v3stubs);
						System.exit(-1);
					}
				}

//				if (stubs - (i + 1) != stubsMap.get(v2)) {
//					System.err.println("Stub not connected!");
//				}
//
//				// System.err.println("Num stubs after = " + stubsMap.get(v1));
//				if (stubsMap.get(v2) != 0) {
//					System.err.println("Vertex has stubs left!");
//				}
			}
		}
		
//		if(nextWave.isEmpty()) {
//			int triadstubs = 0;
//			for(Ego v : potentialTriads)
//				triadstubs += stubsMap.get(v);
//			System.err.println("Triad stubs = " + triadstubs);
//			
//			int totalstubs = 0;
//			for(Ego v : egoList)
//				totalstubs += stubsMap.get(v);
//			System.err.println("Total stubs = " + totalstubs);
//		}
		return nextWave;
	}
	
	private TObjectIntHashMap<Ego> initDegreeDistribution(Collection<? extends Ego> egos) {
		TObjectIntHashMap<Ego> stubsMap = new TObjectIntHashMap<Ego>();
		/*
		 * For now, uniform degree distribution
		 */
		Queue<Ego> egos2 = new LinkedList<Ego>(egos);
		while(!egos2.isEmpty()) {
			Ego e1 = egos2.poll();
			for(Ego e2 : egos) {
				if(random.nextDouble() <= 1/alpha * getDyadProba(e1, e2, scale)) {
					stubsMap.adjustOrPutValue(e1, 1, 1);
					stubsMap.adjustOrPutValue(e2, 1, 1);
				}
			}
		}
		
		return stubsMap;
	}
	
	private double getDyadProba(Ego e1, Ego e2, double scale) {
		Coord c1 = e1.getPerson().getSelectedPlan().getFirstActivity().getCoord();
		Coord c2 = e2.getPerson().getSelectedPlan().getFirstActivity().getCoord();
		double d = c1.calcDistance(c2);
		
		return alpha / Math.pow(1 + d / scale, gamma);
	}
	
	private Ego findNeighbour(Ego e, List<? extends Ego> egos, TObjectIntHashMap<Ego> stubsMap, boolean bias) {
		Collections.shuffle(egos);
		Ego neighbour = null;
		/*
		 * TODO: Probably do Collections.shuffle() here?
		 */
//		TIntIntHashMap pDist = new TIntIntHashMap();
//		double k = stubsMap.get(e) + e.getNeighbours().size();
//		Coord c1 = e.getPerson().getSelectedPlan().getFirstActivity().getCoord();
//		if(bias) {			
//			for(Vertex n : e.getNeighbours()) {
//				Coord c2 = ((Ego) n).getPerson().getSelectedPlan().getFirstActivity().getCoord();
//				double d = c1.calcDistance(c2);
//				pDist.adjustOrPutValue((int)Math.floor(d/5000.0), 1, 1);
//			}
//		}
		
		for (Ego n : egos) {
			if (n != e) {
//				Coord c2 = n.getPerson().getSelectedPlan().getFirstActivity().getCoord();
//				double d = c1.calcDistance(c2);
//				double p = getDyadProba(e, n) - (pDist.get((int)Math.floor(d/5000.0)) / k);
				double p = getDyadProba(e, n, scale2);
				if (random.nextDouble() <= p) {
					if (!n.getNeighbours().contains(e) && stubsMap.get(n) > 0) {
						neighbour = n;
						break;
					}
				}
			}
		}
		/*
		 * If no neighbor has been found, choose one by random.
		 */
		if (neighbour == null) {
			rndLinkCount++;
			for (Ego n : egos) {
				if (n != e) {
					if (!n.getNeighbours().contains(e) && stubsMap.get(n) > 0) {
						neighbour = n;
						break;
					}
				}
			}
		}
		
		return neighbour;
	}
	
	private boolean formConnection(Ego e1, Ego e2, SocialNetwork net, TObjectIntHashMap<Ego> stubsMap) {
		if(e1 == e2)
			return false;
		
		if(stubsMap.get(e1) > 0 && stubsMap.get(e2) > 0) {
			if(!e1.getNeighbours().contains(e2)) {
				net.addEdge(e1, e2);
				if(!stubsMap.adjustValue(e1, -1))
					System.exit(-1);
				if(!stubsMap.adjustValue(e2, -1))
					System.exit(-1);
				return true;
			} else {
				return false;
			}
		} else {
//			System.err.println("Connection cannot be formed because no stubs are left!");
			return false;
		}
	}
}
