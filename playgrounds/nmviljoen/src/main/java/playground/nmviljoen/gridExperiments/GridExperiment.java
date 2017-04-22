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

import org.apache.log4j.Logger;

import edu.uci.ics.jung.graph.DirectedGraph;

/**
 *
 * @author jwjoubert
 */
public class GridExperiment {
	final private Logger log = Logger.getLogger(GridExperiment.class);
	private Archetype archetype = null;
	private int instanceNumber = 0;
	private DirectedGraph<NmvNode, NmvLink> physicalNetwork = null;
	
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
	
	
	public enum Archetype{
		MALIK("Malik", "Some longer description for Malik network"),
		NAVABI("Navabi", "Some longer description for Navabi network"),
		RESSLER("Ressler", "Some longer description for Ressler network");
		
		private String shortName;
		private String longName;
		
		private Archetype(String shortName, String longName) {
			this.shortName = shortName;
			this.longName = longName;
		}
		
		public String getShortName(){
			return this.shortName;
		}
	}
	
}
