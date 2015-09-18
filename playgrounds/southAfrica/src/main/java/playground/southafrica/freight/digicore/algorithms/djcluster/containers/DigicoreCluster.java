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

package playground.southafrica.freight.digicore.algorithms.djcluster.containers;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * A container class for clusters. Activity locations are grouped into clusters, each
 * believed to represent a significant point of activity. Each cluster has a unique
 * identity; contains a list of points, each associated with an activity; and has a
 * center of gravity. 
 * 
 * @author jwjoubert
 */
public class DigicoreCluster implements Identifiable<DigicoreCluster>{
	private final Logger log = Logger.getLogger(DigicoreCluster.class);
	
	private Id<DigicoreCluster> clusterId;
	private Coord centerOfGravity;
	private List<ClusterActivity> activities;
	private Geometry concaveHull;
			
	public DigicoreCluster(Id<DigicoreCluster> clusterId){
		this.clusterId = clusterId;
		this.activities = new ArrayList<ClusterActivity>();
	}
	
	/**
	 * Calculates the center of gravity with each point weighed equally.
	 */
	public void setCenterOfGravity(){
		if(this.concaveHull == null){
			if(activities.size() > 0){
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

	public void setClusterId(Id<DigicoreCluster> id){
		this.clusterId = id;
	}
	
	public List<ClusterActivity> getPoints() {
		return activities;
	}

	@Override
	public Id<DigicoreCluster> getId() {
		return this.clusterId;
	}
	
	public void setConcaveHull(Geometry geometry){
		this.concaveHull = geometry;
	}
	
	public Geometry getConcaveHull(Geometry geometry){
		return this.concaveHull;
	}
}
