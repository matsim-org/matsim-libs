/* *********************************************************************** *
 * project: org.matsim.*
 * RndNetworkGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.johannes.socialnets;

import java.util.HashSet;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;

import edu.uci.ics.jung.algorithms.cluster.ClusterSet;
import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;
import edu.uci.ics.jung.io.GraphMLFile;
import edu.uci.ics.jung.utils.Pair;

public class RndNetworkGenerator {
	
	private static final Logger logger = Logger.getLogger(RndNetworkGenerator.class);

//	private static int numPendingVertices;
	
	private static Queue<Vertex> pendingVerticesRef;
	
	@SuppressWarnings("unchecked")
	public static Graph createGraph(Plans plans, double alpha) throws InterruptedException {
		logger.info("Generating social network...");
		
		UndirectedSparseGraph g = new UndirectedSparseGraph();
		/*
		 * Create a vertex for each person.
		 */
		logger.info("Creating vertices...");
		long randomSeed = Gbl.getConfig().global().getRandomSeed();
		Random rnd = new Random(randomSeed);
		int type1 = 0;
		for(Person p : plans.getPersons().values()) {
			UndirectedSparseVertex v =  new UndirectedSparseVertex();
			g.addVertex(v);
			v.addUserDatum(UserDataKeys.ID, p.getId().toString(), UserDataKeys.COPY_ACT);
			Act act = p.getSelectedPlan().getFirstActivity();
			v.addUserDatum(UserDataKeys.X_COORD, act.getCoord().getX(), UserDataKeys.COPY_ACT);
			v.addUserDatum(UserDataKeys.Y_COORD, act.getCoord().getY(), UserDataKeys.COPY_ACT);
			if(0.1 <= rnd.nextDouble()) {
				v.addUserDatum("type", 1, UserDataKeys.COPY_ACT);
				type1++;
			} else {
				v.addUserDatum("type", 2, UserDataKeys.COPY_ACT);
			}
		}
		logger.info(String.format("Created %1$s vertices.", g.numVertices()));
		logger.info(String.format("%1$s vertices of type 1, %2$s vertices of type 2.", type1, g.numVertices() - type1));
		/*
		 * Insert random ties between persons.
		 */
		logger.info("Creating edges...");
		
		int numThreads = 1;//Runtime.getRuntime().availableProcessors();
		
		int i = 0;
		ConcurrentLinkedQueue<Vertex> pendingVertices = new ConcurrentLinkedQueue<Vertex>(g.getVertices());
		pendingVerticesRef = pendingVertices;
		Thread[] threads = new Thread[numThreads];
		for(i = 0; i < numThreads; i++) {
			threads[i] = new CreateEdgeThread(g, pendingVertices, new Random(randomSeed * i), alpha);
			threads[i].start();
		}
		
		for(i = 0; i < numThreads; i++) {
			threads[i].join();
		}
		
		Set<Vertex> vertices = new HashSet<Vertex>(g.getVertices());
		for(Vertex v : vertices) {
			if(v.degree() == 0)
				g.removeVertex(v);
		}
		
		logger.info(String.format("Inserted %1$s edges.", g.numEdges()));
		ClusterSet clusters = new WeakComponentClusterer().extract(g); 
		logger.info(String.format("Graph has %1$s vertices, %2$s edges, density = %3$s, mean degree = %4$s and %5$s components.",
				g.numVertices(),
				g.numEdges(),
				g.numEdges()/((double)(g.numVertices() * (g.numVertices()-1))),
				GraphStatistics.createDegreeHistogram(g, -1, -1, 0).getMean(),
				clusters.size()));
		StringBuilder builder = new StringBuilder();
		builder.append("Component summary:\n");
		
		for(i = 0; i < clusters.size(); i++) {
			Set cluster = clusters.getCluster(i);
			builder.append("\t");
			builder.append(String.valueOf(i));
			builder.append(" : ");
			builder.append(String.valueOf(cluster.size()));
			builder.append("\n");
		}
		logger.info(builder.toString());
		
		Set<Edge> edges = g.getEdges();
		int count = 0;
		for(Edge e : edges) {
			Pair p = e.getEndpoints();
			Integer v1 = (Integer)((Vertex)p.getFirst()).getUserDatum("type");
			Integer v2 = (Integer)((Vertex)p.getSecond()).getUserDatum("type");
			if(!v1.equals(v2))
				count++;
			
		}
		logger.info(String.format("%1$s edges of %2$s between vertices of different type.", count, g.numEdges()));
		return g;
	}
	
	private static double getTieProba(Vertex v1, Vertex v2, double alpha) {
		Double x1 = (Double) v1.getUserDatum(UserDataKeys.X_COORD);
		Double y1 = (Double) v1.getUserDatum(UserDataKeys.Y_COORD);
		CoordI c1 = new Coord(x1,y1);
		Double x2 = (Double) v2.getUserDatum(UserDataKeys.X_COORD);
		Double y2 = (Double) v2.getUserDatum(UserDataKeys.Y_COORD);
		CoordI c2 = new Coord(x2,y2);
		
		double dist = c1.calcDistance(c2)/1000.0;
		int dv1 = v1.degree();
		if(dv1 == 0)
			dv1 = 1;
		int dv2 = v2.degree();
		if(dv2 == 0)
			dv2 = 1;
		double dSquareSum = Math.pow(dv1, 2) + Math.pow(dv2, 2);
		
		int type1 = (Integer)v1.getUserDatum("type");
		int type2 = (Integer)v2.getUserDatum("type");
		double mixing = 1 - (0.9999 *Math.abs(type1 - type2));
		
		return alpha * 1/Math.pow(dist,2) * mixing;
//		return alpha * 1/dist;
	}
	
	private static synchronized void updateProgress(int edges, int totalVertices) {
		int size = pendingVerticesRef.size();
		logger.info(String.format("Created %1$s edges - %2$s vertices to process (%3$s).", edges, size, 100 * size/(float)totalVertices));
	}

	private static class CreateEdgeThread extends Thread {
		
		private Graph g;
		
		private ConcurrentLinkedQueue<Vertex> pendingVertices;
		
		private Random rnd;
		
		private double alpha;

		public CreateEdgeThread(Graph g, ConcurrentLinkedQueue<Vertex> pendingVertices, Random rnd, double alpha) {
			this.g = g;
			this.pendingVertices = pendingVertices;
			this.rnd = rnd;
			this.alpha = alpha;
		}
		
		public void run() {
			int count = 0; 
			Vertex v1 = pendingVertices.poll();
			while(v1 != null) {
				
				for(Vertex v2 : pendingVertices) {
						rnd.nextDouble();
						if(rnd.nextDouble() <= getTieProba((Vertex) v1, v2, alpha)) {
							try {
								UndirectedSparseEdge e = new UndirectedSparseEdge(v1, v2);
								g.addEdge(e);
								count++;
								if(count % 100 == 0) {
									updateProgress(g.numEdges(), g.numVertices());
								}
							} catch (IllegalArgumentException e) {
								System.err.println("Tried to insert a doubled edge.");
							}
						}
				}
				v1 = pendingVertices.poll();
			}
		}
	}
	
	public static void main(String args[]) throws InterruptedException {
		Config config = Gbl.createConfig(args);
		ScenarioData data = new ScenarioData(config);
		double alpha = Double.parseDouble(config.getParam("randomGraphGenerator", "alpha"));
		
		Plans plans = data.getPopulation();
		Graph g = createGraph(plans, alpha);
		
//		GraphMLFileHandler gmlHandler = new PersonGraphMLFileHandler();
		GraphMLFile gmlFile = new GraphMLFile();
		logger.info("Saving social network...");
		gmlFile.save(g, config.getParam("randomGraphGenerator", "outputFile"));
		logger.info("Done.");
	}
}
