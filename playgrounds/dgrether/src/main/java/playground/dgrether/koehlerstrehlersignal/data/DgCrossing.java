/* *********************************************************************** *
 * project: org.matsim.*
 * DgCrossing
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.data;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;


/**
 * @author dgrether
 * @author tthunig
 *
 */
public class DgCrossing {

	private static final Logger log = Logger.getLogger(DgCrossing.class);
	
	private Id<DgCrossing> id;
	private Map<Id<DgCrossingNode>, DgCrossingNode> nodes = new HashMap<>();
	private Map<Id<DgStreet>, DgStreet> lights = new HashMap<>();
	private Map<Id<DgProgram>, DgProgram> programs = new HashMap<>();
	private String type;

	public DgCrossing(Id<DgCrossing> id) {
		this.id = id;
	}

	public Id<DgCrossing> getId() {
		return this.id;
	}

	public void addNode(DgCrossingNode crossingNode) {
		if (this.nodes.containsKey(crossingNode.getId())){
			log.warn("CrossingNode " + crossingNode.getId() +" already exists.");
		}
		this.nodes.put(crossingNode.getId(), crossingNode);
	}
	
	public Map<Id<DgCrossingNode>, DgCrossingNode> getNodes(){
		return this.nodes;
	}
	
	public Map<Id<DgStreet>, DgStreet> getLights(){
		return this.lights;
	}

	public void addLight(DgStreet light) {
		this.lights.put(light.getId(), light);
	}
	
	public void addProgram(DgProgram p){
		if (this.programs.containsKey(p.getId())){
			log.warn("Program " + p.getId() + " already exists!");
		}
		this.programs.put(p.getId(), p);
	}
	
	public Map<Id<DgProgram>, DgProgram> getPrograms(){
		return this.programs;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
