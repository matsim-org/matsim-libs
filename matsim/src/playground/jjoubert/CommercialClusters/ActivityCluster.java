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

package playground.jjoubert.CommercialClusters;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Point;

public class ActivityCluster {
	private String clusterId;
	private ArrayList<Point> points;
	
	public ActivityCluster(String clusterId){
		this.clusterId = clusterId;
		this.points = new ArrayList<Point>();
	}

	public String getClusterId() {
		return clusterId;
	}
	
	public ArrayList<Point> getPoints() {
		return points;
	}
	
}
