/* *********************************************************************** *
 * project: org.matsim.*
 * OTFSignalSystem
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
package org.matsim.signals.vis;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dgrether
 *
 */
public class VisSignalSystem {
	
	private String id;
	private Map<String, VisSignalGroup> signalGroups = new HashMap<String, VisSignalGroup>();
	private Point2D.Float visCoordinate;

	public VisSignalSystem(String id){
		this.id = id;
	}

	public String getId() {
		return this.id;
	}

	public void addOTFSignalGroup(VisSignalGroup group){
		this.signalGroups.put(group.getId(), group);
	}
	
	public Map<String, VisSignalGroup> getOTFSignalGroups(){
		return this.signalGroups;
	}
	
	public void setVisCoordinate(Point2D.Float visCoordinate) {
		this.visCoordinate = visCoordinate;
	}
	
	public Point2D.Float getVisCoordinate(){
		return this.visCoordinate;
	}
	
}
