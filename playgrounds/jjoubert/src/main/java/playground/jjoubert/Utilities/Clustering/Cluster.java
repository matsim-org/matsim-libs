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

package playground.jjoubert.Utilities.Clustering;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * A container class for clusters. Activity locations are grouped into clusters, each
 * believed to represent a significant point of activity. Each cluster has a unique
 * identity; contains a list of points, each associated with an activity; and has a
 * center of gravity. 
 * 
 * @author jwjoubert
 */
public class Cluster implements Comparable<Cluster>{
	private String clusterId;
	private List<ClusterPoint> points;
	private Point centerOfGravity;
	private final static Logger log = Logger.getLogger(Cluster.class);
			
	public Cluster(String clusterId){
		this.clusterId = clusterId;
		this.points = new ArrayList<ClusterPoint>();
	}
	
	/**
	 * Calculates the center of gravity with each point weighed equally.
	 */
	public void setCenterOfGravity(){
		GeometryFactory gf = new GeometryFactory();
		if(points.size() > 0){
			double xTotal = 0;
			double yTotal = 0;
			for (ClusterPoint p : points) {
				xTotal += p.getPoint().getX();
				yTotal += p.getPoint().getY();
			}			
			double xCenter = xTotal / (double) points.size();
			double yCenter = yTotal / (double) points.size();
			
			centerOfGravity = gf.createPoint(new Coordinate(xCenter, yCenter));			
		} else{
			log.warn("Not enough points in cluster " + clusterId + " to calculate a center of gravity!");
		}
	}

	public Point getCenterOfGravity() {
		return centerOfGravity;
	}

	public String getClusterId() {
		return clusterId;
	}
	
	public void setClusterId(String id){
		this.clusterId = id;
	}
	
	public List<ClusterPoint> getPoints() {
		return points;
	}

	public int compareTo(Cluster o) {
		int result = Integer.parseInt(this.clusterId) - Integer.parseInt(o.clusterId);
		return result;
	}
	
}
