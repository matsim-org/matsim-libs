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

package org.matsim.core.network.algorithms.intersectionSimplifier;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.algorithms.intersectionSimplifier.containers.ClusterActivity;
import org.matsim.core.network.algorithms.intersectionSimplifier.containers.Cluster;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.io.IOUtils;


/**
 * <par>This class implements the density-based clustering approach as published
 * by Zhou <i>et al</i> (2004).</par>
 * <ul>
 * 		<i>``The basic idea of DJ-DigicoreCluster is as follows. For each point, calculate its
 * 		<b>neighborhood</b>: the neighborhood consists of points within distance
 * 		<b>Eps</b>, under condition that there are at least <b>MinPts</b> of them. If no
 * 		such neighborhood is found, the point is labeled noise; otherwise, the points are
 * 		created as a new DigicoreCluster if no neighbor is in an existing DigicoreCluster, or joined with
 * 		an existing DigicoreCluster if any neighbour is in an existing DigicoreCluster.''</i>
 * </ul>
 * <h4>Reference</h4>
 * Zhou, C., Frankowski, D., Ludford, P.m Shekar, S. and Terveen, L. (2004). Discovering
 * personal gazeteers: An interactive clustering approach. <i> Proceedings of the 12th annual
 * ACM International workshop on Geographic Information Systems</i>, p. 266-273. Washington, DC.
 * <h4></h4>
 * @author jwjoubert
 */
public class DensityCluster {
	private List<Node> inputPoints;
	private Map<Id<Coord>, ClusterActivity> lostPoints = new TreeMap<Id<Coord>, ClusterActivity>();
	private QuadTree<ClusterActivity> quadTree;
	private List<Cluster> clusterList;
	private final static Logger log = LogManager.getLogger(DensityCluster.class);
	private String delimiter = ",";
	private final boolean silent;

	/**
	 * Creates a new instance of the density-based cluster with an empty list
	 * of clusters.
	 * @param radius the radius of the search circle within which other activity points
	 * 			are searched.
	 * @param minimumPoints the minimum number of points considered to constitute an
	 * 			independent {@link Cluster}.
	 * @param pointsToCluster
	 */
	public DensityCluster(List<Node> nodesToCluster, boolean silent){
		this.inputPoints = nodesToCluster;

		/*TODO Remove later. */
		int nullCounter = 0;
		for(Node node : inputPoints){
			if(node == null || node.getCoord() == null){
				nullCounter++;
			}
		}
		if(nullCounter > 0){
			log.warn("In DJCluster: of the " + inputPoints.size() + " points, " + nullCounter + " were null.");
		}

		this.clusterList = new ArrayList<Cluster>();
		this.silent = silent;
	}


	/**
	 * Building an <code>ArrayList</code> of <code>DigicoreCluster</code>s. The DJ-Clustering
	 * procedure of Zhou <i>et al</i> (2004) is followed. If there are no points to cluster, a
	 * warning message is logged, and the procedure bypassed.
	 */
	public void clusterInput(double radius, int minimumPoints){
		if(this.inputPoints.size() == 0){
			log.warn("DJCluster.clusterInput() called, but no points to cluster.");
		} else{
			if(!silent){
				log.info("Clustering input points. This may take a while.");
			}
			int clusterIndex = 0;
			int pointMultiplier = 1;
			int uPointCounter = 0;
			int cPointCounter = 0;

			/*
			 * Determine the extent of the QuadTree.
			 */
			double xMin = Double.POSITIVE_INFINITY;
			double yMin = Double.POSITIVE_INFINITY;
			double xMax = Double.NEGATIVE_INFINITY;
			double yMax = Double.NEGATIVE_INFINITY;

			for (Node node : this.inputPoints) {
				Coord c = node.getCoord();
				/* TODO Remove if no NullPointerExceptions are thrown. */
				if(c == null){
					log.warn("Coord is null. Number of points in list: " + inputPoints.size());
				} else{
					xMin = Math.min(xMin, c.getX());
					yMin = Math.min(yMin, c.getY());
					xMax = Math.max(xMax, c.getX());
					yMax = Math.max(yMax, c.getY());
				}
			}
			/*
			 * Build a new QuadTree, and place each point in the QuadTree as a ClusterActivity.
			 * The geographic coordinates of each point is used as the keys in the QuadTree.
			 * Initially all ClusterPoints will have a NULL reference to its DigicoreCluster. An
			 * ArrayList of Points is also kept as iterator for unclustered points.
			 */
			if(!silent){
				log.info("Place points in QuadTree.");
			}
			quadTree = new QuadTree<ClusterActivity>(xMin-1, yMin-1, xMax+1, yMax+1);
			List<ClusterActivity> listOfPoints = new ArrayList<ClusterActivity>();
			for (int i = 0; i < this.inputPoints.size(); i++) {
				double x = inputPoints.get(i).getCoord().getX();
				double y = inputPoints.get(i).getCoord().getY();
				ClusterActivity cp = new ClusterActivity(Id.create(i, Coord.class), inputPoints.get(i), null);
				quadTree.put(x, y, cp);
				listOfPoints.add(cp);
			}
			if(!silent){
				log.info("Done placing activities.");
			}

			int pointCounter = 0;
			while(pointCounter < listOfPoints.size()){
				// Get next point.
				ClusterActivity p = listOfPoints.get(pointCounter);

				if(p.getCluster() == null){
					// Compute the density-based neighbourhood, N(p), of the point p
					Collection<ClusterActivity> neighbourhood = quadTree.getDisk(p.getCoord().getX(), p.getCoord().getY(), radius);
					List<ClusterActivity> uN = new ArrayList<ClusterActivity>(neighbourhood.size());
					List<ClusterActivity> cN = new ArrayList<ClusterActivity>(neighbourhood.size());
					for (ClusterActivity cp : neighbourhood) {
						if(cp.getCluster() == null){
							uN.add(cp);
						} else{
							cN.add(cp);
						}
					}
					if(neighbourhood.size() < minimumPoints){
						/* Point is considered to be noise.
						 * FIXME Not quite true... it may be incorporated into
						 * another cluster later! (JWJ - Mar '14)
						 */

						lostPoints.put(p.getId(), p);
						uPointCounter++;
					}else if(cN.size() > 0){
						/*
						 * Merge all the clusters. Use the DigicoreCluster with the smallest clusterId
						 * value as the remaining DigicoreCluster.
						 */
						List<Cluster> localClusters = new ArrayList<Cluster>();
						Cluster smallestCluster = cN.get(0).getCluster();
						for(int i = 1; i < cN.size(); i++){
							if(Integer.parseInt(cN.get(i).getCluster().getId().toString()) <
									Integer.parseInt(smallestCluster.getId().toString()) ){
								smallestCluster = cN.get(i).getCluster();
							}
							if(!localClusters.contains(cN.get(i).getCluster())){
								localClusters.add(cN.get(i).getCluster());
							}
						}
						for (Cluster DigicoreCluster : localClusters) {
							if(!DigicoreCluster.equals(smallestCluster)){
								List<ClusterActivity> thisClusterList = DigicoreCluster.getPoints();
								for(int j = 0; j < thisClusterList.size(); j++){
									// Change the DigicoreCluster reference of the ClusterActivity.
									thisClusterList.get(j).setCluster(smallestCluster);
									// Add the ClusterActivity to the new DigicoreCluster.
									smallestCluster.getPoints().add(thisClusterList.get(j));
									// Remove the ClusterActivity from old DigicoreCluster.
									/*
									 * 20091009 - I've commented this out... this seems
									 * both dangerous and unnecessary.
									 */
//								DigicoreCluster.getPoints().remove(thisClusterList.get(j));
								}
							}
						}

						// Add unclustered points in the neighborhood.
						for (ClusterActivity cp : uN) {
							smallestCluster.getPoints().add(cp);
							cp.setCluster(smallestCluster);
							cPointCounter++;
							if(lostPoints.containsKey(cp.getId())){
								lostPoints.remove(cp.getId());
								uPointCounter--;
							}
						}
					} else{
						// Create new DigicoreCluster and add all the points.
						Cluster newCluster = new Cluster(Id.create(clusterIndex, Cluster.class));
						clusterIndex++;

						for (ClusterActivity cp : uN) {
							cp.setCluster(newCluster);
							newCluster.getPoints().add(cp);
							cPointCounter++;
							if(lostPoints.containsKey(cp.getId())){
								lostPoints.remove(cp.getId());
								uPointCounter--;
							}
						}
					}
				}
				pointCounter++;
				// Report progress
				if(!silent){
					if(pointCounter == pointMultiplier){
						log.info("   Points clustered: " + pointCounter);
						pointMultiplier = (int) Math.max(pointCounter, pointMultiplier)*2;
					}
				}
			}


			if(!silent){
				log.info("   Points clustered: " + pointCounter + " (Done)");
				int sum = cPointCounter + uPointCounter;
				log.info("Sum should add up: " + cPointCounter + " (clustered) + "
						+ uPointCounter + " (unclustered) = " + sum);

				/* Code added for Joubert & Meintjes paper (2014). */
				log.info("Unclustered points: ");
				for(ClusterActivity ca : lostPoints.values()){
					log.info(String.format("   %.6f,%.6f", ca.getCoord().getX(), ca.getCoord().getY()));
				}
				log.info("New way of unclustered points:");
				log.info("   Number: " + lostPoints.size());
			}

			/*
			 * Build the DigicoreCluster list. Once built, I rename the clusterId field so as to
			 * start at '0', and increment accordingly. This allows me to directly use
			 * the clusterId field as 'row' and 'column' reference in the 2D matrices
			 * when determining adjacency in Social Network Analysis.
			 */
			if(!silent){
				log.info("Building the DigicoreCluster list (2 steps)");
			}
			Map<Cluster, List<ClusterActivity>> clusterMap = new HashMap<Cluster, List<ClusterActivity>>();

			if(!silent){
				log.info("Step 1 of 2:");
				log.info("Number of ClusterPoints to process: " + listOfPoints.size());
			}
			int cpCounter = 0;
			int cpMultiplier = 1;
			for (ClusterActivity ca : listOfPoints) {
				Cluster theCluster = ca.getCluster();
				if(theCluster != null){
					// Removed 7/12/2011 (JWJ): Seems unnecessary computation.
//				theCluster.setCenterOfGravity();

					if(!clusterMap.containsKey(theCluster)){
						List<ClusterActivity> newList = new ArrayList<ClusterActivity>();
						clusterMap.put(theCluster, newList);
					}
					clusterMap.get(theCluster).add(ca);
				}

				if(!silent){
					if(++cpCounter == cpMultiplier){
						log.info("   ClusterPoints processed: " + cpCounter + " (" + String.format("%3.2f", ((double)cpCounter/(double)listOfPoints.size())*100) + "%)");
						cpMultiplier *= 2;
					}
				}
			}
			if(!silent){
				log.info("   ClusterPoints processed: " + cpCounter + " (Done)");
			}

			if(!silent){
				log.info("Step 2 of 2:");
				log.info("Number of clusters to process: " + clusterMap.keySet().size());
			}
			int clusterCounter = 0;
			int clusterMultiplier = 1;
			int clusterNumber = 0;
			for (Map.Entry<Cluster, List<ClusterActivity>> e : clusterMap.entrySet()) {
				Cluster digicoreCluster = e.getKey();
				List<ClusterActivity> listOfClusterPoints = e.getValue();
				if(listOfClusterPoints.size() >= minimumPoints){
					digicoreCluster.setClusterId(Id.create(clusterNumber++, Cluster.class));
					clusterNumber++;
					digicoreCluster.setCenterOfGravity();
					clusterList.add(digicoreCluster);
				} else if(!silent){
					log.warn(" ... why do we HAVE a cluster with too few points?...");
				}

				if(!silent){
					if(++clusterCounter == clusterMultiplier){
						log.info("   Clusters processed: " + clusterCounter + " (" + String.format("%3.2f",	((double)clusterCounter / (double)clusterMap.keySet().size())*100) + "%)");
						clusterMultiplier *= 2;
					}
				}
			}
			if(!silent){
				log.info("   Clusters processed: " + clusterCounter + " (Done)");
				log.info("DigicoreCluster list built.");
			}
		}

		// lost list must be made up of clusters without Id.
	}


	/**
	 * For each DigicoreCluster, this method writes out the DigicoreCluster id, the DigicoreCluster's center of
	 * gravity (as a longitude and latitude value), and the order of the DigicoreCluster, i.e.
	 * the number of activity points from the input data associated with the DigicoreCluster.
	 * Output is a comma-separated flat file, by default, but the delimiter can be set
	 * using the class method <code>setDelimiter(String string)</code>.
	 * <h5>File format:</h5>
	 * <ul><code>
	 * 		ClusterId,Long,Lat,NumberOfActivities<br>
	 * 		0,28.7654,35.4576,12<br>
	 * 		1,28.0114,31.3421,5<br>
	 * 		...
	 * </code></ul>
	 *
	 * @param filename the absolute file path to where the DigicoreCluster information is written.
	 */
	public void writeClustersToFile(String filename){

		int clusterCount = 0;
		int clusterMultiplier = 1;
		int totalClusters = clusterList.size();
		if(!silent){
			log.info("Writing a total of " + totalClusters + " to file.");
		}

			try (BufferedWriter output = IOUtils.getBufferedWriter(filename)) {
				output.write("ClusterId,Long,Lat,NumberOfActivities");
				output.write("\n");

				for (Cluster c : clusterList) {
					c.setCenterOfGravity();
					Coord center = c.getCenterOfGravity();
					output.write(c.getId().toString());
					output.write(delimiter);
					output.write(String.valueOf(center.getX()));
					output.write(delimiter);
					output.write(String.valueOf(center.getY()));
					output.write(delimiter);
					output.write(String.valueOf(c.getPoints().size()));
					output.write("\n");

					clusterCount++;
					// Report progress
					if(!silent){
						if(clusterCount == clusterMultiplier){
							log.info("   Clusters written: " + clusterCount);
							clusterMultiplier *= 2;
						}
					}
				}
				if(!silent){
					log.info("   Clusters written: " + clusterCount + " (Done)" );
				}
		} catch (IOException e) {
			log.error("Could not write cluster to file.", e);
		}
	}

	public List<Cluster> getClusterList() {
		return clusterList;
	}


	public QuadTree<ClusterActivity> getClusteredPoints() {
		return quadTree;
	}


	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public Map<Id<Coord>,ClusterActivity> getLostPoints(){
		return this.lostPoints;
	}

}
