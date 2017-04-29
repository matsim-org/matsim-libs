/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreVehicleWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.nmviljoen.io;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.io.MatsimXmlWriter;

import edu.uci.ics.jung.graph.DirectedGraph;
import playground.nmviljoen.gridExperiments.GridExperiment;
import playground.nmviljoen.gridExperiments.NmvLink;
import playground.nmviljoen.gridExperiments.NmvNode;

public class MultilayerInstanceWriter extends MatsimXmlWriter implements MatsimWriter{
	private final Logger log = Logger.getLogger(MultilayerInstanceWriter.class);
	private GridExperiment experiment;

		
	public MultilayerInstanceWriter(GridExperiment experiment){
		super();
		this.experiment = experiment;
	}

	
	@Override
	public void write(final String filename){
		log.info("Writing grid experiment to file: " + filename);
		writeV1(filename);
	}
	
	
	public void writeV1(final String filename){
		String dtd = "http://matsim.org/files/dtd/multilayerNetwork_v1.dtd";
		MultilayerInstanceWriterHandler handler = new MultilayerInstanceWriterHandlerImpl_v1(experiment);
		
		openFile(filename);
		writeXmlHead();
		writeDoctype("multilayerNetwork", dtd);
			
		try {
			handler.startInstance(writer);
			
			/* Physical network */
			DirectedGraph<NmvNode, NmvLink> physicalNetwork = experiment.getPhysicalNetwork();
			handler.startPhysicalNetwork(writer);
			handler.startPhysicalNodes(writer);
			/* Write all physical nodes. */
				Map<Integer, NmvNode> sortedPhysicalNodes = sortNodes(physicalNetwork.getVertices());
				for(Integer i : sortedPhysicalNodes.keySet()){
					handler.startPhysicalNode(writer, sortedPhysicalNodes.get(i));
					handler.endPhysicalNode(writer);
				}
			handler.endPhysicalNodes(writer);
			handler.startPhysicalEdges(writer);
			/* Write all physical edges. */
				Map<Integer, Map<Integer, NmvLink>> sortedPhysicalLinks = sortLinks(physicalNetwork.getEdges());
				for(Integer i : sortedPhysicalLinks.keySet()){
					Map<Integer, NmvLink> thisMap = sortedPhysicalLinks.get(i);
					for(Integer j : thisMap.keySet()){
						NmvLink link = sortedPhysicalLinks.get(i).get(j);
						handler.startPhysicalEdge(writer, link);
						handler.endPhysicalEdge(writer);
					}
				}
			handler.endPhysicalEdges(writer);
			handler.endPhysicalNetwork(writer);
			
			/* Logical network */
			DirectedGraph<NmvNode, NmvLink> logicalNetwork = experiment.getLogicalNetwork();
			handler.startLogicalNetwork(writer);
			handler.startLogicalNodes(writer);
			/* Write all logical nodes. */
			Map<Integer, NmvNode> sortedLogicalNodes = sortNodes(logicalNetwork.getVertices());
			for(Integer i : sortedLogicalNodes.keySet()){
				handler.startLogicalNode(writer, sortedLogicalNodes.get(i));
				handler.endLogicalNode(writer);
			}
			handler.endLogicalNodes(writer);
			handler.startLogicalEdges(writer);
			/* Write all logical egdes. */
				Map<Integer, Map<Integer, NmvLink>> sortedLogicalLinks = sortLinks(logicalNetwork.getEdges());
				for(Integer i : sortedLogicalLinks.keySet()){
					Map<Integer, NmvLink> thisMap = sortedLogicalLinks.get(i);
					for(Integer j : thisMap.keySet()){
						NmvLink link = sortedLogicalLinks.get(i).get(j);
						handler.startLogicalEdge(writer, link);
						handler.endLogicalEdge(writer);
					}
				}
			handler.endLogicalEdges(writer);
			handler.endLogicalNetwork(writer);
			
			/* Association lists */
			Map<String, String> associationList = experiment.getLogicalToPhysicalAssociationMap();
			handler.startAssociations(writer);
			/* Write all associations. */
				Map<Integer, String> sortedAssociations = sortAssociationMap(associationList);
				for(int i : sortedAssociations.keySet()){
					String logicalId = String.valueOf(i);
					handler.startAssociation(writer, logicalId, associationList.get(logicalId));
					handler.endAssociation(writer);
				}
			handler.endAssociations(writer);
			
			/* Shortest path sets. */
			handler.startSets(writer);
			/* Write all sets */
				for(String fromId : experiment.getShortestPathSets().keySet()){
					for(String toId : experiment.getShortestPathSets().get(fromId).keySet()){
						handler.startSet(writer, fromId, toId);
						Set<List<String>> set = experiment.getShortestPathSets().get(fromId).get(toId);
						List<List<String>> sortedSet = sortPathSet(set);
						Iterator<List<String>> iterator = sortedSet.iterator();
						while(iterator.hasNext()){
							/* Write all paths */
							handler.startPath(writer, iterator.next());
							handler.endPath(writer);
						}
						handler.endSet(writer);
					}
				}
			handler.endSets(writer);
			
			handler.endInstance(writer);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write XML for multilayered network.");
		}finally{
			try {
				this.writer.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close XML writer.");
			}
		}
	}
	
	private Map<Integer, NmvNode> sortNodes(Collection<NmvNode> nodes){
		Map<Integer, NmvNode> sortedMap = new TreeMap<Integer, NmvNode>();
		Iterator<NmvNode> iterator = nodes.iterator();
		while(iterator.hasNext()){
			NmvNode node = iterator.next();
			int id = Integer.parseInt(node.getId());
			sortedMap.put(id, node);
		}
		return sortedMap;
	}
	
	private Map<Integer, String> sortAssociationMap(Map<String, String> map){
		Map<Integer, String> sortedMap = new TreeMap<>();
		for(String s : map.keySet()){
			sortedMap.put(Integer.parseInt(s), map.get(s));
		}
		return sortedMap;
	}
	
	
	private Map<Integer, Map<Integer, NmvLink>> sortLinks(Collection<NmvLink> links){
		Map<Integer, Map<Integer, NmvLink>> sortedLinks = new TreeMap<>();
		Iterator<NmvLink> iterator = links.iterator();
		while(iterator.hasNext()){
			NmvLink link = iterator.next();
			String[] sa = link.getId().split("_");
			Integer from = Integer.parseInt(sa[0]);
			Integer to = Integer.parseInt(sa[1]);
			if(!sortedLinks.containsKey(from)){
				Map<Integer, NmvLink> newMap = new TreeMap<>();
				sortedLinks.put(from, newMap);
			}
			sortedLinks.get(from).put(to, link);
		}
		return sortedLinks;
	}
	
	private List<List<String>> sortPathSet(Set<List<String>> set){
		List<List<String>> finalList = new ArrayList<>(set.size());
		List<String> sortedList = new ArrayList<String>(set.size());
		for(List<String> list : set){
			String s = list.get(0);
			for(int i = 1; i < list.size(); i++){
				s += " ";
				s += list.get(i);
			}
			sortedList.add(s);
		}
		Collections.sort(sortedList);
		
		for(String s : sortedList){
			String[] sa = s.split(" ");
			List<String> thisList = new ArrayList<>(sa.length);
			for(int i = 0; i < sa.length; i++){
				thisList.add(sa[i]);
			}
			finalList.add(thisList);
		}
		
		return finalList;
	}
	
	
}

