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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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

import playground.johannes.snowball2.Clustering;
import playground.johannes.snowball2.CountComponents;
import playground.johannes.snowball2.Degree;
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
	
	private static double kDist;
	
	private static double kMixing;
	
	private static double kMixingRatio;
	
	private static double kClustering;
	
	private static final String ATTRIBUTES_KEY = "attr";

	@SuppressWarnings("unchecked")
	public static Graph createGraph(Plans plans) throws InterruptedException {
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
			VertexAttributes attr = new VertexAttributes();
			attr.x = act.getCoord().getX();
			attr.y = act.getCoord().getY();
			v.addUserDatum(UserDataKeys.X_COORD, act.getCoord().getX(), UserDataKeys.COPY_ACT);
			v.addUserDatum(UserDataKeys.Y_COORD, act.getCoord().getY(), UserDataKeys.COPY_ACT);
			if(kMixingRatio <= rnd.nextDouble()) {
				attr.type = 1;
				v.addUserDatum(UserDataKeys.TYPE_KEY, 1, UserDataKeys.COPY_ACT);
				type1++;
			} else {
				attr.type = 2;
				v.addUserDatum(UserDataKeys.TYPE_KEY, 2, UserDataKeys.COPY_ACT);
			}
			v.addUserDatum(ATTRIBUTES_KEY, attr, UserDataKeys.COPY_ACT);
		}
		logger.info(String.format("Created %1$s vertices.", g.numVertices()));
		logger.info(String.format("%1$s vertices of type 1, %2$s vertices of type 2.", type1, g.numVertices() - type1));
		/*
		 * Insert random ties between persons.
		 */
		logger.info("Creating edges...");
		
		int numThreads = Runtime.getRuntime().availableProcessors();
		
		int i = 0;
		ConcurrentLinkedQueue<Vertex> pendingVertices = new ConcurrentLinkedQueue<Vertex>(g.getVertices());
		
		CreateEdgeThread[] threads = new CreateEdgeThread[numThreads];
		for(i = 0; i < numThreads; i++) {
			threads[i] = new CreateEdgeThread(g, pendingVertices, new Random(randomSeed * i));
			threads[i].start();
		}
		
		for(i = 0; i < numThreads; i++) {
			threads[i].join();
		}
		
		logger.info("Inserting edges...");
		for(i = 0; i < numThreads; i++) {
			for(Edge e : threads[i].edges) {
				try {
					g.addEdge(e);
				} catch (IllegalArgumentException ex) {
					logger.warn("Tried to insert already exsisting edge.");
				}
			}
		}
		
		logger.info("Removing isolates...");
		Set<Vertex> vertices = new HashSet<Vertex>(g.getVertices());
		for(Vertex v : vertices) {
			if(v.degree() == 0)
				g.removeVertex(v);
		}
		
		logger.info("Closing triangles...");
		closeTriangles(g, new Random(randomSeed * numThreads));
		
		logger.info(String.format("Inserted %1$s edges.", g.numEdges()));
		CountComponents cc = new CountComponents(null, null);
		cc.run(g);
		Collection clusters = cc.getClusterSet();  
		logger.info(String.format("Graph has %1$s vertices, %2$s edges, density = %3$s, mean degree = %4$s, mean clusterin = %5$s and %6$s components.",
				g.numVertices(),
				g.numEdges(),
				g.numEdges()/((double)(g.numVertices() * (g.numVertices()-1))),
				new Degree().run(g),
				new Clustering().run(g),
				clusters.size()));
		StringBuilder builder = new StringBuilder();
		builder.append("Component summary:\n");
		
		for(Object cluster : clusters) {
			builder.append("\t");
			builder.append(String.valueOf(i));
			builder.append(" : ");
			builder.append(String.valueOf(((Collection) cluster).size()));
			builder.append("\n");
		}
		logger.info(builder.toString());
		
		Set<Edge> edges = g.getEdges();
		int count = 0;
		for(Edge e : edges) {
			Pair p = e.getEndpoints();
			Integer v1 = (Integer)((Vertex)p.getFirst()).getUserDatum(UserDataKeys.TYPE_KEY);
			Integer v2 = (Integer)((Vertex)p.getSecond()).getUserDatum(UserDataKeys.TYPE_KEY);
			if(!v1.equals(v2))
				count++;
			
		}
		logger.info(String.format("%1$s edges of %2$s between vertices of different type.", count, g.numEdges()));
		return g;
	}
	
	private static double getTieProba(Vertex v1, Vertex v2) {
//		Double x1 = (Double) v1.getUserDatum(UserDataKeys.X_COORD);
//		Double y1 = (Double) v1.getUserDatum(UserDataKeys.Y_COORD);
		VertexAttributes attr1 = (VertexAttributes)v1.getUserDatum(ATTRIBUTES_KEY);
		CoordI c1 = new Coord(attr1.x, attr1.y);
//		Double x2 = (Double) v2.getUserDatum(UserDataKeys.X_COORD);
//		Double y2 = (Double) v2.getUserDatum(UserDataKeys.Y_COORD);
		VertexAttributes attr2 = (VertexAttributes)v2.getUserDatum(ATTRIBUTES_KEY);
		CoordI c2 = new Coord(attr2.x, attr2.y);
		
		double dist = c1.calcDistance(c2)/1000.0;
//		int dv1 = v1.degree();
//		if(dv1 == 0)
//			dv1 = 1;
//		int dv2 = v2.degree();
//		if(dv2 == 0)
//			dv2 = 1;
//		double dSquareSum = Math.pow(dv1, 2) + Math.pow(dv2, 2);
		
		int type1 = attr1.type;//(Integer)v1.getUserDatum(UserDataKeys.TYPE_KEY);
		int type2 = attr2.type;//(Integer)v2.getUserDatum(UserDataKeys.TYPE_KEY);
		double mixing = 1 - (kMixing * Math.abs(type1 - type2));
		
		return kDist * 1/Math.pow(dist,2) * mixing;
//		return alpha * 1/Math.pow(dist,2);
//		return alpha * 1/dist;
	}

	@SuppressWarnings("unchecked")
	private static void closeTriangles(Graph g, Random rnd) {
		Set<Vertex> vertices = g.getVertices();
		int count = 0;
		List<Edge> newEdges = new LinkedList<Edge>();
		for(Vertex v : vertices) {
			Set<Vertex> neighbours1 = v.getNeighbors();
			List<Vertex> neighbours2 = new LinkedList<Vertex>(v.getNeighbors());
			for(Vertex n1 : neighbours1) {
				neighbours2.remove(n1);
				for(Vertex n2 : neighbours2) {
					if(n2.findEdge(n1) == null) {
						if(kClustering >= rnd.nextDouble()) {
							UndirectedSparseEdge e = new UndirectedSparseEdge(n1, n2);
							newEdges.add(e);
							
						}
					}
				}
			}
			count++;
			if(count % 100 == 0) {
				int pending = vertices.size() - count;
				logger.info(String.format("%1$s vertices to process (%2$s).", pending, count/(float)vertices.size() *100));
			}
		}
		
		for(Edge e : newEdges) {
			try {
				g.addEdge(e);
			} catch (Exception ex) {
//				logger.warn("Tried to insert a duplicated edge.");
			}
		}
	}
	
	private static class CreateEdgeThread extends Thread {
		
		private static int vCount;
		
		private static int eCount;
		
		private Graph g;
		
		private ConcurrentLinkedQueue<Vertex> pendingVertices;
		
		private Random rnd;
		
		public List<Edge> edges = new LinkedList<Edge>();
		
//		private double alpha;

		public CreateEdgeThread(Graph g, ConcurrentLinkedQueue<Vertex> pendingVertices, Random rnd) {
			this.g = g;
			this.pendingVertices = pendingVertices;
			this.rnd = rnd;
//			this.alpha = alpha;
		}
		
		public void run() {
			Vertex v1 = pendingVertices.poll();
			while(v1 != null) {
				vCount++;
				for(Vertex v2 : pendingVertices) {
						rnd.nextDouble();
						if(rnd.nextDouble() <= getTieProba((Vertex) v1, v2)) {
								UndirectedSparseEdge e = new UndirectedSparseEdge(v1, v2);
//								g.addEdge(e);
								edges.add(e);
								eCount++;
								if(eCount % 100 == 0) {
									logger.info(String.format("Processed %1$s vertices (%2$s) - %3$s edges.", vCount, vCount/(float)g.numVertices()*100, eCount));
								}
							
						}
				}
				v1 = pendingVertices.poll();
			}
		}
	}
	
	private static class VertexAttributes {
		
		public double x;
		
		public double y;
		
		public int type;
		
	}
	
	public static void main(String args[]) throws InterruptedException {
		Config config = Gbl.createConfig(args);
		ScenarioData data = new ScenarioData(config);
		kDist = Double.parseDouble(config.getParam("randomGraphGenerator", "kDist"));
		kMixing = Double.parseDouble(config.getParam("randomGraphGenerator", "kMixing"));
		kMixingRatio = Double.parseDouble(config.getParam("randomGraphGenerator", "kMixingRatio"));
		kClustering = Double.parseDouble(config.getParam("randomGraphGenerator", "kClustering"));
		Plans plans = data.getPopulation();
		Graph g = createGraph(plans);
		
//		GraphMLFileHandler gmlHandler = new PersonGraphMLFileHandler();
		GraphMLFile gmlFile = new GraphMLFile();
		logger.info("Saving social network...");
		gmlFile.save(g, config.getParam("randomGraphGenerator", "outputFile"));
		logger.info("Done.");
	}
}
