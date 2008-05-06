/* *********************************************************************** *
 * project: org.matsim.*
 * Sampler.java
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

/**
 * 
 */
package playground.johannes.snowball;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.xml.sax.SAXException;

import playground.johannes.socialnets.GraphStatistics;
import playground.johannes.socialnets.PersonGraphMLFileHandler;
import cern.colt.list.IntArrayList;
import edu.uci.ics.jung.algorithms.cluster.ClusterSet;
import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.SparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;
import edu.uci.ics.jung.io.GraphMLFile;
import edu.uci.ics.jung.io.PajekNetWriter;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.utils.UserDataContainer;

/**
 * @author illenberger
 * 
 */
public class Sampler {

	public final static UserDataContainer.CopyAction.Shared COPY_ACT = new UserDataContainer.CopyAction.Shared();

	public static final String PERSON_KEY = "person";

	public static final String WAVE_KEY = "wave";

	private static final Logger logger = Logger.getLogger(Sampler.class);

	private Random rnd = new Random(5);

	public void run(Graph g, int waves, int initialEgos) {
		Collection<Vertex> egos = selectedIntialEgos(g, initialEgos);

		for (Vertex ego : egos) {
			tagAsVisited(ego, 0);
		}

		for (int wave = 1; wave <= waves; wave++) {
			System.out.println("Sampling wave " + wave + "...");
			egos = runWave(egos, wave);
		}
	}

	@SuppressWarnings("unchecked")
	private Collection<Vertex> selectedIntialEgos(Graph g, int count) {
		List<Vertex> edges = new LinkedList<Vertex>(g.getVertices());
		Collections.shuffle(edges, rnd);
		return edges.subList(0, count);
	}

	private Collection<Vertex> runWave(Collection<Vertex> egos, int wave) {
		Collection<Vertex> alters = new LinkedList<Vertex>();
		/*
		 * Expand to each ego's alters.
		 */
		for (Vertex ego : egos) {
			rnd.nextDouble();
			/*
			 * The initial egos (first wave) always participate.
			 */
			if(wave == 1 || getProbaParticipate(ego) >= rnd.nextDouble())
				alters.addAll(expand(ego, wave));
		}

		return alters;
	}

	private void tagAsVisited(Vertex v, int wave) {
		/*
		 * Tag the destination vertex.
		 */
		IntArrayList vertexVisits = (IntArrayList) v.getUserDatum(WAVE_KEY);
		if (vertexVisits == null) {
			vertexVisits = new IntArrayList();
			v.addUserDatum(WAVE_KEY, vertexVisits, COPY_ACT);
		}
		vertexVisits.add(wave);
	}

	private void tagAsVisited(Edge e, int wave) {
		/*
		 * Tag the edge.
		 */
		IntArrayList edgeVisits = (IntArrayList) e.getUserDatum(WAVE_KEY);
		if (edgeVisits == null) {
			edgeVisits = new IntArrayList();
			e.addUserDatum(WAVE_KEY, edgeVisits, COPY_ACT);
		}
		edgeVisits.add(wave);
	}

	@SuppressWarnings("unchecked")
	private Collection<Vertex> expand(Vertex ego, int wave) {
		List<Vertex> sampledAlters = new LinkedList<Vertex>();
		Set<Edge> ties = ego.getOutEdges();

		for (Edge e : ties) {
			Vertex alter = e.getOpposite(ego);
			/*
			 * Do not go back to the ego and do not sample egos that have been
			 * already visited in a previous wave.
			 */
//			if (!alter.equals(ego)) {
				rnd.nextDouble();
				if (getProbaTieNamed(ego, alter) >= rnd.nextDouble()) {
					if (alter.getUserDatum(WAVE_KEY) == null) {
						sampledAlters.add(alter);
					}
					tagAsVisited(e, wave);
					tagAsVisited(alter, wave);
				}
//			}
		}

		return sampledAlters;
	}

	private double getProbaTieNamed(Vertex ego, Vertex alter) {
		return 1;
	}
	
	private double getProbaParticipate(Vertex ego) {
		return 0.1;
	}

	public Graph extractSampledGraph(Graph fullGraph) {
		Graph sampledGraph = new SparseGraph();
		Map<Vertex, Vertex> vertexMapping = new HashMap<Vertex, Vertex>();

		for (Object v : fullGraph.getVertices()) {
			IntArrayList waves = (IntArrayList) ((Vertex) v)
					.getUserDatum(WAVE_KEY);
			if (waves != null) {
				Vertex newVertex = new UndirectedSparseVertex();
				Person p = (Person) ((Vertex) v).getUserDatum(PERSON_KEY);
				newVertex.addUserDatum(PERSON_KEY, p, COPY_ACT);
				newVertex.addUserDatum(WAVE_KEY, waves, COPY_ACT);
				sampledGraph.addVertex(newVertex);

				vertexMapping.put((Vertex) v, newVertex);
			}
		}

		for (Object e : fullGraph.getEdges()) {
			IntArrayList waves = (IntArrayList) ((Edge) e)
					.getUserDatum(WAVE_KEY);
			if (waves != null) {
				Pair endPoints = ((Edge) e).getEndpoints();
				Edge newEdge = new UndirectedSparseEdge(vertexMapping
						.get(endPoints.getFirst()), vertexMapping.get(endPoints
						.getSecond()));
				newEdge.addUserDatum(WAVE_KEY, waves, COPY_ACT);
				sampledGraph.addEdge(newEdge);
			}
		}

		return sampledGraph;
	}

	public static void main(String args[]) {
		Config config = new Config();
		config.addCoreModules();
		Gbl.setConfig(config);
		Gbl.createWorld();

		String networkFile = args[0];
		String plansFile = args[1];
		String graphFile = args[2];
		/*
		 * Load the traffic network...
		 */
		logger.info("Loading network...");
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFile);
		Gbl.getWorld().setNetworkLayer(network);
		/*
		 * Load the population...
		 */
		logger.info("Loading plans...");
		Plans plans = new Plans();
		MatsimPlansReader reader = new MatsimPlansReader(plans);
		try {
			reader.parse(plansFile);
			/*
			 * Load the social network...
			 */
			logger.info("Loading social network...");
			PersonGraphMLFileHandler fileHandler = new PersonGraphMLFileHandler(
					plans);
			GraphMLFile gmlFile = new GraphMLFile(fileHandler);
			Graph g = gmlFile.load(graphFile);
			/*
			 * Simulate snowball sampling...
			 */
			Sampler s = new Sampler();
			s.run(g, 3, 10);
			/*
			 * Compute statistics...
			 */
			Map<String, Integer> sampledVertices = SampleStatistics.countSampledVertices(g);
			StringBuilder sBuilder = new StringBuilder();
			sBuilder.append("Sampled vertex statistics:\n");
			for(String key : sampledVertices.keySet()) {
				sBuilder.append("\t");
				sBuilder.append(key);
				sBuilder.append(":\t");
				sBuilder.append(sampledVertices.get(key));
				sBuilder.append("\n");
			}
			logger.info(sBuilder.toString());
			
			Map<String, Integer> sampledEdges = SampleStatistics.countSampledEdges(g);
			sBuilder = new StringBuilder();
			sBuilder.append("Sampled edge statistics:\n");
			for(String key : sampledEdges.keySet()) {
				sBuilder.append("\t");
				sBuilder.append(key);
				sBuilder.append(":\t");
				sBuilder.append(sampledEdges.get(key));
				sBuilder.append("\n");
			}
			logger.info(sBuilder.toString());
			
			Graph g2 = s.extractSampledGraph(g);
			logger.info("Mean degrees: observed: " + 
					GraphStatistics.meanDegree(g) + 
					"\tsampled: "+GraphStatistics.meanDegree(g2));
			logger.info("Mean clustering coefficient: observed: " + 
					GraphStatistics.meanClusterCoefficient(g) + 
					"\tsampled: "+GraphStatistics.meanClusterCoefficient(g2));
			
			WeakComponentClusterer wcc = new WeakComponentClusterer();
			ClusterSet observerCluster = wcc.extract(g);
			ClusterSet sampledCluster = wcc.extract(g2);
			logger.info("Weak components: observed: " + 
					observerCluster.size() +" components;\tsampled: " +
					sampledCluster.size() + " components.");
			/*
			 * Dump graph for visualization in Pajek.
			 */
			PajekVisWriter w = new PajekVisWriter();
			w.write(g2, "/Users/fearonni/vsp-work/socialnets/devel/snowball/sampled.net");
			w.write(g, "/Users/fearonni/vsp-work/socialnets/devel/snowball/network.net");
		} catch (Exception e) {
			logger.fatal("Loading population failed!", e);
		}
	}
}
