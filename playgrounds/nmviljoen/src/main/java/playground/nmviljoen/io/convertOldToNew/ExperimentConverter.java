/* *********************************************************************** *
 * project: org.matsim.*
 * ExperimentConverter.java
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
package playground.nmviljoen.io.convertOldToNew;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import playground.nmviljoen.gridExperiments.GridExperiment;
import playground.nmviljoen.gridExperiments.GridExperiment.Archetype;
import playground.nmviljoen.gridExperiments.NmvLink;
import playground.nmviljoen.gridExperiments.NmvNode;
import playground.nmviljoen.io.MultilayerInstanceWriter;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * Class to convert the old experiment files to the new XML format.
 * 
 * @author jwjoubert
 */
public class ExperimentConverter {
	final private static Logger LOG = Logger.getLogger(ExperimentConverter.class);
	private static DirectedGraph<NmvNode, NmvLink> physicalNetwork;
	private static Map<String, Coord> nodeMap;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ExperimentConverter.class.toString(), args);
		
		String physicalNetworkNodes = args[0];
		String physicalNetworkLinks = args[1];
		String pathToInstances = args[2];
		pathToInstances += pathToInstances.endsWith("/") ? "" : "/";
		Archetype archetype = Archetype.valueOf(args[3]);
		String xmlFolder = args[4];
		xmlFolder += xmlFolder.endsWith("/") ? "" : "/";
		new File(xmlFolder).mkdirs();
		
		/* Build the physical network only once. */
		buildNodeMap(physicalNetworkNodes);
		addPhysicalEdges(physicalNetworkLinks);
		
		List<File> folders = FileUtils.sampleFiles(new File(pathToInstances), Integer.MAX_VALUE, null);
		Counter counter = new Counter("  instance # ");
		for(File folder : folders){
			if(folder.isDirectory()){
				String foldername = folder.getName();
				String[] sa = foldername.split("_");
				int instanceNumber = Integer.parseInt(sa[1]);
				
				String logicalNetwork = folder.getAbsolutePath() 
						+ (folder.getAbsolutePath().endsWith("/") ? "" : "/")
						+ "Base_" + String.valueOf(instanceNumber) + "/"
						+ archetype.getShortName() 
						+ "LinkList.csv";
				String associationList = folder.getAbsolutePath() 
						+ (folder.getAbsolutePath().endsWith("/") ? "" : "/")
						+ "Base_" + String.valueOf(instanceNumber) + "/"
						+ "assocList.csv";
				String segmentPaths = folder.getAbsolutePath() 
						+ (folder.getAbsolutePath().endsWith("/") ? "" : "/")
						+ "Base_" + String.valueOf(instanceNumber) + "/"
						+ "segmentPaths.csv";
				String xmlFile = String.format("%s%s_%03d.xml.gz", 
						xmlFolder, archetype.getAcronym(), instanceNumber);
				
				 DirectedGraph<NmvNode, NmvLink> thisLogical = parseLogicalNetwork(logicalNetwork);
				 
				 GridExperiment experiment = new GridExperiment();
				 experiment.setArchetype(archetype);
				 experiment.setInstanceNumber(instanceNumber);
				 experiment.setPhysicalNetwork(physicalNetwork);
				 experiment.setLogicalNetwork(thisLogical);
				 
				 parseAssociationList(associationList, experiment);
				 parseShortestPathSets(segmentPaths, experiment);
				 
				 new MultilayerInstanceWriter(experiment).write(xmlFile);
				 counter.incCounter();
			}
		}
		counter.printCounter();
		Header.printFooter();
	}
	
	private static void buildNodeMap(String physicalNodeFile){
		LOG.info("Reading the physical network nodes...");
		/* Build the physical network */
		physicalNetwork = new DirectedSparseMultigraph<>();
		nodeMap = new TreeMap<String, Coord>();
		BufferedReader brNodes = IOUtils.getBufferedReader(physicalNodeFile);
		try{
			String line = brNodes.readLine(); /* Header. */
			while((line = brNodes.readLine()) != null){
				String[] sa = line.split(",");
				String id = sa[0];
				double x = Double.parseDouble(sa[1]);
				double y = Double.parseDouble(sa[2]);
				Coord c = CoordUtils.createCoord(x, y);
				if(nodeMap.containsKey(id)){
					LOG.error("Duplicate node: " + line);
				} else{
					nodeMap.put(id, c);
					physicalNetwork.addVertex(new NmvNode(null, id, x, y));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(physicalNodeFile);
		} finally{
			try {
				brNodes.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(physicalNodeFile);
			}
		}
		LOG.info("Total number of nodes in map: " + nodeMap.size());
		LOG.info("Total number of nodes in physical network: " + physicalNetwork.getVertexCount());
	}
	
	
	private static void addPhysicalEdges(String physicalEdgeFile){
		LOG.info("Reading the physical network links...");
		BufferedReader brEdges = IOUtils.getBufferedReader(physicalEdgeFile);
		try{
			String line = brEdges.readLine(); /* Header. */
			while((line = brEdges.readLine()) != null){
				String[] sa = line.split(",");
				String fromId = sa[0];
				String toId = sa[1];
				double weight = Double.parseDouble(sa[2]);
				
				NmvNode fromNode = getNode(fromId, physicalNetwork);
				NmvNode toNode = getNode(toId, physicalNetwork);
				NmvLink link = new NmvLink(fromId + "_" + toId, weight);
				
				physicalNetwork.addEdge(link, fromNode, toNode, EdgeType.DIRECTED);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(physicalEdgeFile);
		} finally{
			try {
				brEdges.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(physicalEdgeFile);
			}
		}
		LOG.info("Total number of edges in physical network: " + physicalNetwork.getEdgeCount());
		
	}
	
	private static NmvNode getNode(String id, DirectedGraph<NmvNode, NmvLink> network){
		NmvNode node = null;
		Iterator<NmvNode> iterator = network.getVertices().iterator();
		boolean found = false;
		while(!found && iterator.hasNext()){
			NmvNode thisNode = iterator.next();
			if(thisNode.getId().equalsIgnoreCase(id)){
				found = true;
				node = thisNode;
			}
		}
		if(node == null){
			LOG.warn("Cannot find node '" + id + "', returning NULL. Creating and adding it to the network.");
		}
		
		return node;
	}
	
	private static DirectedGraph<NmvNode, NmvLink> parseLogicalNetwork(String filename){
		LOG.info("Parse logical network from " + filename + "...");
		DirectedGraph<NmvNode, NmvLink> network = new DirectedSparseMultigraph<>();
		
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = br.readLine(); /* Header. */
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				String fromId = sa[0];
				String toId = sa[1];
				double weight = Double.parseDouble(sa[2]);
				
				NmvNode fromNode = getNode(fromId, network);
				if(fromNode == null){
					fromNode = new NmvNode(fromId);
					network.addVertex(fromNode);
				}
				NmvNode toNode = getNode(toId, network);
				if(toNode == null){
					toNode = new NmvNode(toId);
					network.addVertex(toNode);
				}
				NmvLink link = new NmvLink(fromId + "_" + toId, weight);
				
				network.addEdge(link, fromNode, toNode, EdgeType.DIRECTED);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(filename);
			}
		}
		
		return network;
	}
	
	private static void parseAssociationList(String filename, GridExperiment experiment){
		LOG.info("Parse association list...");
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = br.readLine(); /* Header. */
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				String logicalId = sa[0];
				String physicalId = sa[2];
				experiment.addAssociation(logicalId, physicalId);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(filename);
			}
		}
	}
	
	@SuppressWarnings("unused")
	private static void parseShortestPathSets(String filename, GridExperiment experiment){
		LOG.info("Parse shortest path sets...");
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = br.readLine(); /* Header. */
			while((line = br.readLine()) != null){
				String[] sa = line.split(" ");
				String lfromId = sa[0];
				String ltoId = sa[1];
				String pFromId = sa[2]; /* Unused */
				String pToId = sa[3];   /* Unused */
				int numberOfNodes = Integer.parseInt(sa[4]);

				List<String> pathList = new ArrayList<String>(19);
				for(int i = 5; i < 5+numberOfNodes; i++){
					pathList.add(sa[i]);
				}
				
				experiment.addShortestPath(lfromId, ltoId, pathList);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(filename);
			}
		}
		
	}
	

}
