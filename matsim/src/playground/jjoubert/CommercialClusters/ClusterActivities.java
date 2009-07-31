/* *********************************************************************** *
 * project: org.matsim.*
 * ClusterActivities.java
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

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import playground.jjoubert.Utilities.MyActivityReader;
import playground.jjoubert.Utilities.MyShapefileReader;

public class ClusterActivities {
	/* 
	 * String value that must be set. Allowed study areas are:
	 * 		- SouthAfrica
	 * 		- Gauteng
	 * 		- KZN
	 * 		- WesternCape
	 */
	private static String studyAreaName = "Gauteng";
	/*
	 * String value that must be set. Allowed activity types are:
	 * 		- Minor
	 * 		- Major
	 */
	private static String activityType = "Minor";

	// Set the home directory, depending on where the job is executed.
	static String root = "/Users/johanwjoubert/MATSim/workspace/MATSimData/"; 	// Mac
//	static String root = "/home/jjoubert/";										// IVT-Sim0
//	static String root = "/home/jjoubert/data/";								// Satawal

	private final static Logger log = Logger.getLogger(ClusterActivities.class);
	private static int radius = 100;
	private static int minPoints = 5;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log.info("========================================================");
		log.info("Clustering activities for " + studyAreaName);
		log.info("========================================================");
		log.info("");
//		String studyAreaShapefile = root + "Shapefiles/" + studyAreaName + "/" + studyAreaName + "_UTM35S.shp";
//		MyShapefileReader msr = new MyShapefileReader(studyAreaShapefile);
//		MultiPolygon studyArea = msr.readMultiPolygon();
		
//		String activityFilename = root + studyAreaName + "/Activities/" + studyAreaName + activityType + "Locations.txt";
		String activityFilename = root + "Gauteng/Activities/GautengMinorLocations_CityDeepSample.txt";
		MyActivityReader ar = new MyActivityReader();
		QuadTree<Point> studyAreaPoints = ar.readActivityPointsToQuadTree(activityFilename);
		
		DJCluster djc = new DJCluster(radius, minPoints, studyAreaPoints);
		djc.clusterInput();
		String pointFilename = root + "Gauteng/Activities/Minor_CityDeep_Point_" + radius + "_" + minPoints + ".txt";
		String clusterFilename = root + "Gauteng/Activities/Minor_CityDeep_Cluster_" + radius + "_" + minPoints + ".txt";
		String lineFilename = root + "Gauteng/Activities/Minor_CityDeep_Line_" + radius + "_" + minPoints + ".txt";
		String polygonFilename = root + "Gauteng/Activities/Minor_CityDeep_Polygon_" + radius + "_" + minPoints + ".txt";
		djc.visualizeClusters(pointFilename, clusterFilename, lineFilename, polygonFilename);
		
//		String clusterFilename = root + studyAreaName + "/Activities/" + studyAreaName + activityType + "Clusters.txt";
//		djc.writeClustersToFile(clusterFilename);
		


	}

}
