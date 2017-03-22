/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreClusterExecuter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.southafrica.freight.digicore.algorithms.djcluster;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.algorithms.djcluster.containers.DigicoreCluster;


public class DigicoreClusterCallable implements Callable<List<DigicoreCluster>> {
	private List<Coord> pointList;
	private final double radius;
	private final int minimumPoints;
	private List<DigicoreCluster> clusterList = new ArrayList<DigicoreCluster>();
	private Counter counter;
	
	public DigicoreClusterCallable(List<Coord> pointList, double radius, int minPoints, Counter counter) {
		this.pointList = pointList;
		this.radius = radius;
		this.minimumPoints = minPoints;
		this.counter = counter;
	}

	
	public List<DigicoreCluster> getListOfClusters(){
		return this.clusterList;
	}


	@Override
	public List<DigicoreCluster> call() throws Exception {
		if(this.pointList.size() > 0){
			DJCluster djc = new DJCluster(this.pointList, true);
			djc.clusterInput(this.radius, this.minimumPoints);
			this.clusterList = djc.getClusterList();			
		}
		
		counter.incCounter();
		this.pointList = null;

		return this.clusterList;
	}

}

