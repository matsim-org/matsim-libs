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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * <par>This class implements the DJ-Cluster density-based clustering approach suggested by
 * Zhou <i>et al</i> (2004).</par>
 * <ul>
 * 		<i>``The basic idea of DJ-Cluster is as follows. For each point, calculate its 
 * 		<b>neighborhood</b>: the neighborhood consists of points within distance 
 * 		<b>Eps</b>, under condition that there are at least <b>MinPts</b> of them. If no
 * 		such neighborhood is found, the point is labeled noise; otherwise, the points are
 * 		created as a new cluster if no neighbor is in an existing cluster, or joined with
 * 		an existing cluster if any neighbour is in an existing cluster.''</i>
 * </ul>
 * 
 * @author jwjoubert
 */
public class DJCluster {
	private QuadTree<Point> inputPoints;
	private QuadTree<ClusterPoint> clusteredPoints;
	private ArrayList<Cluster> clusterList;
	private float radius;
	private int minimumPoints;
	private final static Logger log = Logger.getLogger(DJCluster.class);
	private String delimiter = ",";

	/**
	 * Creates a new instance of the DJ-Cluster with an empty list of clusters.
	 * @param radius the radius of the search circle within which other activity points
	 * 			are searched.
	 * @param minimumPoints the minimum number of points considered to constitute an
	 * 			independent cluster.
	 * @param points the <code>QuadTree</code> of <code>Point</code>s that were generated
	 * 			from the activity locations file. 
	 */
	public DJCluster(float radius, int minimumPoints, QuadTree<Point> points){
		this.radius = radius;
		this.minimumPoints = minimumPoints;
		this.inputPoints = points;
		// Create the clustered point QuadTree with the same extent as the input point QuadTree. 
		this.clusteredPoints = new QuadTree<ClusterPoint>(points.getMinEasting(),
													points.getMinNorthing(),
													points.getMaxEasting(),
													points.getMaxNorthing());
		this.clusterList = new ArrayList<Cluster>();
	}
	
	/**
	 * Building an <code>ArrayList</code> of <code>Cluster</code>s. The DJ-Clustering
	 * procedure of Zhou <i>et al</i> (2004) is followed.
	 */
	public void clusterInput(){
		log.info("Clustering input points. This may take a while.");
		int clusterIndex = 0;
		int pointCounter = 0;
		int pointMultiplier = 1;
		
		QuadTree<Point> copyInputPoints = new QuadTree<Point>(inputPoints.getMinEasting(),
																inputPoints.getMinNorthing(),
																inputPoints.getMaxEasting(),
																inputPoints.getMaxNorthing());
		for (Point point : inputPoints.values()) {
			copyInputPoints.put(point.getX(), point.getY(), point);
		}
		
		Point point;
		double xPos = inputPoints.getMinEasting();
		double yPos = inputPoints.getMinNorthing();
		while(inputPoints.values().size() > 0){
			point = inputPoints.get(xPos, yPos);
			xPos = point.getX();
			yPos = point.getY();

			// Compute the density-based neighbourhood N(p) of p
			Collection<Point> neighbourhood = copyInputPoints.get(point.getX(), point.getY(), radius);
			Cluster djCluster = densityJoinable(neighbourhood, point);
			ArrayList<Cluster> djClusterList = getDensityJoinableClusters(neighbourhood, point);
			if(neighbourhood.size() < minimumPoints){
				// Consider the point to be noise.
				inputPoints.remove(point.getX(), point.getY(), point);
				pointCounter++;				
			} else if(djClusterList.size() > 0){
				// Merge all the clusters. 
				Map<Integer,Cluster> clusterMap = new HashMap<Integer, Cluster>();
				Integer smallestClusterIndex = new Integer(Integer.MAX_VALUE);
				for (Cluster cluster : djClusterList) {
					clusterMap.put(Integer.parseInt(cluster.getClusterId()), cluster);
					smallestClusterIndex = Math.min(smallestClusterIndex, Integer.parseInt(cluster.getClusterId()));
				}
				Cluster toCluster = clusterMap.get(smallestClusterIndex);
				for (Cluster cluster : djClusterList) {
					if(cluster == toCluster){
						// Do nothing.
					} else{
						// Add all points to toCluster.
						toCluster.getPoints().addAll(cluster.getPoints());
						// Update the cluster Ids of the points in the QuadTree
						for (Point p : cluster.getPoints()) {
							ArrayList<ClusterPoint> cpList = (ArrayList<ClusterPoint>) clusteredPoints.get(p.getX(), p.getY(), 0.0);
							for (ClusterPoint cp : cpList) {
								cp.setCluster(toCluster);
							}
						}
						clusterList.remove(cluster);
					}
				}
				// Add the points in the neighbourhood
				Collection<Point> unclusteredNeighbourhood = inputPoints.get(point.getX(), point.getY(), radius);
				for (Point p : unclusteredNeighbourhood) {
					// Add the point as a clustered point.
					ClusterPoint cp = new ClusterPoint(p, djCluster);
					clusteredPoints.put(p.getX(), p.getY(), cp);

					// Add the point to the cluster.
					djCluster.getPoints().add(p);

					// Remove the point from the input data.
					inputPoints.remove(p.getX(), p.getY(), p);

					pointCounter++;
				}
			} else{
				// Create a new cluster.
				Cluster newCluster = new Cluster(String.valueOf(clusterIndex));
				clusterList.add(newCluster);
				clusterIndex++;
				
				/* 
				 * For each point in the neighbourhood, create a new ClusterPoint, 
				 * and add it to the QuadTree.
				 */			
				for (Point p : neighbourhood) {
					// Add the point as a clustered point.
					ClusterPoint cp = new ClusterPoint(p, newCluster);
					clusteredPoints.put(p.getX(), p.getY(), cp);

					// Add the point to the cluster.
					newCluster.getPoints().add(p);

					// Remove the point from the input data.
					inputPoints.remove(p.getX(), p.getY(), p);

					pointCounter++;
				}
			}
			
//			if(djCluster == null){
//				if(neighbourhood.size() < minimumPoints){
//					// If N(p) is null, consider the point as noise (increment noisePointCounter)
//					noisePointCounter++;
//					inputPoints.remove(point.getX(), point.getY(), point);
//					pointCounter++;
//				} else{
//					// Add the points to a new cluster
//					Cluster newCluster = new Cluster(String.valueOf(clusterIndex));
//					clusterList.add(newCluster);
//					clusterIndex++;
//					
//					/* 
//					 * For each point in the neighbourhood, create a new ClusterPoint, 
//					 * and add it to the QuadTree.
//					 */			
//					for (Point p : neighbourhood) {
//						// Add the point as a clustered point.
//						ClusterPoint cp = new ClusterPoint(p, newCluster);
//						clusteredPoints.put(p.getX(), p.getY(), cp);
//
//						// Add the point to the cluster.
//						newCluster.getPoints().add(p);
//
//						// Remove the point from the input data.
//						inputPoints.remove(p.getX(), p.getY(), p);
//
//						pointCounter++;
//					}
//				}			
//			} else{
//				/* 
//				 * For each point in the neighbourhood, create a new ClusterPoint, 
//				 * and add it to the QuadTree.
//				 */
//				Collection<Point> unclusteredNeighbourhood = inputPoints.get(point.getX(), point.getY(), radius);
//				for (Point p : unclusteredNeighbourhood) {
//					/* 
//					 * Only the add the neighbourhood point if it does not ALREADY
//					 * exist in the QuadTree.
//					 */
//					if(inputPoints.get(p.getX(), p.getY(), 0).contains(p)){
//						// Add the point as a clustered point.
//						ClusterPoint cp = new ClusterPoint(p, djCluster);
//						clusteredPoints.put(p.getX(), p.getY(), cp);
//
//						// Add the point to the cluster.
//						djCluster.getPoints().add(p);
//
//						// Remove the point from the input data.
//						inputPoints.remove(p.getX(), p.getY(), p);
//
//						pointCounter++;
//					}
//				}
//			}

			// Report progress
			if(pointCounter >= pointMultiplier){
				log.info("   Points clustered: " + String.valueOf(pointCounter));
				pointMultiplier = (int) Math.max(pointCounter, pointMultiplier)*2;
			}
		}
		log.info("   Points clustered: " + String.valueOf(pointCounter) + " (Done)");
	}
	
	public void writeClustersToFile(String filename){
		int clusterCount = 0;
		int clusterMultiplier = 1;
		int totalClusters = clusterList.size();
		log.info("Writing a total of " + totalClusters + " to file.");
		
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(filename)));
			try{
				output.write("ClusterId,Long,Lat,NumberOfActivities");
				output.newLine();
				
				for (Cluster c : clusterList) {
					c.setCenterOfGravity();
					Point center = c.getCenterOfGravity();
					output.write(c.getClusterId());
					output.write(delimiter);
					output.write(String.valueOf(center.getX()));
					output.write(delimiter);
					output.write(String.valueOf(center.getY()));
					output.write(delimiter);
					output.write(String.valueOf(c.getPoints().size()));
					output.newLine();
					
					clusterCount++;
					// Report progress
					if(clusterCount == clusterMultiplier){
						log.info("   Clusters written: " + String.valueOf(clusterCount));
						clusterMultiplier *= 2;
					}
				}
				log.info("   Clusters written: " + String.valueOf(clusterCount) + " (Done)" );
			} finally{
				output.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<Cluster> getClusterList() {
		return clusterList;
	}

	private Cluster densityJoinable(Collection<Point> neighbourhood, Point p){
		Cluster result = null;
		ArrayList<Point> theNeighbourhood = (ArrayList<Point>) neighbourhood;
		Map<Double,Point> map = new TreeMap<Double, Point>();
		
		ArrayList<Double> distanceList = new ArrayList<Double>(theNeighbourhood.size());
		for (Point p2 : neighbourhood) {
			Double distance = new Double(p2.distance(p));
			distanceList.add(distance);
			map.put(distance, p2);
		}
		Collections.sort(distanceList);
		
		int pointIndex = 0;
		Point point;
		while(result == null && pointIndex < distanceList.size()){
			point = map.get(distanceList.get(pointIndex));
			Collection<ClusterPoint> clusterLocation = clusteredPoints.get(point.getX(), point.getY(), 0.0);
			for (ClusterPoint cp : clusterLocation) {
				if(cp.getPoint().equals(point)){
					result = cp.getCluster();
					break;					
				}
			}
			pointIndex++;
		}
		return result;
	}
	
	private ArrayList<Cluster> getDensityJoinableClusters(Collection<Point> neighbourhood, Point p){
		ArrayList<Cluster> result = new ArrayList<Cluster>();
		for (Point np : neighbourhood) {
			Collection<ClusterPoint> cp = clusteredPoints.get(np.getX(), np.getY(), 0.0);
			for (ClusterPoint thisCp : cp) {
				if(thisCp.getPoint().equals(np)){
					if(!result.contains(thisCp.getCluster())){
						result.add(thisCp.getCluster());
					}
				}
			}
		}		
		return result;
	}
	
	public void visualizeClusters(	String pointFilename, 
									String clusterFilename, 
									String lineFilename,
									String polygonFilename){
		log.info("Processing clusters for visualization (" + String.valueOf(this.getClusterList().size()) + " clusters)");
		GeometryFactory gf = new GeometryFactory();
		int clusterCount = 0;
		int clusterMultiplier = 1;
		
		int pointId = 0;
		
		try {
			BufferedWriter output_Points = new BufferedWriter(new FileWriter(new File(pointFilename)));
			BufferedWriter output_Clusters = new BufferedWriter(new FileWriter(new File(clusterFilename)));
			BufferedWriter output_Lines = new BufferedWriter(new FileWriter(new File(lineFilename)));
			BufferedWriter output_Polygon = new BufferedWriter(new FileWriter(new File(polygonFilename)));
			try{
				// Write headers.
				output_Points.write("ID,X,Y,ClusterID");
				output_Points.newLine();
				output_Clusters.write("ID,X,Y,NumberOfPoints");
				output_Clusters.newLine();
				output_Lines.write("ID");
				output_Lines.newLine();
				output_Polygon.write("ID");
				output_Polygon.newLine();
				
				for (Cluster c : this.getClusterList()) {
					c.setCenterOfGravity();
					// Write the cluster.
					Point cog = c.getCenterOfGravity();
					output_Clusters.write(c.getClusterId());
					output_Clusters.write(",");
					output_Clusters.write(String.valueOf(cog.getX()));
					output_Clusters.write(",");
					output_Clusters.write(String.valueOf(cog.getY()));
					output_Clusters.write(",");
					output_Clusters.write(String.valueOf(c.getPoints().size()));
					output_Clusters.newLine();
					
					// Write the cluser's polygon
					ArrayList<Point> al = c.getPoints();
					Coordinate[] ca = new Coordinate[ al.size()+1 ];
					for(int i = 0; i < al.size(); i++){
						ca[i] = new Coordinate(al.get(i).getX(), al.get(i).getY());
					}
					ca[ca.length-1] = ca[0];
					
					Geometry g = gf.createMultiPoint(ca);
					Polygon convexHull = (Polygon) g.convexHull();
					Polygon convexHullBuffer = (Polygon) convexHull.buffer(2);
					
					if(c.getClusterId().equalsIgnoreCase("41")){						
						log.info("Cluster " + c.getClusterId());
					}
					//TODO Fix contraction
					Polygon nonConvexHull = contractPolygon(c, convexHull);
					Polygon nonConvexHullBuffer = (Polygon) nonConvexHull.buffer(2);
					
					output_Polygon.write(c.getClusterId());
					output_Polygon.newLine();
					Coordinate [] boundary = nonConvexHullBuffer.getCoordinates();
					for (Coordinate coordinate : boundary) {
						output_Polygon.write(String.valueOf(coordinate.x));
						output_Polygon.write(",");
						output_Polygon.write(String.valueOf(coordinate.y));
						output_Polygon.newLine();
					}
					output_Polygon.write("END");
					output_Polygon.newLine();
					
					for (Point p : c.getPoints()) {
						// Write the point
						output_Points.write(String.valueOf(pointId));
						output_Points.write(",");
						output_Points.write(String.valueOf(p.getX()));
						output_Points.write(",");
						output_Points.write(String.valueOf(p.getY()));
						output_Points.write(",");
						output_Points.write(c.getClusterId());
						output_Points.newLine();
						
						// Write the connecting line
						output_Lines.write(String.valueOf(pointId));
						output_Lines.newLine();
						output_Lines.write(String.valueOf(p.getX()));
						output_Lines.write(",");
						output_Lines.write(String.valueOf(p.getY()));
						output_Lines.newLine();
						output_Lines.write(String.valueOf(cog.getX()));
						output_Lines.write(",");
						output_Lines.write(String.valueOf(cog.getY()));
						output_Lines.newLine();
						output_Lines.write("END");
						output_Lines.newLine();
						
						pointId++;
					}
					
					clusterCount++;
					// Report progress.
					if(clusterCount == clusterMultiplier){
						log.info("   Clusters completed: " + String.valueOf(clusterCount));
						clusterMultiplier *= 2;
					}
				}
				log.info("   Clusters completed: " + String.valueOf(clusterCount) + " (Done)");
				
			} finally{
				output_Points.write("END");
				output_Points.close();
				output_Clusters.write("END");
				output_Clusters.close();
				output_Lines.write("END");
				output_Lines.close();
				output_Polygon.write("END");
				output_Polygon.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Polygon contractPolygon(Cluster c, Polygon convexHull) {
		GeometryFactory gf = new GeometryFactory();
		Polygon hull = (Polygon) convexHull.clone();
		Coordinate [] envelope = convexHull.getEnvelope().getCoordinates();
		
		// Build QuadTrees of internal and boundary points.
		QuadTree<Point> internal;
		
		internal = new QuadTree<Point>(envelope[0].x, envelope[0].y, envelope[2].x, envelope[2].y);
		for (Point point : c.getPoints()) {
			Coordinate [] ca = convexHull.getCoordinates();
			int index = 0;
			boolean onBoundary = false;
			while(!onBoundary && index < ca.length){
				Point boundaryPoint = gf.createPoint(ca[index]);
				if(boundaryPoint.distance(point) == 0){
					onBoundary = true;
				}
				index++;
			}
			if(!onBoundary){
				internal.put(point.getX(), point.getY(), point);
			}
		}
		int numberOfIterations = Math.min(100, (int) (((double) internal.size()) * 0.5));		

		for(int i = 0; i < numberOfIterations; i++){
			Coordinate [] hullCoordinates = hull.getCoordinates();
			LineString ls = findLongestSegmentIndex(hullCoordinates);	
			double lsx1 = Math.min(ls.getStartPoint().getX(), ls.getEndPoint().getX());
			double lsy1 = Math.min(ls.getStartPoint().getY(), ls.getEndPoint().getY());
			double lsx2 = Math.max(ls.getStartPoint().getX(), ls.getEndPoint().getX());
			double lsy2 = Math.max(ls.getStartPoint().getY(), ls.getEndPoint().getY());
			ArrayList<Double> distances = new ArrayList<Double>(internal.size());
			Map<Double,Point> distanceMap = new HashMap<Double, Point>();
			for (Point point : internal.values()) {
				double pX = point.getX();
				double pY = point.getY();
				if((pX > lsx1 && pX < lsx2) && (pY > lsy1 && pY < lsy2)){
					Double d = new Double(ls.distance(point));
					if(d > 0){
						distances.add(d);
						distanceMap.put(d, point);
					}
				}
			}
			if(distances.size() > 0){
				Collections.sort(distances);

				Point pointToFollow = ls.getStartPoint();
				Coordinate ptf = new Coordinate(pointToFollow.getX(), pointToFollow.getY());
				Point pointToInsert = distanceMap.get(distances.get(0));

				Coordinate pti = new Coordinate(pointToInsert.getX(), pointToInsert.getY());
				Coordinate [] newHull = new Coordinate[hullCoordinates.length+1];
				int coordinateIndex = 0;
				boolean inserted = false;
				for(Coordinate coordinate : hullCoordinates){
					newHull[coordinateIndex] = coordinate;
					coordinateIndex++;
					if(coordinate.equals2D(ptf) && !inserted){
						newHull[coordinateIndex] = pti;
						coordinateIndex++;
						inserted = true;
					}
				}
				hull = gf.createPolygon(gf.createLinearRing(newHull), null);

				// Reevaluate the internal and external QuadTrees
				internal = new QuadTree<Point>(envelope[0].x, envelope[0].y, envelope[2].x, envelope[2].y);
				for (Point point : c.getPoints()) {
					Coordinate [] ca = convexHull.getCoordinates();
					int index = 0;
					boolean onBoundary = false;
					while(!onBoundary && index < ca.length){
						Point boundaryPoint = gf.createPoint(ca[index]);
						if(boundaryPoint.distance(point) == 0){
							onBoundary = true;
						}
						index++;
					}
					if(!onBoundary){
						internal.put(point.getX(), point.getY(), point);
					}
				}
			} else{
				break;
			}
		}
		Polygon result = cleanPolygon(hull);
		return result;
	}
	
	private Polygon cleanPolygon(Polygon polygon){
		GeometryFactory gf = new GeometryFactory();
		Polygon result = null;
		Coordinate[] coordinates = polygon.getCoordinates();
		ArrayList<Coordinate> al = new ArrayList<Coordinate>(coordinates.length);
		al.add(coordinates[0]);
		for(int i = 1; i < coordinates.length-1; i++){
			if(!coordinates[i].equals2D(coordinates[i-1])){
				al.add(coordinates[i]);
			}
		}
		al.add(al.get(0));
		int index = 0;
		Coordinate[]  ca = new Coordinate[al.size()];
		for (Coordinate coordinate : al) {
			ca[index] = coordinate;
			index++;
		}
		result = gf.createPolygon(gf.createLinearRing(ca), null);
		return result;
	}

	private LineString findLongestSegmentIndex(Coordinate[] hullCoordinates) {
		GeometryFactory gf = new GeometryFactory();
		Map<Double, LineString> lengthMap = new HashMap<Double, LineString>();
		ArrayList<Double> lengths = new ArrayList<Double>(hullCoordinates.length);
		for(int i = 0; i < hullCoordinates.length; i++){
			Coordinate [] linePoints = new Coordinate [2];
			if(i < hullCoordinates.length-1 ){
				linePoints[0] = hullCoordinates[i];
				linePoints[1] = hullCoordinates[i+1];
			} else{
				linePoints[0] = hullCoordinates[i];
				linePoints[1] = hullCoordinates[0];
			}
			LineString ls = gf.createLineString(linePoints);
			Double length = new Double(ls.getLength());
			lengths.add(length);
			lengthMap.put(length, ls);
		}
		Collections.sort(lengths);
		LineString result = lengthMap.get(lengths.get(lengths.size()-1));
		return result;
	}

	public QuadTree<Point> getInputPoints() {
		return inputPoints;
	}
	
	public QuadTree<ClusterPoint> getClusteredPoints() {
		return clusteredPoints;
	}
	
	public float getRadius() {
		return radius;
	}
	
	public int getMinimumPoints() {
		return minimumPoints;
	}
	

}
