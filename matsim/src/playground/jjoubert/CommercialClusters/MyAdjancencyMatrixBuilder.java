/* *********************************************************************** *
 * project: org.matsim.*
 * MyAdjancencyMatrixBuilder.java
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
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;

import playground.jjoubert.CommercialTraffic.Activity;
import playground.jjoubert.CommercialTraffic.Chain;
import playground.jjoubert.CommercialTraffic.Vehicle;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class MyAdjancencyMatrixBuilder {
	private final static Logger log = Logger.getLogger(MyAdjancencyMatrixBuilder.class);
	private final int matrixDimension;
	private DenseDoubleMatrix2D distanceAdjacency;
	private DenseDoubleMatrix1D inOrderAdjacency;
	private DenseDoubleMatrix1D outOrderAdjacency;
	private DenseDoubleMatrix2D orderAdjacency;

	private QuadTree<ClusterPoint> clusteredPoints;
	
	public MyAdjancencyMatrixBuilder(ArrayList<Cluster> clusterList){
		matrixDimension = clusterList.size();
		distanceAdjacency = new DenseDoubleMatrix2D(matrixDimension, matrixDimension);
		inOrderAdjacency = new DenseDoubleMatrix1D(matrixDimension);
		outOrderAdjacency = new DenseDoubleMatrix1D(matrixDimension);
		orderAdjacency = new DenseDoubleMatrix2D(matrixDimension, matrixDimension);
		clusteredPoints = buildQuadTree(clusterList);
	}
	
	
	public MyAdjancencyMatrixBuilder(ArrayList<Cluster> clusterList, QuadTree<ClusterPoint> clusteredPoints){
		matrixDimension = clusterList.size();
		distanceAdjacency = new DenseDoubleMatrix2D(matrixDimension, matrixDimension);
		inOrderAdjacency = new DenseDoubleMatrix1D(matrixDimension);
		outOrderAdjacency = new DenseDoubleMatrix1D(matrixDimension);
		orderAdjacency = new DenseDoubleMatrix2D(matrixDimension, matrixDimension);
		this.clusteredPoints = clusteredPoints;
	}

	
	private QuadTree<ClusterPoint> buildQuadTree(ArrayList<Cluster> clusterList) {
		log.info("Building QuadTree.");
		double xMin = Double.MAX_VALUE;
		double yMin = Double.MAX_VALUE;
		double xMax = Double.MIN_VALUE;
		double yMax = Double.MIN_VALUE;

		int inputPointCounter = 0;
		for(Cluster c : clusterList){
			for(ClusterPoint p : c.getPoints()){
				xMin = Math.min(xMin, p.getPoint().getX());
				yMin = Math.min(yMin, p.getPoint().getY());
				xMax = Math.max(xMax, p.getPoint().getX());
				yMax = Math.max(yMax, p.getPoint().getY());			
			}
		}
		QuadTree<ClusterPoint> qt = new QuadTree<ClusterPoint>(xMin, yMin, xMax, yMax);
		for(Cluster c : clusterList){
			for(ClusterPoint p : c.getPoints()){
				qt.put(p.getPoint().getX(), p.getPoint().getY(), p);
				inputPointCounter++;
			}
		}
		log.info("QuadTree built. Input points: " + String.valueOf(inputPointCounter) + "; Output points: " + qt.size() + " (with .size()); " + qt.values().size() + " (with .values.size())");
		return qt;
	}


	public void buildAdjacency(Vehicle vehicle){
		GeometryFactory gf = new GeometryFactory();
		log.info("Building adjacency for vehicle #" + String.valueOf(vehicle.getVehID()) + " with " + String.valueOf(vehicle.getChains().size()) + " chains.");
		int chainCounter = 0;
		int chainMultiplier = 1;
		
		for(Chain chain : vehicle.getChains() ){
			ArrayList<Activity> al = chain.getActivities();
			if(al.size() > 3){
				for(int a = 1; a < al.size() - 2; a++){
					Point p1 = gf.createPoint(al.get(a).getLocation().getCoordinate());
					Point p2 = gf.createPoint(al.get(a+1).getLocation().getCoordinate());
					ArrayList<Cluster> link = testValidLink(p1,p2);
					if(link != null){
						int row = Integer.parseInt(link.get(0).getClusterId());
						int col = Integer.parseInt(link.get(1).getClusterId());
						orderAdjacency.setQuick(row, col, orderAdjacency.getQuick(row, col) + 1);
						if(distanceAdjacency.getQuick(row, col) == 0){
							distanceAdjacency.setQuick(row, col, link.get(0).getCenterOfGravity().distance(link.get(1).getCenterOfGravity()));
						}
					}
				}
			}
			chainCounter++;
			// Report progress
			if(chainCounter == chainMultiplier){
				log.info("   Chains: " + chainCounter);
				chainMultiplier *= 2;
			}
		}
		log.info("   Chains: " + chainCounter + " (Done)");
	}


	private ArrayList<Cluster> testValidLink(Point p1, Point p2) {
		ArrayList<Cluster> result = null;
		Collection<ClusterPoint> c1List = clusteredPoints.get(p1.getX(), p1.getY(), 0.0);
		Collection<ClusterPoint> c2List = clusteredPoints.get(p2.getX(), p2.getY(), 0.0);
		Cluster c1 = null;
		Cluster c2 = null;
		if(c1List.size() > 0){
			c1 = ((ArrayList<ClusterPoint>) c1List).get(0).getCluster();
			int pos = Integer.parseInt(c1.getClusterId());
			inOrderAdjacency.setQuick(pos, inOrderAdjacency.getQuick(pos)+1);
		}
		if(c2List.size() > 0){
			c2 = ((ArrayList<ClusterPoint>) c2List).get(0).getCluster();
			int pos = Integer.parseInt(c2.getClusterId());
			outOrderAdjacency.setQuick(pos, outOrderAdjacency.getQuick(pos)+1);			
		}
		if(c1 != null && c2 != null){
			result = new ArrayList<Cluster>(2);
			result.add(c1);
			result.add(c2);
		}		
		return result;
	}

	public DenseDoubleMatrix2D getDistanceAdjacency() {
		return distanceAdjacency;
	}

	public DenseDoubleMatrix2D getOrderAdjacency() {
		return orderAdjacency;
	}


}
