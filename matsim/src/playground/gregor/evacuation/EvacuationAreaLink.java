/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationAreaLink.java
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

package playground.gregor.evacuation;

import org.matsim.basic.v01.Id;

/**
 * @author glaemmel
 *
 */

// simple evacuation link description, according to the 
// desasterarea xml file
public class EvacuationAreaLink {
	private Id id;
	private double deadline;
	
	public EvacuationAreaLink(Id id, double deadline){
		this.id = id;
		this.deadline = deadline;
	}
	public EvacuationAreaLink(String id, double deadline){
		this.id = new Id(id);
		this.deadline = deadline;
	}
	
	
	public Id getId(){ return this.id; }
	public double getDeadline() { return this.deadline; }
	
	public void setId(Id id){ this.id = id; }
	public void setDeadline(double deadline) { this.deadline = deadline; }
	
}
