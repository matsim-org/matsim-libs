/* *********************************************************************** *
 * project: org.matsim.*
 * ClusterPoint.java
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

package org.matsim.core.network.algorithms.intersectionSimplifier.containers;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;


/**
 * A simple class linking a {@link DigicoreActivity} to a specific cluster.
 * 
 * @author jwjoubert
 */
public class ClusterActivity implements Identifiable<Coord>{
	private Id<Coord> activityId;
	private Coord coord;
	private Cluster cluster;
	
	public ClusterActivity(Id<Coord> activityId, Coord coord, Cluster cluster){
		this.activityId = activityId;
		this.coord = coord;
		this.cluster = cluster;
	}

	public Coord getCoord() {
		return this.coord;
	}

	public Cluster getCluster() {
		return cluster;
	}
	
	public void setCluster(Cluster cluster){
		this.cluster = cluster;
	}

	@Override
	public Id<Coord> getId() {
		return this.activityId;
	}
	

}
