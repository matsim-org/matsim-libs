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

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class DgCrossing {

	private Id id;
	private boolean signalized;
	private Map<Id, DgCrossingNode> nodes = new HashMap<Id, DgCrossingNode>();
	private Map<Id, DgStreet> lights = new HashMap<Id, DgStreet>();
	private Map<Id, DgProgram> programs = new HashMap<Id, DgProgram>();

	public DgCrossing(Id id) {
		this.id = id;
	}

	public Id getId() {
		return this.id;
	}

	public void addNode(DgCrossingNode crossingNode) {
		this.nodes.put(crossingNode.getId(), crossingNode);
	}
	
	public Map<Id, DgCrossingNode> getNodes(){
		return this.nodes;
	}
	
	public Map<Id, DgStreet> getLights(){
		return this.lights;
	}

	public void addLight(DgStreet light) {
		this.lights.put(light.getId(), light);
	}

	public void setSignalized(boolean signalized) {
		this.signalized = signalized;
	}
	
	public void addProgram(DgProgram p){
		this.programs.put(p.getId(), p);
	}
	
	public Map<Id, DgProgram> getPrograms(){
		return this.programs;
	}
}
