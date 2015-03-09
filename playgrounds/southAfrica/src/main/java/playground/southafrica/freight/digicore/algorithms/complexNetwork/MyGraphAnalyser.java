/* *********************************************************************** *
 * project: org.matsim.*
 * MyGraphAnalyser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.southafrica.freight.digicore.algorithms.complexNetwork;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;

import playground.southafrica.freight.digicore.containers.DigicoreNetwork;
import playground.southafrica.utilities.Header;
import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.scoring.EigenvectorCentrality;
import edu.uci.ics.jung.graph.util.Pair;

public class MyGraphAnalyser {
	private final static Logger LOG = Logger.getLogger(MyGraphAnalyser.class);

	/**
	 * Implementation of different network and graph analysis. The first argument 
	 * indicates which analysis to run.
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(MyGraphAnalyser.class.toString(), args);
		
		DigicoreNetworkParser dnp  = new DigicoreNetworkParser();
		DigicoreNetwork network = null;
		try {
			network = dnp.parseNetwork(args[0]);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't read network from " + args[0] );
		}
		
		int run = Integer.parseInt(args[1]);
		switch (run) {
		case 1:
			LOG.info("Calculating the degree distribution.");
			calculateDegreeDistribution(args, network, false);
			break;
		case 2:
			LOG.info("Calculating the weighted degree distribution.");
			calculateDegreeDistribution(args, network, true);
			break;
		case 3:	
			LOG.info("Calaculating unweighted `Q' from Molloy and Reed.");
			calculateMolloyReedQ(args, network, false);
			break;
		case 4:
			LOG.info("Just write out ALL the weighted degree values.");
			writeAllDegreeValues(args, network, true);
			break;
		case 5:
			LOG.info("Just write out ALL the degree values.");
			writeAllDegreeValues(args, network, false);
			break;
		case 6:
			LOG.info("Write the edge list and weights for R.");
			writeEdgeListForR(args, network, true);
			break;
		case 7:
			LOG.info("Calculate betweenness and eigenvalue centrality for R visualisation.");
			calculateBetweennessAndEigenvalueCentrality(args, network);
		default:
			break;
		}
		LOG.info("========================   DONE   ==========================");
	}
	
	static class MyTransformer implements Transformer<Pair<Id<ActivityFacility>>, Number>{
		private final DigicoreNetwork network;
		
		public MyTransformer(DigicoreNetwork network) {
			this.network = network;
		}
		
		@Override
		public Number transform(Pair<Id<ActivityFacility>> edge) {
			return this.network.getWeights().get(edge);
		}
	}
	
	
	private static void calculateBetweennessAndEigenvalueCentrality(String[] args, DigicoreNetwork network){
		/* Create a map with edge weights. First, consolidate all activity types
		 * so that only a single edge weight exists. */
		Map<Pair<Id<ActivityFacility>>,Number> weightMap = new HashMap<Pair<Id<ActivityFacility>>, Number>();
		
		for(Tuple<Pair<Id<ActivityFacility>>, Pair<String> > tuple : network.getWeights().keySet()){
			if(!weightMap.containsKey(tuple.getFirst())){
				weightMap.put(tuple.getFirst(), network.getWeights().get(tuple));
			} else{
				weightMap.put(tuple.getFirst(), 
						new Integer(weightMap.get(tuple.getFirst()).intValue() + network.getWeights().get(tuple)));
			}
		}
		
		LOG.info("Evaluating the betweenness centrality...");
		BetweennessCentrality<Id<ActivityFacility>, Pair<Id<ActivityFacility>>> bc = 
				new BetweennessCentrality<Id<ActivityFacility>, Pair<Id<ActivityFacility>>>(network);
		bc.setEdgeWeights(weightMap);
		bc.setRemoveRankScoresOnFinalize(false);
		bc.evaluate();
		
		LOG.info("Evaluating the Eigenvector centrality...");
		EigenvectorCentrality<Id<ActivityFacility>, Pair<Id<ActivityFacility>>> ec = 
				new EigenvectorCentrality<Id<ActivityFacility>, Pair<Id<ActivityFacility>>>(network);
		Transformer<Pair<Id<ActivityFacility>>, Number> t = new MyTransformer(network);
		ec.setEdgeWeights(t);
		LOG.info("   tolerance: " + ec.getTolerance());
		LOG.info("   max iterations: " + ec.getMaxIterations());
		LOG.info("Setting new tolerance and interations...");
		ec.setMaxIterations(10000);
		ec.setTolerance(1e-8);
		LOG.info("   tolerance: " + ec.getTolerance());
		LOG.info("   max iterations: " + ec.getMaxIterations());		
		ec.acceptDisconnectedGraph(true);
		ec.initialize();

		
		try{
			ec.evaluate();
		} catch(IllegalArgumentException e){
			LOG.error("Cannot complete Eigenvector centrality... adding artificial edges.");
			DigicoreNetwork network2 = addArtificialEdges(network);
			ec = new EigenvectorCentrality<Id<ActivityFacility>, Pair<Id<ActivityFacility>>>(network2);
			ec.setEdgeWeights(t);			
			ec.setMaxIterations(10000);
			ec.setTolerance(1e-8 );
			LOG.info("   tolerance: " + ec.getTolerance());
			LOG.info("   max iterations: " + ec.getMaxIterations());	
			ec.acceptDisconnectedGraph(true);
			ec.initialize();
			ec.evaluate();
		}
		
		LOG.info("Writing the output to " + args[2]);
		BufferedWriter bw = IOUtils.getBufferedWriter(args[2], true);
		try{
			try {
				bw.write("NodeId,Long,Lat,BC,EC");
				bw.newLine();
				for(Id<ActivityFacility> node : network.getVertices()){
					bw.write(node.toString());
					bw.write(",");
					Coord c = network.getCoordinates().get(node);
					bw.write(String.format("%.2f,%.2f,", c.getX(), c.getY()));
					bw.write(String.valueOf(bc.getVertexRankScore(node)));
					bw.write(",");
					bw.write(String.valueOf(ec.getVertexScore(node)));
					bw.newLine();
				}
			} catch (IOException e) {
				throw new RuntimeException("Could not write to BufferedWriter " + args[2]);
			}			
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter " + args[2]);
			}
		}
	}

	
	@SuppressWarnings("unchecked")
	private static DigicoreNetwork addArtificialEdges(DigicoreNetwork network){
		/* Duplicate the current network. */
		LOG.info("   Duplicating the network...");
		int sourceCount = 0;
		int sinkCount = 0;
		DigicoreNetwork newNetwork = new DigicoreNetwork();
		for(Tuple<Pair<Id<ActivityFacility>>, Pair<String> > tuple : network.getWeights().keySet()){
			Pair<Id<ActivityFacility>> edge = tuple.getFirst();
			Id<ActivityFacility> first = edge.getFirst();
			if(!newNetwork.containsVertex(first)){
				newNetwork.addVertex(first);
				newNetwork.getCoordinates().put(first, network.getCoordinates().get(first));
			}
			Id<ActivityFacility> second = edge.getSecond();
			if(!newNetwork.containsVertex(second)){
				newNetwork.addVertex(second);
				newNetwork.getCoordinates().put(second, network.getCoordinates().get(second));
			}
			newNetwork.addEdge(edge, first, second);
			newNetwork.getWeights().put(tuple, network.getWeights().get(tuple));
		}
		
		/* Evaluate each node as being either a sink or source. */
		LOG.info("   Looking for sources and sinks...");
		for(Id<ActivityFacility> node : network.getVertices()){
			/* Add the node to the network. */
			boolean isSource = network.getPredecessorCount(node) == 0 ? true : false;
			boolean isSink = network.getSuccessorCount(node) == 0 ? true : false;
			
			if(isSource){
				LOG.warn("   Node " + node.toString() + " is a source.");
				sourceCount++;
				Object o = network.getVertices().toArray()[(int) Math.round(Math.random()*network.getVertexCount())];
				Id<ActivityFacility> someOrigin = null;
				if(o instanceof Id<?>){
					Id<?> someId = (Id<?>)o;
					someOrigin = (Id<ActivityFacility>) someId;
				} else{
					throw new RuntimeException("Cannot sample an origin node for source: sampled object is of type " + o.getClass().toString());
				}
				/* Add new edge with weight 1. */
				Pair<Id<ActivityFacility>> newEdge = new Pair<Id<ActivityFacility>>(someOrigin, node);
				newNetwork.addEdge(newEdge, someOrigin, node);
				newNetwork.getWeights().put(new Tuple<Pair<Id<ActivityFacility>>, Pair<String>>(newEdge, new Pair<String>("dummy","dummy")), 1);
			}
			if(isSink){
				LOG.warn("   Node " + node.toString() + " is a sink.");
				sinkCount++;
				Object o = network.getVertices().toArray()[(int) Math.round(Math.random()*network.getVertexCount())];
				Id<ActivityFacility> someDestination = null;
				if(o instanceof Id){
					someDestination = (Id<ActivityFacility>) o;
				} else{
					throw new RuntimeException("Cannot sample a destination node for sink: sampled object is of type " + o.getClass().toString());
				}
				/* Add new edge with weight 1. */
				Pair<Id<ActivityFacility>> newEdge = new Pair<Id<ActivityFacility>>(node, someDestination);
				newNetwork.addEdge(newEdge, node, someDestination);
				newNetwork.getWeights().put(new Tuple<Pair<Id<ActivityFacility>>, Pair<String>>(newEdge, new Pair<String>("dummy","dummy")), 1);
			}
		}
		LOG.info("   Edges added to eliminate sources: " + sourceCount);
		LOG.info("   Edges added to eliminate sinks  : " + sinkCount);
		return newNetwork;
	}

	private static void writeEdgeListForR(String[] args, DigicoreNetwork network, boolean weighted) {
		BufferedWriter bw = IOUtils.getBufferedWriter(args[2]);
		LOG.info("Total number of edges to write: " + network.getEdgeCount());
		Counter c = new Counter("   edges: ");
		try{
			bw.write("From,To,Weight,ox,oy,dx,dy");
			bw.newLine();
			for(Pair<Id<ActivityFacility>> arc : network.getEdges()){
				bw.write(arc.getFirst().toString());
				bw.write(",");
				bw.write(arc.getSecond().toString());
				bw.write(",");
				bw.write(String.valueOf(network.getEdgeWeight(arc.getFirst(), arc.getSecond())));
				bw.write(",");
				bw.write(String.format("%.4f", network.getCoordinates().get(arc.getFirst()).getX()));
				bw.write(",");
				bw.write(String.format("%.4f", network.getCoordinates().get(arc.getFirst()).getY()));
				bw.write(",");
				bw.write(String.format("%.4f", network.getCoordinates().get(arc.getSecond()).getX()));
				bw.write(",");
				bw.write(String.format("%.4f", network.getCoordinates().get(arc.getSecond()).getY()));
				bw.newLine();
				c.incCounter();
			}
			c.printCounter();
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter " + args[2]);
		}finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter " + args[2]);
			}
		}
	}


	private static void calculateDegreeDistribution(String[] args, DigicoreNetwork network, boolean weighted) {
		Map<String, Integer> inDegreeMap = new HashMap<String, Integer>(network.getVertexCount());
		Map<String, Integer> outDegreeMap = new HashMap<String, Integer>(network.getVertexCount());
		Map<String, Integer> degreeMap = new HashMap<String, Integer>(network.getVertexCount());
		
		for(Id<ActivityFacility> node : network.getVertices()){
			/* First for in-degree. */
			int inDegree = getInDegree(network, node, weighted);
			if(inDegree > 0){
				String inDegreeId = String.valueOf(inDegree);
				if(!inDegreeMap.containsKey(inDegreeId)){
					inDegreeMap.put(inDegreeId, 1);
				} else{
					int old = inDegreeMap.get(inDegreeId);
					inDegreeMap.put(inDegreeId,  old + 1);
				}
			} else{
				/* Disregard in-degree with value 0. */
			}
			
			/* Next for out-degree. */
			int outDegree = getOutDegree(network, node, weighted);
			if(outDegree > 0){
				String outDegreeId =String.valueOf(outDegree);
				if(!outDegreeMap.containsKey(outDegreeId)){
					outDegreeMap.put(outDegreeId, 1);
				} else{
					int old = outDegreeMap.get(outDegreeId);
					outDegreeMap.put(outDegreeId,  old + 1);
				}
			} else{
				/* Disregard out-degree with value 0. */
			}
			
			/* Last for total degree. */
			int degree = inDegree + outDegree;
			if(degree > 0){
				String degreeId = String.valueOf(degree);
				if(!degreeMap.containsKey(degreeId)){
					degreeMap.put(degreeId, 1);
				} else{
					int old = degreeMap.get(degreeId);
					degreeMap.put(degreeId,  old + 1);
				}
			} else{
				/* Disregard degree with value 0. */ 
				LOG.warn("Should have nodes with zero degree.");
			}
		}
		
		printDegreeDistribution(args[2], inDegreeMap);
		printDegreeDistribution(args[3], outDegreeMap);
		printDegreeDistribution(args[4], degreeMap);
	}


	private static void calculateMolloyReedQ(String[] args, DigicoreNetwork network, boolean weighted){
		Map<String, Integer> inDegreeMap = new HashMap<String, Integer>(network.getVertexCount());
		Map<String, Integer> outDegreeMap = new HashMap<String, Integer>(network.getVertexCount());
		Map<String, Integer> degreeMap = new HashMap<String, Integer>(network.getVertexCount());
		
		for(Id<ActivityFacility> node : network.getVertices()){
			/* First for in-degree. */
			int inDegree = getInDegree(network, node, weighted);
			if(inDegree > 0){
				String inDegreeId = String.valueOf(inDegree);
				if(!inDegreeMap.containsKey(inDegreeId)){
					inDegreeMap.put(inDegreeId, 1);
				} else{
					int old = inDegreeMap.get(inDegreeId);
					inDegreeMap.put(inDegreeId,  old + 1);
				}
			} else{
				/* Disregard in-degree with value 0. */
			}
			
			/* Next for out-degree. */
			int outDegree = getOutDegree(network, node, weighted);
			if(outDegree > 0){
				String outDegreeId = String.valueOf(outDegree);
				if(!outDegreeMap.containsKey(outDegreeId)){
					outDegreeMap.put(outDegreeId, 1);
				} else{
					int old = outDegreeMap.get(outDegreeId);
					outDegreeMap.put(outDegreeId,  old + 1);
				}
			} else{
				/* Disregard out-degree with value 0. */
			}
			
			/* Last for total degree. */
			int degree = inDegree + outDegree;
			if(degree > 0){
				String degreeId = String.valueOf(degree);
				if(!degreeMap.containsKey(degreeId)){
					degreeMap.put(degreeId, 1);
				} else{
					int old = degreeMap.get(degreeId);
					degreeMap.put(degreeId,  old + 1);
				}
			} else{
				/* Disregard degree with value 0. */ 
				LOG.warn("Should have nodes with zero degree.");
			}
		}
		LOG.info("---------- Results from Molly & Reed ------------");
		LOG.info(" in-Degree Q-value : " + calculateQ(inDegreeMap));
		LOG.info(" out-Degree Q-value: " + calculateQ(outDegreeMap));
		LOG.info(" Degree Q-value    : " + calculateQ(degreeMap));
	}
	
	private static void writeAllDegreeValues(String[] args, DigicoreNetwork network, boolean weighted){
		BufferedWriter bw = IOUtils.getBufferedWriter(args[2]);
		Counter c = new Counter("   nodes: ");
		try{
			for(Id<ActivityFacility> node : network.getVertices()){
				try {
					bw.write(String.valueOf(getInDegree(network, node, weighted)));
					bw.newLine();
				} catch (IOException e) {
					throw new RuntimeException("Could not write to BufferedWriter " + args[2]);
				}
				c.incCounter();
			}
			c.printCounter();
		}finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter " + args[2]);
			}
		}
	}
	
	
	private static double calculateQ(Map<String, Integer> map){
		/* Calculate the total number of instances. */
		int count = 0;
		for(String id : map.keySet()){
			count += map.get(id);
		}
		
		double q = 0;
		for(String k : map.keySet()){
			q += ((double)map.get(k)/count)*Integer.parseInt(k.toString())*(Integer.parseInt(k.toString()) - 2);
		}
		return q;
	}
	
	
	private static void printDegreeDistribution(String filename, Map<String, Integer> map) {
		/* Add each degree-value to a list for sorting. */
		List<Integer> c = new ArrayList<Integer>();
		for(String id : map.keySet()){
			c.add(Integer.parseInt(id.toString()));
		}
		Collections.sort(c);
		
		/* Calculate the total number of instances. */
		int count = 0;
		for(String id : map.keySet()){
			count += map.get(id);
		}
		
		/* Write to file. */
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		double sum = 0;
		try{
			bw.write("Degree,Freq,DegreeDist,CumDist");
			bw.newLine();
//			for(int i = c.size()-1; i >= 0; i--){
			for(int i = 0; i < c.size(); i++){
				/* Write the order. */
				bw.write(String.valueOf(c.get(i)));
				bw.write(",");
				int freq = map.get(String.valueOf(c.get(i)));
				bw.write(String.valueOf(freq));
				bw.write(",");
				double degreeDist = (double)map.get(String.valueOf(c.get(i))) / (double)count;
				bw.write(String.valueOf(degreeDist));
				bw.write(",");
				sum += degreeDist;
				bw.write(String.valueOf(sum));
				bw.newLine();
			}
		} catch (IOException e) {
			throw new RuntimeException("Couldn't write to BufferedWriter for " + filename);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Couldn't close BufferedWriter for " + filename);
			}
		}
	}
	
	
	public MyGraphAnalyser() {

	}
	
	
	private static int getInDegree(DigicoreNetwork network, Id<ActivityFacility> node, boolean weighted){
		if(!weighted){
			return network.getInEdges(node).size();
		} else{
			int inDegree = 0;
			for(Pair<Id<ActivityFacility>> arc : network.getInEdges(node)){
				inDegree += network.getEdgeWeight(arc.getFirst(), arc.getSecond());
			}
			return inDegree;
		}
	}

	
	private static int getOutDegree(DigicoreNetwork network, Id<ActivityFacility> node, boolean weighted){
		if(!weighted){
			return network.getOutEdges(node).size();
		} else{
			int outDegree = 0;
			for(Pair<Id<ActivityFacility>> arc : network.getOutEdges(node)){
				outDegree += network.getEdgeWeight(arc.getFirst(), arc.getSecond());
			}
			return outDegree;
		}
	}

}

