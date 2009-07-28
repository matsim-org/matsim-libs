/* *********************************************************************** *
 * project: org.matsim.*
 * DJCluster.java
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

import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;

import com.vividsolutions.jts.geom.Point;

public class DJCluster {
	private QuadTree<Point> inputPoints;
	private QuadTree<Point> clusteredPoints;
	private float radius;
	private int minimumPoints;
	private final static Logger log = Logger.getLogger(DJCluster.class);

	public DJCluster(float radius, int minimumPoints, QuadTree<Point> points){
		this.radius = radius;
		this.minimumPoints = minimumPoints;
		this.inputPoints = points;
		// Create the clustered point QuadTree with the same extent as the input point QuadTree. 
		this.clusteredPoints = new QuadTree<Point>(points.getMinEasting(),
													points.getMinNorthing(),
													points.getMaxEasting(),
													points.getMaxNorthing());
	}
	
	public void clusterInput(){
		log.info("Clustering input points. This may take a while.");
		int clusterIndex = 0;
		int pointCounter = 0;
		int pointMultiplier = 1;
		int noisePointCounter = 0;
		for (Point point : inputPoints.values()) {
			// Report progress
			if(pointCounter%pointMultiplier == 0){
				log.info("   Points clustered: " + String.valueOf(pointCounter));
				pointMultiplier *= 2;
			}

			// Compute the density-based neighbourhood N(p) of p
			Collection<Point> neighbourhood = inputPoints.get(point.getX(), point.getY(), radius);
			if(neighbourhood.size() < minimumPoints){
				// If N(p) is null, consider the point as noise (increment noisePointCounter)
				noisePointCounter++;
			} else if(this.isDensityJoinable(neighbourhood)){
				// Add the point to the existing cluster
			} else{
				// TODO add the point as a new cluster
				ActivityCluster ac = new ActivityCluster(String.valueOf(clusterIndex));
				
				//TODO ... must probably rewrite to also have an object "ClusterPoint"
			}
			
			
			
			
			//TODO If NOT, 
			
			
			pointCounter++;
		}
		log.info("   Points clustered: " + String.valueOf(pointCounter));
		log.info("Clustering complete.");
	}
	
	private boolean isDensityJoinable(Collection<Point> neighbourhood){
		// TODO this must be changed to return the CLUSTER, not just a boolean.
		boolean result = false;
			for (Point point : neighbourhood) {
			Collection<Point> clusterLocation = clusteredPoints.get(point.getX(), point.getY(), 0.0);
			if(clusterLocation.contains(point)){
				result = true;
				break;
			}
		}
		return result;
	}
	
	public QuadTree<Point> getInputPoints() {
		return inputPoints;
	}
	
	public QuadTree<Point> getClusteredPoints() {
		return clusteredPoints;
	}
	
	public float getRadius() {
		return radius;
	}
	
	public int getMinimumPoints() {
		return minimumPoints;
	}
	

}
