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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import playground.jjoubert.CommercialTraffic.Chain;
import playground.jjoubert.CommercialTraffic.CommercialVehicle;
import playground.jjoubert.Utilities.Clustering.DJCluster;
import playground.jjoubert.Utilities.MyActivityReader;
import playground.jjoubert.Utilities.MyShapefileReader;
import playground.jjoubert.Utilities.MyXmlConverter;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class ActivityClusterBuilder {
	
	private Logger log = Logger.getLogger(ActivityClusterBuilder.class);
	private DJCluster djc;
	private MultiPolygon studyArea;
	private MyCommercialClusterStringBuilder sb;
	
	public ActivityClusterBuilder(MyCommercialClusterStringBuilder stringBuilder){
		this.sb = stringBuilder;
		
		MyShapefileReader msfr = new MyShapefileReader(sb.getShapefilename());
		this.studyArea = msfr.readMultiPolygon();
	}
	
	
	/**
	 * This method clusters activities.
	 * @param radius
	 * @param minimumPoints
	 * @param activityType the type of activity to be clustered. This parameter may be
	 * 		<code>minor</code>, <code>major</code> or <code>null</code>. If the activity
	 * 		type is <code>null</code>, then <b><i>both</i></b> <code>minor</code> and 
	 * 		<code>major</code> activity types will be clustered.
	 */
	public void clusterActivities(float radius, int minimumPoints, String activityType){
		List<Point> studyAreaPoints = new ArrayList<Point>();
		try{
			if(activityType == null){
				// Add BOTH minor and major activities 
				MyActivityReader minor = new MyActivityReader();
				List<Point> minorPoints = minor.readActivityPointsToList(sb.getMinorActivityFilename(), studyArea);
				studyAreaPoints.addAll(minorPoints);

				MyActivityReader major = new MyActivityReader();
				List<Point> majorPoints = major.readActivityPointsToList(sb.getMajorActivityFilename(), studyArea);
				studyAreaPoints.addAll(majorPoints);	
				
			} else if(activityType.equalsIgnoreCase("minor")){
				// Add only minor activities
				MyActivityReader minor = new MyActivityReader();
				List<Point> minorPoints = minor.readActivityPointsToList(sb.getMinorActivityFilename(), studyArea);
				studyAreaPoints.addAll(minorPoints);
			} else if(activityType.equalsIgnoreCase("major")){
				// Add only major activities
				MyActivityReader major = new MyActivityReader();
				List<Point> majorPoints = major.readActivityPointsToList(sb.getMajorActivityFilename(), studyArea);
				studyAreaPoints.addAll(majorPoints);
			} else{
				log.warn("Incorrect activity type provided.");
				throw new RuntimeException("Only `minor', `major' or `null' are acceptable activity types.");
			}
		} finally{
			log.info("Read activity file(s) sucessfully.");
		}
		this.djc = new DJCluster(radius, minimumPoints, studyAreaPoints);
		djc.clusterInput();
	}

	/**
	 * 
	 * @param vehicleFoldername
	 * @param vehicleFilename
	 * @param silent a logical variable indicating whether log messages should be 
	 * 		displayed for the individual vehicle chains. If <code>true</code> then 
	 * 		messages will be suppressed and only a vehicle counter will be logged.
	 * 		If <code>false</code> then each individual vehicle chain's progress 
	 * 		will be logged and displayed.
	 */
	public void executeSna(String vehicleFoldername, String vehicleFilename, boolean silent){
		// Ensure the cluster list contains clusters.
		if(djc.getClusterList().size() > 0){
			MyAdjancencyMatrixBuilder mamb = new MyAdjancencyMatrixBuilder(djc.getClusterList());
			if(vehicleFoldername != null && vehicleFilename != null){
				throw new RuntimeException("Both a vehicle folder and vehicle filename has been supplied.");
			} else if(vehicleFoldername != null){
				File folder = new File(vehicleFoldername);
				if(folder.exists() && folder.isDirectory()){
					File[] fileList = folder.listFiles();
					if(silent){
						log.info("Approximate number of vehicle files to process: " + fileList.length);
					}
					int vehicleCounter = 0;
					int vehicleMultiplier = 1;
					for (File file : fileList) {
						if(file.getName().length() > 4){
							String extention = file.getName().substring(file.getName().length()-4);
							if(extention.equalsIgnoreCase(".xml")){
								List<Chain> chains = readVehicleChain(file.getAbsolutePath(), silent);
								mamb.buildAdjacency(chains, silent);
							}
						}
						
						// Report progress
						if(silent){
							if(++vehicleCounter == vehicleMultiplier){
								log.info("   Vehicles processed: " + vehicleCounter);
								vehicleMultiplier = vehicleMultiplier*2;
							}
						}
					}
					if(silent){
						log.info("   Vehicles processed: " + vehicleCounter + " (Done)");
					}
				}
			} else if(vehicleFilename != null){
				List<Chain> chains = readVehicleChain(vehicleFilename, silent);
				mamb.buildAdjacency(chains, silent);
			} else{
				throw new RuntimeException("No vehicle file or folder specified.");
			}
			List<String> outputList = sb.getSnaOutputFilenameList();
			mamb.writeAdjacenciesToFile(outputList.get(0), outputList.get(1), outputList.get(2), outputList.get(3));
			mamb.writeAdjacencyAsNetworkToFile(djc.getClusterList(), outputList.get(4), outputList.get(5), outputList.get(6));
		} else{
			throw new RuntimeException("The cluster list size is not positive! Can not build adjacency matrix.");
		}
	}
	
	private List<Chain> readVehicleChain(String filename, boolean silent){
		List<Chain> result = null;
		MyXmlConverter mxc = new MyXmlConverter(silent);
		Object o = mxc.readObjectFromFile(filename);
		if(o instanceof CommercialVehicle){
			result = ((CommercialVehicle) o).getChains();
		} else{
			log.warn("Could not cast the object " + filename + " as a type CommercialVehicle!");
		}
		return result;
	}
	
	public DJCluster getDjc() {
		return djc;
	}


}