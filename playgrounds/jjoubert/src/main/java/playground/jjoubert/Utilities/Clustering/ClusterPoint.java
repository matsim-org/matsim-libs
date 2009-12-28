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

package playground.jjoubert.Utilities.Clustering;

import com.vividsolutions.jts.geom.Point;

/**
 * A simple class linking a point to a specific cluster.
 * 
 * @author jwjoubert
 */
public class ClusterPoint{
	private int pointId;
	private Point point;
	private Cluster cluster;
	
	public ClusterPoint(int pointId, Point point, Cluster cluster){
		this.pointId = pointId;
		this.point = point;
		this.cluster = cluster;
	}

	public int getPointId() {
		return pointId;
	}

	public Point getPoint() {
		return point;
	}

	public Cluster getCluster() {
		return cluster;
	}
	
	public void setCluster(Cluster cluster){
		this.cluster = cluster;
	}
	

}
