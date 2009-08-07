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

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import playground.jjoubert.CommercialTraffic.Vehicle;
import playground.jjoubert.Utilities.MyActivityReader;
import playground.jjoubert.Utilities.MyShapefileReader;
import playground.jjoubert.Utilities.MyXmlConverter;

public class ClusterActivities {
	//====================================================================
	/*
	 * Different scenarios to run:
	 * 		1 - City Deep sample (with output)
	 * 		2 - Study area (with output)
	 * 		3 - City Deep sample (no output; single vehicle SNA)
	 * 		4 - Study area (no output; single vehicle SNA)
	 */
	private static int scenario = 4;
	//====================================================================
	
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
	private static int radius = 20;
	private static int minPoints = 30;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log.info("========================================================");
		log.info("Clustering activities for " + studyAreaName);
		log.info("========================================================");
		log.info("");
		String studyAreaShapefile = root + "Shapefiles/" + studyAreaName + "/" + studyAreaName + "_UTM35S.shp";
		MyShapefileReader msr = new MyShapefileReader(studyAreaShapefile);
		MultiPolygon studyArea = msr.readMultiPolygon();
		
		String activityFilename = null;
		String pointFilename = null;
		String clusterFilename = null;
		String lineFilename = null;
		String polygonFilename = null;
		String vehicleFilename = null;
		String vehicleFoldername = null;
		boolean visualizeClusters = false;
		boolean writeXml = false;
		boolean doSNA = false;
		
		switch (scenario) {
		case 1: // City Deep (with output)
			activityFilename = root + "Gauteng/Activities/GautengMinorLocations_CityDeepSample.txt";
			pointFilename = root + "Gauteng/Activities/Minor_CityDeep_Point_" + radius + "_" + minPoints + ".txt";
			clusterFilename = root + "Gauteng/Activities/Minor_CityDeep_Cluster_" + radius + "_" + minPoints + ".txt";
			lineFilename = root + "Gauteng/Activities/Minor_CityDeep_Line_" + radius + "_" + minPoints + ".txt";
			polygonFilename = root + "Gauteng/Activities/Minor_CityDeep_Polygon_" + radius + "_" + minPoints + ".txt";
			visualizeClusters = true;
			writeXml = false;
			break;
			
		case 2: // Given study area (with output)
			activityFilename = root + studyAreaName + "/Activities/" + studyAreaName + activityType + "Locations.txt";
			pointFilename = root + "Gauteng/Activities/" + studyAreaName + activityType + "Point_" + radius + "_" + minPoints + ".txt";
			clusterFilename = root + "Gauteng/Activities/" + studyAreaName + activityType + "Cluster_" + radius + "_" + minPoints + ".txt";
			lineFilename = root + "Gauteng/Activities/" + studyAreaName + activityType + "Line_" + radius + "_" + minPoints + ".txt";
			polygonFilename = root + "Gauteng/Activities/" + studyAreaName + activityType + "Polygon_" + radius + "_" + minPoints + ".txt";
			visualizeClusters = true;
			writeXml = false;			
			break;
			
		case 3: // City Deep (no output; with SNA; single vehicle)
			activityFilename = root + "Gauteng/Activities/GautengMinorLocations_CityDeepSample.txt";
			pointFilename = root + "Gauteng/Activities/Minor_CityDeep_Point_" + radius + "_" + minPoints + ".txt";
			clusterFilename = root + "Gauteng/Activities/Minor_CityDeep_Cluster_" + radius + "_" + minPoints + ".txt";
			lineFilename = root + "Gauteng/Activities/Minor_CityDeep_Line_" + radius + "_" + minPoints + ".txt";
			polygonFilename = root + "Gauteng/Activities/Minor_CityDeep_Polygon_" + radius + "_" + minPoints + ".txt";
			vehicleFilename = root + "Gauteng/XML/93580.xml"; // has 2
			vehicleFilename = root + "Gauteng/XML/93579.xml"; // has 2
			doSNA = true; 
			break;
			
		case 4: // Study area (no output; with single vehicle SNA)
			activityFilename = root + studyAreaName + "/Activities/" + studyAreaName + activityType + "Locations.txt";
			pointFilename = root + "Gauteng/Activities/" + studyAreaName + activityType + "Point_" + radius + "_" + minPoints + ".txt";
			clusterFilename = root + "Gauteng/Activities/" + studyAreaName + activityType + "Cluster_" + radius + "_" + minPoints + ".txt";
			lineFilename = root + "Gauteng/Activities/" + studyAreaName + activityType + "Line_" + radius + "_" + minPoints + ".txt";
			polygonFilename = root + "Gauteng/Activities/" + studyAreaName + activityType + "Polygon_" + radius + "_" + minPoints + ".txt";
			vehicleFilename = root + "Gauteng/XML/93579.xml";
			doSNA = true;

		default:
			log.info("An invalid scenario " + String.valueOf(scenario) + " has been selected");
			break;
		}
		
		
		MyActivityReader ar = new MyActivityReader();
		ArrayList<Point> studyAreaPoints = ar.readActivityPointsToArrayList(activityFilename, studyArea);
		
		DJCluster djc = new DJCluster(radius, minPoints, studyAreaPoints);
		djc.clusterInput();
				
		if(visualizeClusters){
			djc.visualizeClusters(pointFilename, clusterFilename, lineFilename, polygonFilename);
		}
		
		if(writeXml){
			MyXmlConverter mxc = new MyXmlConverter();
			String xmlFilename = root + studyAreaName + "/" + studyAreaName + activityType + "Cluster_" + radius + "_" + minPoints + ".xml";
			mxc.writeObjectToFile(djc.getClusterList(), xmlFilename);
		}
		
		if(doSNA){
			if(vehicleFoldername != null && vehicleFilename != null){
				log.warn("Both a vehicle folder and vehicle filename has been supplied!");
			} else if(vehicleFoldername != null){
				// TODO Run the whole folder
				log.warn("I have not implemented the vehicle folder yet!");
			} else if(vehicleFilename != null){
				ArrayList<Cluster> cl = djc.getClusterList();
				MyAdjancencyMatrixBuilder mamb = new MyAdjancencyMatrixBuilder(cl);

				Vehicle v = null;
				MyXmlConverter mxc = new MyXmlConverter();
				Object o = mxc.readObjectFromFile(vehicleFilename);
				if(o instanceof Vehicle){
					v = (Vehicle) o;
				} else{
					log.warn("Could not cast the object " + vehicleFilename + " as a type Vehicle!");
				}
				mamb.buildAdjacency(v);
			} else{
				log.warn("SNA not performed!! No vehicle file specified.");
			}
		}
		log.info("Process completed.");
	}

}

//		QuadTree<Point> sa1 = ar.readActivityPointsToQuadTree(activityFilename, studyArea);
//		log.info("Size of sa1: " + sa1.values().size());
//		QuadTree<Point> sa2 = new QuadTree<Point>(sa1.getMinEasting(), sa1.getMinNorthing(), sa1.getMaxEasting(), sa1.getMaxNorthing());
//		for (Point point : sa1.values()) {
//			sa2.put(point.getX(), point.getY(), point);
//		}
//		log.info("Size of sa2: " + sa2.values().size());