/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkAnalysis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.southafrica.projects.complexNetworks.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import com.google.common.base.Function;

import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.scoring.EigenvectorCentrality;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;
import playground.southafrica.projects.complexNetworks.pathDependence.DigicorePathDependentNetworkReader_v2;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork.PathDependentNode;
import playground.southafrica.utilities.Header;

/**
 * Class to perform a number of network analyses on a given path dependent
 * complex network. It is assumed that the given network is in the national,
 * South African coordinate reference system Hartebeesthoek Lo29 (NE).
 * 
 * @author jwjoubert
 */
public class NetworkAnalysis {
	final private static Logger LOG = Logger.getLogger(NetworkAnalysis.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(NetworkAnalysis.class.toString(), args);
		
		String network = args[0];
		int option = Integer.parseInt(args[1]);
		String output = args[2];
		
		/* Parse the network. */
		DigicorePathDependentNetworkReader_v2 nr = new DigicorePathDependentNetworkReader_v2();
		nr.readFile(network);
		PathDependentNetwork pdn = nr.getPathDependentNetwork();

		switch (option) {
		case 1:
			/* In-, out- and total degree centralities. */
			calculateAndWriteDegreeCentralities(pdn, output);
			break;
		case 2:
			/* convert to JUNG */
			calculateVertexBetweenness(pdn, output);
			break;
		case 3:
			calculateEigen(pdn, output);
			break;
		default:
			break;
		}
		
		
		
		Header.printFooter();
	}
	

	/**
	 * Calculate the in-degree, out-degree and total degree for each node, and
	 * write the results to file. 
	 * 
	 * @param network
	 * @param output
	 */
	private static void calculateAndWriteDegreeCentralities(PathDependentNetwork network, String output){
		LOG.info("Calculating the degree centralities...");
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.HARTEBEESTHOEK94_LO29, TransformationFactory.WGS84);
		
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		Counter counter = new Counter("  vertices # ");
		try{
			bw.write("id,x,y,lon,lat,in,out");
			bw.newLine();
			
			for(PathDependentNode pdn : network.getPathDependentNodes().values()){
				Id<Node> id = pdn.getId();
				Coord cH = pdn.getCoord();
				Coord cWGS = ct.transform(cH);
				int inDegree = pdn.getInDegree();
				int outDegree = pdn.getOutDegree();
				bw.write(String.format("%s,%.0f,%.0f,%.6f,%.6f,%d,%d\n", 
						id, cH.getX(), cH.getY(), cWGS.getX(), cWGS.getY(), inDegree, outDegree));
				counter.incCounter();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + output);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
		counter.printCounter();
		
		LOG.info("Done with degree centralities.");
	}
	
	
	private static void calculateVertexBetweenness(PathDependentNetwork network, String output){
		LOG.info("Converting the path-dependent network to JUNG sparse graph...");
		DirectedSparseGraph<Id<Node>, Pair<Id<Node>>> graph = new DirectedSparseGraph<>();
		Map<Pair<Id<Node>>, Number> edgeWeights = new HashMap<Pair<Id<Node>>, Number>();
		
		Counter counter = new Counter("  edges # ");
		LOG.info("Total number of edges: " + network.getNumberOfEdges());
		for(Id<Node> oId : network.getEdges().keySet()){
			Map<Id<Node>, Double> thisMap = network.getEdges().get(oId);
			for(Id<Node> dId : thisMap.keySet()){
				double weight = thisMap.get(dId);
				Pair<Id<Node>> pair = new Pair<Id<Node>>(oId, dId);
				graph.addEdge(pair, oId, dId, EdgeType.DIRECTED);
				edgeWeights.put(pair, weight);
				counter.incCounter();
			}
		}
		counter.printCounter();
		LOG.info("Done with JUNG sparse graph.");
		
		
		LOG.info("Calculating vertex betweenness...");
		BetweennessCentrality<Id<Node>, Pair<Id<Node>>> bc = new BetweennessCentrality<>(graph);
		bc.setEdgeWeights(edgeWeights);
		bc.setRemoveRankScoresOnFinalize(false);
		bc.evaluate();
		LOG.info("Done calculating betweenness.");
		
		LOG.info("Writing the output to " + output);
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.HARTEBEESTHOEK94_LO29, 
				TransformationFactory.WGS84);
		try{
			try {
				bw.write("id,x,y,lon,lat,bc,ec");
				bw.newLine();
				for(Id<Node> id : network.getEdges().keySet()){
					Coord cH = network.getPathDependentNode(id).getCoord();
					Coord cWGS = ct.transform(cH);
					double bcDouble = bc.getVertexRankScore(id);
					bw.write(String.format("%s,%.0f,%.0f,%.6f,%.6f,%f,%s\n", 
							id, cH.getX(), cH.getY(), cWGS.getX(), cWGS.getY(), bcDouble, "NA"));
				}
			} catch (IOException e) {
				throw new RuntimeException("Could not write to BufferedWriter " + output);
			}			
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter " + output);
			}
		}
	}
	
	
	private static void calculateEigen(PathDependentNetwork network, String output){
		LOG.info("Converting the path-dependent network to JUNG sparse graph...");
		DirectedSparseGraph<Id<Node>, Pair<Id<Node>>> graph = new DirectedSparseGraph<>();
		Map<Pair<Id<Node>>, Number> edgeWeights = new HashMap<Pair<Id<Node>>, Number>();
		
		Counter counter = new Counter("  edges # ");
		LOG.info("Total number of edges: " + network.getNumberOfEdges());
		for(Id<Node> oId : network.getEdges().keySet()){
			Map<Id<Node>, Double> thisMap = network.getEdges().get(oId);
			for(Id<Node> dId : thisMap.keySet()){
				double weight = thisMap.get(dId);
				Pair<Id<Node>> pair = new Pair<Id<Node>>(oId, dId);
				graph.addEdge(pair, oId, dId, EdgeType.DIRECTED);
				edgeWeights.put(pair, weight);
				counter.incCounter();
			}
		}
		counter.printCounter();
		LOG.info("Done with JUNG sparse graph.");
		
		LOG.info("Calculating vertex eigenvalue centralities...");
		EigenvectorCentrality<Id<Node>, Pair<Id<Node>>> ec = 
				new EigenvectorCentrality<>(graph);
		Function<Pair<Id<Node>>, Number> t = new Function<Pair<Id<Node>>, Number>() {
			@Override
			public Number apply(Pair<Id<Node>> edge) {
				return network.getWeight(edge.getFirst(), edge.getSecond());
			}
		};
		ec.setEdgeWeights(t);
		LOG.info("   tolerance: " + ec.getTolerance());
		LOG.info("   max iterations: " + ec.getMaxIterations());
		LOG.info("Setting new tolerance and interations...");
		ec.setMaxIterations(1000);
		ec.setTolerance(1e-5);
		LOG.info("   tolerance: " + ec.getTolerance());
		LOG.info("   max iterations: " + ec.getMaxIterations());		
		ec.acceptDisconnectedGraph(true);
		ec.initialize();
		try{
			ec.evaluate();
		} catch(IllegalArgumentException e){
			LOG.error("Cannot complete Eigenvector centrality... need to add artificial edges.");
//			DigicoreNetwork network2 = addArtificialEdges(network);
//			ec = new EigenvectorCentrality<Id<ActivityFacility>, Pair<Id<ActivityFacility>>>(network2);
//			ec.setEdgeWeights(t);			
//			ec.setMaxIterations(10000);
//			ec.setTolerance(1e-8 );
//			LOG.info("   tolerance: " + ec.getTolerance());
//			LOG.info("   max iterations: " + ec.getMaxIterations());	
//			ec.acceptDisconnectedGraph(true);
//			ec.initialize();
//			ec.evaluate();
			throw new RuntimeException("Must first implement method to eliminate sources and sinks.");
		}
		LOG.info("Done calculating eigenvalue centrality.");
		
		LOG.info("Writing the output to " + output);
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.HARTEBEESTHOEK94_LO29, 
				TransformationFactory.WGS84);
		try{
			try {
				bw.write("id,x,y,lon,lat,bc,ec");
				bw.newLine();
				for(Id<Node> id : network.getEdges().keySet()){
					Coord cH = network.getPathDependentNode(id).getCoord();
					Coord cWGS = ct.transform(cH);
					double ecDouble =ec.getVertexScore(id);
					bw.write(String.format("%s,%.0f,%.0f,%.6f,%.6f,%s,%f\n", 
							id, cH.getX(), cH.getY(), cWGS.getX(), cWGS.getY(), "NA", ecDouble));
				}
			} catch (IOException e) {
				throw new RuntimeException("Could not write to BufferedWriter " + output);
			}			
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter " + output);
			}
		}
	}
	
	

}
