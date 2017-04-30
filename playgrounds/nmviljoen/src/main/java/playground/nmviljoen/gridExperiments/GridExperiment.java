/* *********************************************************************** *
 * project: org.matsim.*
 * GridExperiment.java
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
package playground.nmviljoen.gridExperiments;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.uci.ics.jung.graph.DirectedGraph;

/**
 * A container for the multilayer network experiments. This is mainly for Nadia
 * Viljoen's PhD work, but may be useful elsewhere too. The container represents
 * a specific problem instance, which contains a specific physical network, a 
 * logical network, an archetype describing the logical network, and an instance
 * number. The experiment also keeps track of the associations between the 
 * physical and logical network, and the shortest path sets between the logical
 * nodes.
 * 
 * @author jwjoubert
 */
public class GridExperiment {
	final private Logger log = Logger.getLogger(GridExperiment.class);
	private Archetype archetype = null;
	private int instanceNumber = 0;
	private DirectedGraph<NmvNode, NmvLink> physicalNetwork = null;
	private DirectedGraph<NmvNode, NmvLink> logicalNetwork = null;
	Map<String, String> logicalToPhysicalMap = new TreeMap<>();
	Map<String, String> physicalToLogicalMap = new TreeMap<>();
	Map<String, Map<String, Set<List<String>>>> shortestPathSets = new TreeMap<>();
	
	public GridExperiment() {
	}
	
	public void setArchetype(Archetype type){
		this.archetype = type;
	}
	
	public Archetype getArchetype(){
		if(this.archetype == null){
			log.error("Experiment does not have an archetype set. Returning null!");
		}
		return this.archetype;
	}
	
	public int getInstanceNumber() {
		if(this.instanceNumber == 0){
			log.error("Experiment does not have an instance number set. Returning zero!!");
		}
		return instanceNumber;
	}
	
	public void setInstanceNumber(int instanceNumber) {
		this.instanceNumber = instanceNumber;
	}
	
	public void setPhysicalNetwork(DirectedGraph<NmvNode, NmvLink> network){
		this.physicalNetwork = network;
	}
	
	public DirectedGraph<NmvNode, NmvLink> getPhysicalNetwork(){
		return this.physicalNetwork;
	}
	
	public void setLogicalNetwork(DirectedGraph<NmvNode, NmvLink> network){
		this.logicalNetwork = network;
	}
	
	public DirectedGraph<NmvNode, NmvLink> getLogicalNetwork(){
		return this.logicalNetwork;
	}
	
	public void addAssociation(String logicalId, String physicalId){
		if(logicalToPhysicalMap.containsKey(logicalId)){
			throw new RuntimeException("Logical Id '" + logicalId + 
					" is already associated with " + logicalToPhysicalMap.get(logicalId) + 
					". Cannot associate it with physical node '" + physicalId + ".");
		}
		logicalToPhysicalMap.put(logicalId, physicalId);
		physicalToLogicalMap.put(physicalId, logicalId);
	}
	
	public String getLogicalNodeFromPhysical(String physicalId){
		if(!physicalToLogicalMap.containsKey(physicalId)){
			log.warn("There is no logical node associated with physical node '" 
					+ physicalId + ". Returning NULL.");
		}
		return physicalToLogicalMap.get(physicalId);
	}
	
	public String getPhysicalNodeFromLogical(String logicalId){
		if(!logicalToPhysicalMap.containsKey(logicalId)){
			throw new RuntimeException("There is no physical node associated with logical node '" 
					+ logicalId + ".");
		}
		return logicalToPhysicalMap.get(logicalId);
	}
	
	public Map<String, String> getLogicalToPhysicalAssociationMap(){
		return this.logicalToPhysicalMap;
	}
	
	public void addShortestPath(String fromId, String toId, List<String> path){
		if(!shortestPathSets.containsKey(fromId)){
			shortestPathSets.put(fromId, new TreeMap<>());
		}
		if(!shortestPathSets.get(fromId).containsKey(toId)){
			shortestPathSets.get(fromId).put(toId, new HashSet<>());
		}
		shortestPathSets.get(fromId).get(toId).add(path);
	}
	
	public Map<String, Map<String, Set<List<String>>>> getShortestPathSets(){
		return shortestPathSets;
	}
	
	/**
	 * A descriptive characterisation of the logical network configuration.
	 *  
	 * @author jwjoubert
	 */
	public enum Archetype{
		RESSLER("Ressler", "Single hub logical network", "sh"),
		MALIK("Malik", "Double hub logical network", "dh"),
		NAVABI("Navabi", "Fully connected logical network", "fc");
		
		private String shortName;
		private String description;
		private String acronym;
		
		private Archetype(String shortName, String longName, String acronym) {
			this.shortName = shortName;
			this.description = longName;
			this.acronym = acronym;
		}
		
		public String getShortName(){
			return this.shortName;
		}
		
		public String getDescription(){
			return this.description;
		}
		
		public String getAcronym(){
			return this.acronym;
		}
		
		public static Archetype parseArchetypeFromDescription(String s){
			if(s.equalsIgnoreCase(Archetype.RESSLER.description)){
				return RESSLER;
			} else if(s.equalsIgnoreCase(Archetype.MALIK.description)){
				return MALIK;
			} else if(s.equalsIgnoreCase(Archetype.NAVABI.description)){
				return NAVABI;
			} else{
				throw new RuntimeException("Cannot parse an archetype from " + s);
			}
		}
		
		
	}
	
}
