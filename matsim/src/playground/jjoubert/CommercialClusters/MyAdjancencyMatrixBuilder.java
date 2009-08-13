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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;

import playground.jjoubert.CommercialTraffic.Activity;
import playground.jjoubert.CommercialTraffic.Chain;
import playground.jjoubert.Utilities.Clustering.Cluster;
import playground.jjoubert.Utilities.Clustering.ClusterPoint;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class MyAdjancencyMatrixBuilder {
	private final static Logger log = Logger.getLogger(MyAdjancencyMatrixBuilder.class);
	private final int matrixDimension;
	private SparseDoubleMatrix2D distanceAdjacency;
	private SparseDoubleMatrix1D inOrderAdjacency;
	private SparseDoubleMatrix1D outOrderAdjacency;
	private SparseDoubleMatrix2D orderAdjacency;
	private QuadTree<Double> distanceAdjacencyQT;
	private QuadTree<Integer> orderAdjacencyQT;
	private TreeMap<String,Integer> inOrderAdjacencyTM;
	private TreeMap<String,Integer> outOrderAdjacencyTM;

	private QuadTree<ClusterPoint> clusteredPoints;
	
	public MyAdjancencyMatrixBuilder(ArrayList<Cluster> clusterList){
		matrixDimension = clusterList.size();
		distanceAdjacency = new SparseDoubleMatrix2D(matrixDimension, matrixDimension);
		inOrderAdjacency = new SparseDoubleMatrix1D(matrixDimension);
		outOrderAdjacency = new SparseDoubleMatrix1D(matrixDimension);
		orderAdjacency = new SparseDoubleMatrix2D(matrixDimension, matrixDimension);
		distanceAdjacencyQT = new QuadTree<Double>(0,0,matrixDimension+1,matrixDimension+1);
		orderAdjacencyQT = new QuadTree<Integer>(0,0,matrixDimension+1,matrixDimension+1);
		inOrderAdjacencyTM = new TreeMap<String, Integer>();
		outOrderAdjacencyTM = new TreeMap<String, Integer>();
		clusteredPoints = buildQuadTree(clusterList);
	}
	
	
	public MyAdjancencyMatrixBuilder(ArrayList<Cluster> clusterList, QuadTree<ClusterPoint> clusteredPoints){
		matrixDimension = clusterList.size();
		distanceAdjacency = new SparseDoubleMatrix2D(matrixDimension, matrixDimension);
		inOrderAdjacency = new SparseDoubleMatrix1D(matrixDimension);
		outOrderAdjacency = new SparseDoubleMatrix1D(matrixDimension);
		orderAdjacency = new SparseDoubleMatrix2D(matrixDimension, matrixDimension);
		distanceAdjacencyQT = new QuadTree<Double>(0,0,matrixDimension+1,matrixDimension+1);
		orderAdjacencyQT = new QuadTree<Integer>(0,0,matrixDimension+1,matrixDimension+1);
		inOrderAdjacencyTM = new TreeMap<String, Integer>();
		outOrderAdjacencyTM = new TreeMap<String, Integer>();
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
		return qt;
	}


	public void buildAdjacency(ArrayList<Chain> chains){
		GeometryFactory gf = new GeometryFactory();
		log.info("Building adjacency for " + chains.size() + " chains.");
		int chainCounter = 0;
		int chainMultiplier = 1;
		
		for(Chain chain : chains ){
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

	public SparseDoubleMatrix2D getDistanceAdjacency() {
		return distanceAdjacency;
	}

	public SparseDoubleMatrix2D getOrderAdjacency() {
		return orderAdjacency;
	}
	
	public void writeAdjacenciesToFile( String distanceFilename, 
										String orderFilename, 
										String inOrderFilename,
										String outOrderFilename){
		log.info("Writing adjacencies and orders to file.");
		try {
			BufferedWriter outputDistance = new BufferedWriter(new FileWriter(new File(distanceFilename)));
			BufferedWriter outputOrder = new BufferedWriter(new FileWriter(new File(orderFilename)));
			BufferedWriter outputInOrder = new BufferedWriter(new FileWriter(new File(inOrderFilename)));
			BufferedWriter outputOutOrder = new BufferedWriter(new FileWriter(new File(outOrderFilename)));
			int rowMultiplier = 1;
			try{
				for (int row = 0; row < matrixDimension; row++) {
					/*
					 * Write the in- and out-order values.
					 */
					outputInOrder.write(String.valueOf(row));
					outputInOrder.write(",");
					outputInOrder.write(String.valueOf(inOrderAdjacency.getQuick(row)));
					outputInOrder.newLine();
					outputOutOrder.write(String.valueOf(row));
					outputOutOrder.write(",");
					outputOutOrder.write(String.valueOf(outOrderAdjacency.getQuick(row)));
					outputOutOrder.newLine();
					
					/*
					 * Write the distance- and order adjacency values.
					 */
					for (int col = 0; col < matrixDimension-1; col++){
						if(orderAdjacency.getQuick(row, col) > 0){
							outputDistance.write(String.valueOf(distanceAdjacency.getQuick(row, col)));
							outputOrder.write(String.valueOf(orderAdjacency.getQuick(row, col)));
						} else{
							outputDistance.write("NA");
							outputOrder.write("NA");
						}
						outputDistance.write(",");
						outputOrder.write(",");
					}
					if(distanceAdjacency.getQuick(row, matrixDimension-1) > 0){
						outputDistance.write(String.valueOf(distanceAdjacency.getQuick(row, matrixDimension-1)));
						outputDistance.write(String.valueOf(orderAdjacency.getQuick(row, matrixDimension-1)));
					}
					outputDistance.newLine();
					outputOrder.newLine();
					
					if(row+1 == rowMultiplier){
						log.info("   Rows processed: " + String.valueOf(row+1));
						rowMultiplier *= 2;
					}
				}
				log.info("   Rows processed: " + matrixDimension + " (Done)");
			} finally{
				outputInOrder.close();
				outputOutOrder.close();
				outputDistance.close();
				outputOrder.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public void writeAdjacencyAsPajekNetworkToFile(String outputFilename) {
		log.info("Writing order adjacency as Pajek network file.");
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(new File( outputFilename)));
			try{
				output.write("*Vertices ");
				output.write(String.valueOf(orderAdjacency.rows()));
				output.newLine();
				output.write("*Arcs");
				output.newLine();
				int rowMultiplier = 0;
				for(int r = 0; r < orderAdjacency.rows(); r++){
					for(int c = 0; c < orderAdjacency.columns(); c++){
						if(orderAdjacency.getQuick(r, c) > 0){
							output.write(String.valueOf(r));
							output.write(" ");
							output.write(String.valueOf(c));
							output.write(" ");
							output.write(String.valueOf(orderAdjacency.getQuick(r, c)));
							output.newLine();
						}
					}
					if(rowMultiplier == r+1){
						log.info("   Rows processed: " + String.valueOf(r+1));
						rowMultiplier *= 2;
					}
				}
				log.info("   Rows processed: " + orderAdjacency.rows() + " (Done)");
			} finally{
				output.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
