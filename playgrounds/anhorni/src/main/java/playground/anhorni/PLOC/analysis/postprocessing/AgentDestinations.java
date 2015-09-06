/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.PLOC.analysis.postprocessing;

import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;

public class AgentDestinations {
	
	private TreeMap<Integer, List<Coord>> destinations = new TreeMap<Integer, List<Coord>>();
	private Id agentId;
	
	public AgentDestinations(Id id) {
		this.agentId = id;
	}
	
	public void addDestination(int activityNumber, Coord coord) {
		if (this.destinations.get(activityNumber) == null) this.destinations.put(activityNumber, new Vector<CoordImpl>());
		this.destinations.get(activityNumber).add(coord);
	}
	
	public double getAverageDistanceFromCenterPointForAllActivities() {
		double distance = 0.0;
		for (Integer number : this.destinations.keySet()) {
			distance += this.getAverageDistanceFromCenterPoint(number);
		}
		return distance / this.destinations.keySet().size();
	}
	
	public double getAverageDistanceFromCenterPoint(int activityNumber) {
		Coord center = this.getCenterPoint(activityNumber);
		
		double distance = 0.0;
		for (Coord coord : this.destinations.get(activityNumber)) {
			distance += CoordUtils.calcDistance(center, coord);
		}
		distance /= this.destinations.get(activityNumber).size();
		return distance;
	}
	
	private Coord getCenterPoint(int activityNumber) {
		double x = 0.0;
		double y = 0.0;
		for (Coord coord : this.destinations.get(activityNumber)) {
			x += coord.getX();
			y += coord.getY();
		}
		x /= this.destinations.get(activityNumber).size();
		y /= this.destinations.get(activityNumber).size();
		return new Coord(x, y);
	}
}
