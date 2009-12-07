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
import java.util.List;

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

/**
 * This class builds adjacency matrices. An element <code>(i,j)</code> of the matrix 
 * refers to the entry in the <code>i</code>-th row and the <code>j</code>-th column in
 * the matrix. Each element <code>(i,j)</code> represents the directed link from the 
 * cluster <code>i</code> to cluster <code>j</code>. The following matrices are 
 * currently determined:   
 * <ul>
 *  <li><b><code>orderAdjacency</code>:</b> in the the order adjacency matrix we count 
 *  	the number of links that exist from cluster <code>i</code> to cluster 
 *  	<code>j</code>. A link in this this matrix context <i>exists</i> if and only if, 
 *  	for two consecutive activities <code>a</code> and <code>b</code> in a chain, 
 *  	activity <code>a</code> belongs to cluster <code>i</code> and activity <code>b
 *  	</code> belongs to cluster <code>j</code>.
 * 	<li><b><code>distanceAdjacency</code>:</b> the distance adjacency matrix indicates the 
 * 		distance from cluster <code>i</code> to cluster <code>j</code>, but only if the 
 * 		order adjacency of element <code>(i,j)</code> is greater than 0.
 * </ul>
 * @author jwjoubert
 */
public class MyAdjancencyMatrixBuilder {
	private final static Logger log = Logger.getLogger(MyAdjancencyMatrixBuilder.class);
	private final int matrixDimension;
	private SparseDoubleMatrix2D distanceAdjacency;
	private SparseDoubleMatrix1D inOrderAdjacency;
	private SparseDoubleMatrix1D outOrderAdjacency;
	private SparseDoubleMatrix2D orderAdjacency;
//	private QuadTree<Double> distanceAdjacencyQT;
//	private QuadTree<Integer> orderAdjacencyQT;
//	private TreeMap<String,Integer> inOrderAdjacencyTM;
//	private TreeMap<String,Integer> outOrderAdjacencyTM;

	private QuadTree<ClusterPoint> clusteredPoints;
	
	public MyAdjancencyMatrixBuilder(List<Cluster> clusterList){
		matrixDimension = clusterList.size();
		distanceAdjacency = new SparseDoubleMatrix2D(matrixDimension, matrixDimension);
		inOrderAdjacency = new SparseDoubleMatrix1D(matrixDimension);
		outOrderAdjacency = new SparseDoubleMatrix1D(matrixDimension);
		orderAdjacency = new SparseDoubleMatrix2D(matrixDimension, matrixDimension);
//		distanceAdjacencyQT = new QuadTree<Double>(0,0,matrixDimension+1,matrixDimension+1);
//		orderAdjacencyQT = new QuadTree<Integer>(0,0,matrixDimension+1,matrixDimension+1);
//		inOrderAdjacencyTM = new TreeMap<String, Integer>();
//		outOrderAdjacencyTM = new TreeMap<String, Integer>();
		clusteredPoints = buildQuadTree(clusterList);
	}
	
	
	public MyAdjancencyMatrixBuilder(List<Cluster> clusterList, QuadTree<ClusterPoint> clusteredPoints){
		matrixDimension = clusterList.size();
		distanceAdjacency = new SparseDoubleMatrix2D(matrixDimension, matrixDimension);
		inOrderAdjacency = new SparseDoubleMatrix1D(matrixDimension);
		outOrderAdjacency = new SparseDoubleMatrix1D(matrixDimension);
		orderAdjacency = new SparseDoubleMatrix2D(matrixDimension, matrixDimension);
//		distanceAdjacencyQT = new QuadTree<Double>(0,0,matrixDimension+1,matrixDimension+1);
//		orderAdjacencyQT = new QuadTree<Integer>(0,0,matrixDimension+1,matrixDimension+1);
//		inOrderAdjacencyTM = new TreeMap<String, Integer>();
//		outOrderAdjacencyTM = new TreeMap<String, Integer>();
		this.clusteredPoints = clusteredPoints;
	}

	
	private QuadTree<ClusterPoint> buildQuadTree(List<Cluster> clusterList) {
		log.info("Building QuadTree.");
		double xMin = Double.MAX_VALUE;
		double yMin = Double.MAX_VALUE;
		double xMax = Double.MIN_VALUE;
		double yMax = Double.MIN_VALUE;

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
			}
		}
		return qt;
	}


	public void buildAdjacency(List<Chain> chains, boolean silent){
		GeometryFactory gf = new GeometryFactory();
		if(!silent){
			log.info("Building adjacency for " + chains.size() + " chains.");
		}
		int chainCounter = 0;
		int chainMultiplier = 1;
		
		for(Chain chain : chains ){
			List<Activity> al = chain.getActivities();
//			if(al.size() >= 3){
//				for(int a = 1; a < al.size()-2; a++){ // Do not read the major activities at the ends of the chain. The `-2' is because I have an `a+1' within the loop.
			if(al.size() >= 2){
				for(int a = 0; a < al.size()-2; a++){ // Read all the activities. The `-2' is because I have an `a+1' within the loop.
					/*
					 * TODO Why NOT?! We will miss ALL the "full truck load" trips if a 
					 * vehicle only performs one activity per chain.
					 */
					Point p1 = gf.createPoint(al.get(a).getLocation().getCoordinate());
					Point p2 = gf.createPoint(al.get(a+1).getLocation().getCoordinate());
					List<Cluster> link = testValidLink(p1,p2);
					if(link != null){
						int row = Integer.parseInt(link.get(0).getClusterId());
						int col = Integer.parseInt(link.get(1).getClusterId());
						orderAdjacency.setQuick(row, col, orderAdjacency.getQuick(row, col) + 1);
						if(distanceAdjacency.getQuick(row, col) == 0){
							distanceAdjacency.setQuick(row, col, link.get(0).getCenterOfGravity().distance(link.get(1).getCenterOfGravity()));
						}
					}
				}
			} else{
				log.warn("Found a chain with only two activities: Vehicle " + chain.getActivities().get(0).getLocation().getVehID());
			}
				
			if(!silent){
				chainCounter++;
				// Report progress
				if(chainCounter == chainMultiplier){
					log.info("   Chains: " + chainCounter);
					chainMultiplier *= 2;
				}
			}
		}
		if(!silent){
			log.info("   Chains: " + chainCounter + " (Done)");
		}
	}


	private List<Cluster> testValidLink(Point p1, Point p2) {
		List<Cluster> result = null;
		Collection<ClusterPoint> c1List = clusteredPoints.get(p1.getX(), p1.getY(), 0.0);
		Collection<ClusterPoint> c2List = clusteredPoints.get(p2.getX(), p2.getY(), 0.0);
		Cluster c1 = null;
		Cluster c2 = null;
		if(c1List.size() > 0){
			c1 = ((List<ClusterPoint>) c1List).get(0).getCluster();
			int pos = Integer.parseInt(c1.getClusterId());
			// Increment the in-Order adjacency.
			inOrderAdjacency.setQuick(pos, inOrderAdjacency.getQuick(pos)+1);
		}
		if(c2List.size() > 0){
			c2 = ((List<ClusterPoint>) c2List).get(0).getCluster();
			int pos = Integer.parseInt(c2.getClusterId());
			// increment the out-Order adjacency.
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
						log.info("   Rows processed: " + (row+1));
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
	/**
	 * This method takes the cluster adjacency matrix and the cluster list, and writes a
	 * <A HREF="http://pajek.imfm.si/doku.php?id=pajek">Pajek</A> network file. An example 
	 * of the file format is given here. 
	 * <ul>
	 * 	<b>Format</b><ul><code>
	 * 					Vertices*<br>
	 * 					1 "1" 0.23553 0.12546<br>
	 * 					2 "2" 0.12346 0.87543<br>
	 * 					3 "3" 1.00000 0.26543<br>
	 * 					...<br>
	 * 					1434 "1434" 0.32432 0.54327<br>
	 * 					Arcs*<br>
	 * 					1 4 12<br>
	 * 					1 10 2<br>
	 * 					1 13 234<br>
	 * 					1 22 8<br>
	 * 					...
	 * 				</code></ul>
	 * </ul>
	 * 
	 * @param clusters the <code>List</code> of <code>Cluster</code>s so that the cluster
	 * 		centroid coordinates can be written as attributes of the vertices.
	 * @param pajekFilename the complete path of the <code>*.net</code> file to which
	 * 		the <i>Pajek</i> network is written. 
	 * @param rNetworkFilename the complete path of the <code>*.txt</code> file to which the
	 * 		<i>R</i> network is written. 
	 * @param rNodeFilename the complete path of the <code>*.txt</code> file to which the
	 * 		<i>R</i> node coordinates are written. 
	 */
	public void writeAdjacencyAsNetworkToFile(List<Cluster> clusters, String pajekFilename, String rNetworkFilename, String rNodeFilename) {
		log.info("Writing order adjacency as Pajek and R network files.");
		
		/*
		 * Pajek only handles the x- and y-coordinate in the [0,1] range. So, I need to
		 * find the extent of the envelope, and express the coordinates accordingly.
		 */
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (Cluster cluster : clusters) {
			minX = Math.min(minX, cluster.getCenterOfGravity().getX());
			minY = Math.min(minY, cluster.getCenterOfGravity().getY());
			maxX = Math.max(maxX, cluster.getCenterOfGravity().getX());
			maxY = Math.max(maxY, cluster.getCenterOfGravity().getY());
		}
		log.info("   Pajek coordinate envelope:");
		log.info(String.format("      Min x: %1.5f", minX));
		log.info(String.format("      Min y: %1.5f", minY));
		log.info(String.format("      Max x: %1.5f", maxX));
		log.info(String.format("      Max y: %1.5f", maxY));
		
		try {
			BufferedWriter outputPajek = new BufferedWriter(new FileWriter(new File( pajekFilename)));
			BufferedWriter outputRNetwork = new BufferedWriter(new FileWriter(new File( rNetworkFilename)));
			BufferedWriter outputRNode = new BufferedWriter(new FileWriter(new File( rNodeFilename)));
			try{
				outputPajek.write("*Vertices ");
				outputPajek.write(String.valueOf(orderAdjacency.rows()));
				outputPajek.newLine();
				// Write the vertex name and coordinates
				for(Cluster c : clusters){
					// Write to Pajek file
					outputPajek.write(String.valueOf(Integer.parseInt(c.getClusterId())+1));
					outputPajek.write(String.format(" \"%s\" ", String.valueOf(Integer.parseInt(c.getClusterId())+1)));
					outputPajek.write(String.format("%1.5f  ", (c.getCenterOfGravity().getX() - minX)/(maxX - minX)));
					outputPajek.write(String.format("%1.5f", (c.getCenterOfGravity().getY() - minY)/(maxY - minY)));
					outputPajek.newLine();
					
					// Write to R file
					outputRNode.write(String.valueOf(Integer.parseInt(c.getClusterId())+1));
					outputRNode.write(",");
					outputRNode.write(String.valueOf(c.getCenterOfGravity().getX()));
					outputRNode.write(",");
					outputRNode.write(String.valueOf(c.getCenterOfGravity().getY()));
					outputRNode.newLine();					
				}
				outputPajek.write("*Arcs");
				outputPajek.newLine();
				int rowMultiplier = 0;
				for(int r = 0; r < orderAdjacency.rows(); r++){
					for(int c = 0; c < orderAdjacency.columns(); c++){
						if(orderAdjacency.getQuick(r, c) > 0){
							// Write to Pajek file
							outputPajek.write(String.valueOf(r+1));
							outputPajek.write(" ");
							outputPajek.write(String.valueOf(c+1));
							outputPajek.write(" ");
							outputPajek.write(String.valueOf((int) orderAdjacency.getQuick(r, c)));
							outputPajek.newLine();
							
							// Write to R file
							outputRNetwork.write(String.valueOf(r+1));
							outputRNetwork.write(",");
							outputRNetwork.write(String.valueOf(c+1));
							outputRNetwork.write(",");
							outputRNetwork.write(String.valueOf((int) orderAdjacency.getQuick(r, c)));
							outputRNetwork.newLine();
						}
					}
					if(rowMultiplier == r+1){
						log.info("   Rows processed: " + (r+1));
						rowMultiplier *= 2;
					}
				}
				log.info("   Rows processed: " + orderAdjacency.rows() + " (Done)");
			} finally{
				outputPajek.close();
				outputRNetwork.close();
				outputRNode.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
