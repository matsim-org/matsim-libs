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

import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;
import edu.uci.ics.jung.io.GraphMLFile;
import edu.uci.ics.jung.io.GraphMLFileHandler;

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
		for(Person p : plans.getPersons().values()) {
			UndirectedSparseVertex v =  new UndirectedSparseVertex();
			g.addVertex(v);
			v.addUserDatum(UserDataKeys.ID, p.getId().toString(), UserDataKeys.COPY_ACT);
			Act act = p.getSelectedPlan().getFirstActivity();
			v.addUserDatum(UserDataKeys.X_COORD, act.getCoord().getX(), UserDataKeys.COPY_ACT);
			v.addUserDatum(UserDataKeys.Y_COORD, act.getCoord().getY(), UserDataKeys.COPY_ACT);
		}
		logger.info(String.format("Created %1$s vertices.", g.numVertices()));
		/*
		 * Insert random ties between persons.
		 */
		logger.info("Creating edges...");
		long randomSeed = Gbl.getConfig().global().getRandomSeed();
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
		
		Set<Vertex> vertices = new HashSet<Vertex>();
		for(Vertex v : vertices) {
			if(v.degree() == 0)
				g.removeVertex(v);
		}
		
		logger.info(String.format("Inserted %1$s edges.", g.numEdges()));
		logger.info(String.format("Graph has %1$s vertices, %2$s edges, density = %3$s, mean degree = %4$s and %5$s components.",
				g.numVertices(),
				g.numEdges(),
				g.numEdges()/((double)(g.numVertices() * (g.numVertices()-1))),
				GraphStatistics.createDegreeHistogram(g, -1, -1, 0).mean(),
				new WeakComponentClusterer().extract(g).size()));
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
		
		return alpha * 1/Math.pow(dist,2);
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
		
		GraphMLFileHandler gmlHandler = new PersonGraphMLFileHandler();
		GraphMLFile gmlFile = new GraphMLFile(gmlHandler);
		logger.info("Saving social network...");
		gmlFile.save(g, config.getParam("randomGraphGenerator", "outputFile"));
		logger.info("Done.");
	}
}
