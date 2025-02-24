/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityCluster.java
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

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

/**
 * A container class for clusters. Activity locations are grouped into clusters, each
 * believed to represent a significant point of activity. Each cluster has a unique
 * identity; contains a list of points, each associated with an activity; and has a
 * centre of gravity.
 *
 * @author jwjoubert
 */
public class Cluster implements Identifiable<Cluster>{
	private final Logger log = LogManager.getLogger(Cluster.class);

	private Id<Cluster> clusterId;
	private Coord centerOfGravity;
	private final List<ClusterActivity> activities;
	private Geometry concaveHull;

	public Cluster(Id<Cluster> clusterId){
		this.clusterId = clusterId;
		this.activities = new ArrayList<>();
	}

	/**
	 * Calculates the center of gravity with each point weighed equally.
	 */
	public void setCenterOfGravity(){
		if(this.concaveHull == null){
			if(!activities.isEmpty()){
				double xTotal = 0;
				double yTotal = 0;
				for (ClusterActivity p : activities) {
					xTotal += p.getCoord().getX();
					yTotal += p.getCoord().getY();
				}
				double xCenter = xTotal / (double) activities.size();
				double yCenter = yTotal / (double) activities.size();

				centerOfGravity = new Coord(xCenter, yCenter);
			} else{
				throw new IllegalArgumentException("Not enough points in cluster " + clusterId + " to calculate a center of gravity!");
			}
		} else{
			/* Use the centroid of the hull.
			 * FIXME There is a chance that the hull is an empty geometry. */
			Point centroid = this.concaveHull.getCentroid();
			if(!centroid.isEmpty()){
				centerOfGravity = new Coord(centroid.getX(), centroid.getY());
			} else{
				log.warn("Cannot set centre of gravity for an empty point!!");
				log.debug("   --> Unique facility identifier: " + this.getId().toString());
				log.debug("   --> No centre of gravity set.");
			}
		}
	}

	public Coord getCenterOfGravity() {
		return centerOfGravity;
	}

	public void setClusterId(Id<Cluster> id){
		this.clusterId = id;
	}

	public List<ClusterActivity> getPoints() {
		return activities;
	}

	@Override
	public Id<Cluster> getId() {
		return this.clusterId;
	}

	@SuppressWarnings("unused")
	public void setConcaveHull(Geometry geometry){
		this.concaveHull = geometry;
	}

	@SuppressWarnings("unused")
	public Geometry getConcaveHull(Geometry geometry){
		return this.concaveHull;
	}
}
