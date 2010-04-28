/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyseActivityDuration.java
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

package playground.jjoubert.CommercialTraffic.ActivityAnalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;

import playground.jjoubert.CommercialTraffic.Activity;
import playground.jjoubert.CommercialTraffic.Chain;
import playground.jjoubert.CommercialTraffic.CommercialVehicle;
import playground.jjoubert.Utilities.DateString;
import playground.jjoubert.Utilities.MyShapefileReader;
import playground.jjoubert.Utilities.MyXmlConverter;
import playground.jjoubert.Utilities.Clustering.ClusterPoint;
import playground.jjoubert.Utilities.Clustering.DJCluster;
import playground.jjoubert.Utilities.FileSampler.MyFileFilter;
import playground.jjoubert.Utilities.FileSampler.MyFileSampler;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class CommercialActivityAnalyser {
	private final static Logger log = Logger.getLogger(CommercialActivityAnalyser.class);
	private String signalFilename;
	private String fromCoordinateSystem;
	private String toCoordinateSystem;
	private MultiPolygon studyArea;
	private String studyAreaName;
	private MyFileFilter filter;
	private MyFileSampler sampler;
	private DateString ds;
	private MyActivityAnalysisStringBuilder sb;
	private BufferedWriter vehicleOutput;
	private BufferedWriter chainOutput;
	private BufferedWriter minorOutput;
	private BufferedWriter majorOutput;
	
	/**
	 * @param signalFilename
	 * @param fromCoordinateSystem
	 * @param toCoordinateSystem
	 * @param studyArea a <code>MultiPolygon</code>, assumed to be in the correct 
	 * 		coordinate system. This argument may be <code>null</code>.
	 * @param dateString a <code>DateString</code> so that the unique run can be 
	 * 		identified in the output.
	 */
	public CommercialActivityAnalyser(MyActivityAnalysisStringBuilder sb,
									  String fromCoordinateSystem, 
									  String toCoordinateSystem,
									  String studyArea,
									  DateString dateString){
		this.ds = dateString;
		this.sb = sb;
		this.signalFilename = sb.getSignalFilename();
		this.fromCoordinateSystem = fromCoordinateSystem;
		this.toCoordinateSystem = toCoordinateSystem;
		this.studyAreaName = studyArea;
		MyShapefileReader msr = new MyShapefileReader(sb.getShapefilename(studyArea));
		this.studyArea = msr.readMultiPolygon();
		filter = new MyFileFilter(".txt");
		sampler = new MyFileSampler(sb.getSortedVehicleFoldername());
	}

	public void extractChains(int sample, int sampleSize, double majorThreshold, double withinThreshold, float clusterRadius, int clusterCount, boolean writeStatistics) {
		
		log.info("================================================");
		log.info("Extracting vehicle chains for sample " + sample + ".");
		log.info("Minor/major threshold value: " + String.format("%4.1f", majorThreshold));
		log.info("================================================");
		
		
		List<File> sampleFiles = sampler.sampleFiles(sampleSize, filter);

		CommercialActivityExtractor cae = new CommercialActivityExtractor(fromCoordinateSystem, toCoordinateSystem);
		cae.readSignals(signalFilename);
		
		File xmlFolder = new File(sb.getXmlFoldername());
		if(xmlFolder.exists()){
			log.warn("The folder " + xmlFolder.toString() + " already exists and will be cleared!");
			boolean checkDelete = clearDirectory(xmlFolder);
			if(!checkDelete){
				log.warn("Could not clear and delete " + xmlFolder.toString());
			}
		}
		boolean checkMake = xmlFolder.mkdirs();
		if(!checkMake){
			throw new RuntimeException("Could not successfully create the xml output folder " + xmlFolder.toString());
		}	
		
		File tempFolder = new File(xmlFolder.getParent() + "/Temp/");
		if(tempFolder.exists()){
			log.warn("The folder " + tempFolder.toString() + " already exists and will be cleared!");
			boolean checkDelete = clearDirectory(tempFolder);
			if(!checkDelete){
				log.warn("Could not clear and delete " + tempFolder.toString());
			}
		}
		checkMake = tempFolder.mkdirs();
		if(!checkMake){
			throw new RuntimeException("Could not successfully create the temporary folder " + tempFolder.toString());
		}
		
		List<Activity> allActivities = new ArrayList<Activity>();
		int fileCounter = 0;
		int fileMultiplier = 1;
		int pointCounter = 0;
		List<Activity> activities; 
		MyXmlConverter mxc = new MyXmlConverter(true);
		for (File file : sampleFiles) {
			activities = cae.extractActivities(file);
			pointCounter += activities.size();
			mxc.writeObjectToFile(activities, tempFolder.getAbsolutePath() + "/" + file.getName());
			
			allActivities.addAll(activities);
			
			// Report progress.
			if(++fileCounter == fileMultiplier){
				log.info("Building activity list. Files processed: " + String.format("%5d", fileCounter) 
						+ " [" + String.format("%3.2f", ((double)cae.getTotalBoinkPoints()/(double)pointCounter)*100) + "%]");
				fileMultiplier *= 2;
			}
		}
		log.info("Building activity list. Files processed: " + String.format("%5d", fileCounter) 
				+ " [" + String.format("%3.2f", ((double)cae.getTotalBoinkPoints()/(double)pointCounter)*100) + "%] (Done)");
		
		// Now, convert all the activities to Point, and cluster.
		GeometryFactory gf = new GeometryFactory();
		List<Point> allPoints = new ArrayList<Point>();
		for (Activity activity : allActivities) {
			allPoints.add(gf.createPoint(activity.getLocation().getCoordinate()));
		}
		DJCluster djc = new DJCluster(clusterRadius, clusterCount, allPoints);
		djc.clusterInput();
		QuadTree<ClusterPoint> qt = djc.getClusteredPoints();
		// Trick NOW is, to extract the chain.

		/*
		 * Create the necessary buffered writers. Even if they are not used, they exist.
		 * this just simplifies the implementation to ensure it is closed should
		 * something go wrong. 
		 */
		File outputFolder = new File(sb.getOutputFoldername());
		boolean checkCreate = outputFolder.mkdirs();
		if(!checkCreate){
			throw new RuntimeException("Could not successfully create the output directory " + outputFolder.toString());
		}

		List<String> output = sb.getActivityOutputList();
		try {
			this.vehicleOutput = new BufferedWriter(new FileWriter(new File(output.get(0))));
			this.chainOutput = new BufferedWriter(new FileWriter(new File(output.get(1))));
			this.minorOutput = new BufferedWriter(new FileWriter(new File(output.get(2))));
			this.majorOutput = new BufferedWriter(new FileWriter(new File(output.get(3))));
			
			try{
				// Write the header for each output file.
				vehicleOutput.write("Id,ActivitiesPerChain,AvgDist,AvgDur,Minor,Major,KmPerAreaActivity");
				vehicleOutput.newLine();
				chainOutput.write("VehId,StartHour,Duration,Distance,Activities,InStudyArea,WithinMinor,WithinMajor");
				chainOutput.newLine();
				minorOutput.write("VehId,Long,Lat,StartHour,Duration");
				minorOutput.newLine();
				majorOutput.write("VehId,Long,Lat,StartHour,Duration");
				majorOutput.newLine();
				
				/*
				 * Extract the chains.
				 */
				fileCounter = 0;
				fileMultiplier = 1;
				for (File file : tempFolder.listFiles()) {
					Object o = mxc.readObjectFromFile(file.getAbsolutePath());
					List<Activity> list = null; 
					if(o instanceof ArrayList){
						list = (ArrayList<Activity>) o;
					} else{
						throw new RuntimeException("Could not convert " + file.getName() + " to an ArrayList<Activity>.");
					}
					/* 
					 * Check each activity: if it belongs to a cluster, then change its location 
					 * to that of the centroid of the cluster. Also, thin the activities. 
					 * Whenever two consecutive activities are from the same cluster, remove the 
					 * second, and alter the first's end time to the second's end time.
					 */
					CommercialChainExtractor cce = new CommercialChainExtractor(majorThreshold);
					cce.cleanActivityList(list, qt);
					List<Chain> chains = cce.extractChains(list);
					if(chains.size() > 0){
						String vehicleId = file.getName().substring(0, file.getName().indexOf("."));
						CommercialVehicle v = new CommercialVehicle(Integer.parseInt(vehicleId));
						boolean checkAdd = v.getChains().addAll(chains);
						if(!checkAdd){
							log.warn("Could not successfully add the chains for vehicle " + vehicleId);
						} else{
							v.updateVehicleStatistics(studyArea);
							String xmlFilename = xmlFolder.getAbsolutePath() + "/" + vehicleId + ".xml";
							mxc.writeObjectToFile(v, xmlFilename);
							if(writeStatistics){
								writeStatistics(v, withinThreshold);
							}
						}
					} else{
						/* 
						 * The chain is dropped, and the vehicle is not considered 'useful' for 
						 * analysis. There will not be an XML file for this vehicle.
						 */
					}
					file.delete();						
					// Report progress.
					if(++fileCounter == fileMultiplier){
						log.info("Processing vehicle chains. Files processed: " + String.format("%5d", fileCounter));
						fileMultiplier *= 2;
					}
				}
				log.info("Processing vehicle chains. Files processed: " + String.format("%5d", fileCounter) + " (Done)");
				tempFolder.delete();
				log.info("Chain extraction complete.");	
			} finally{
				vehicleOutput.close();
				chainOutput.close();
				minorOutput.close();
				majorOutput.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	private void writeStatistics(CommercialVehicle v, double withinThreshold) throws IOException{
		// Vehicle statistics.
		vehicleOutput.write(String.valueOf(v.getVehID()));
		vehicleOutput.write(",");
		vehicleOutput.write(String.valueOf(v.getAvgActivitesPerChain()));
		vehicleOutput.write(",");
		vehicleOutput.write(String.valueOf(v.getAvgChainDistance()));
		vehicleOutput.write(",");
		vehicleOutput.write(String.valueOf(v.getAvgChainDuration()));
		vehicleOutput.write(",");
		vehicleOutput.write(String.format("%1.4f", v.getFractionMinorInStudyArea()));
		vehicleOutput.write(",");
		vehicleOutput.write(String.format("%1.4f", v.getFractionMajorInStudyArea()));
		vehicleOutput.write(",");
		if(v.getKilometerPerAreaActivityStat() > 0){
			vehicleOutput.write(String.valueOf(v.getKilometerPerAreaActivityStat()));
		} else{
			vehicleOutput.write("NA");
		}
		vehicleOutput.newLine();

		for (Chain c : v.getChains()) {
			// Write chain statistics.
			chainOutput.write(String.valueOf(v.getVehID()));
			chainOutput.write(",");
			chainOutput.write(String.valueOf(c.getDayStart().get(Calendar.HOUR_OF_DAY)));
			chainOutput.write(",");
			chainOutput.write(String.valueOf(c.getDuration()));
			chainOutput.write(",");
			chainOutput.write(String.valueOf(c.getDistance()));
			chainOutput.write(",");
			chainOutput.write(String.valueOf(c.getActivities().size()-2)); // Do not count 'major' locations
			chainOutput.write(",");
			chainOutput.write(String.valueOf(c.isInStudyArea()));
			chainOutput.write(",");
			chainOutput.write(String.valueOf(v.getFractionMinorInStudyArea() >= withinThreshold));
			chainOutput.write(",");
			chainOutput.write(String.valueOf(v.getFractionMajorInStudyArea() >= withinThreshold));
			chainOutput.newLine();

			List<Activity> activities = c.getActivities();
			// Write 'major' activity.
			majorOutput.write(String.valueOf(v.getVehID()));
			majorOutput.write(",");
			majorOutput.write(String.valueOf(activities.get(0).getLocation().getCoordinate().x));
			majorOutput.write(",");
			majorOutput.write(String.valueOf(activities.get(0).getLocation().getCoordinate().y));
			majorOutput.write(",");
			majorOutput.write(String.valueOf(activities.get(0).getStartHour()));
			majorOutput.write(",");
			majorOutput.write(String.valueOf(activities.get(0).getDuration()));				
			majorOutput.newLine();

			// Write 'minor' activity.
			for (int i = 1; i < activities.size()-1; i++) {
				minorOutput.write(String.valueOf(v.getVehID()));
				minorOutput.write(",");
				minorOutput.write(String.valueOf(activities.get(i).getLocation().getCoordinate().x));
				minorOutput.write(",");
				minorOutput.write(String.valueOf(activities.get(i).getLocation().getCoordinate().y));
				minorOutput.write(",");
				minorOutput.write(String.valueOf(activities.get(i).getStartHour()));
				minorOutput.write(",");
				minorOutput.write(String.valueOf(activities.get(i).getDuration()));				
				minorOutput.newLine();
			}
			
			// Write last 'major' activity.
			Activity a = activities.get(activities.size()-1);
			majorOutput.write(String.valueOf(v.getVehID()));
			majorOutput.write(",");
			majorOutput.write(String.valueOf(a.getLocation().getCoordinate().x));
			majorOutput.write(",");
			majorOutput.write(String.valueOf(a.getLocation().getCoordinate().y));
			majorOutput.write(",");
			majorOutput.write(String.valueOf(a.getStartHour()));
			majorOutput.write(",");
			majorOutput.write(String.valueOf(a.getDuration()));				
			majorOutput.newLine();			
		}		
	}


	/**
	 * This method receives a list of files, extracts the activities making use of the 
	 * class <code>playground.jjoubert.CommercialTraffic.ActivityAnalysis.CommercialActivityExtractor</code>,
	 * and writes the duration and start hour of each activity to a file from where the
	 * <code>R</code>-script <code>EstimateDurationFunction</code> can estimate the 
	 * <i>Weibull</i> distribution parameter confidence intervals.
	 *  
	 * @param sampleFiles the sample files whose activities should be extracted;
	 * @param sample the sample number currently handled, for filename purposes of the 
	 * 		the output;
	 * @param ds the <code>DateString</code> uniquely identifying each run.
	 */
	public void analyseSampleDurationsForR(int sample, int sampleSize) {
		
		log.info("===============================================================");
		log.info("Extracting sample " + sample + " activities for `R' analysis.");
		log.info("===============================================================");
		
		List<File> sampleFiles = sampler.sampleFiles(sampleSize, filter);
		
		CommercialActivityExtractor cae = new CommercialActivityExtractor(fromCoordinateSystem, toCoordinateSystem);
		cae.readSignals(signalFilename);

		String outputFile = "/Users/johanwjoubert/R-Source/Input/SampleActivityDuration-" 
			+ ds.toString() + "-Sample" + String.format("%02d", sample) +".txt";
		
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(outputFile)));
			try{
				output.write("Duration,Hour");
				output.newLine();
				int fileCounter = 0;
				int fileMultiplier = 1;
				int pointCounter = 0;
				for (File file : sampleFiles) {
					List<Activity> listActivities = cae.extractActivities(file);
					pointCounter += listActivities.size();
					for (Activity activity : listActivities) {
						output.write(String.valueOf(activity.getDuration()));
						output.write(",");
						output.write(String.valueOf(activity.getStartHour()));
						output.newLine();
					}
					if(++fileCounter == fileMultiplier){
						log.info("Files processed: " + String.format("%5d", fileCounter) 
								+ " [" + String.format("%3.2f", ((double)cae.getTotalBoinkPoints()/(double)pointCounter)*100) + "%]");
						fileMultiplier *= 2;
					}
				}
				log.info("Files processed: " + String.format("%5d", fileCounter) 
						+ " [" + String.format("%3.2f", ((double)cae.getTotalBoinkPoints()/(double)pointCounter)*100) + "%] (Done)");
			} finally{
				output.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	


	/**
	 * Just a short method to clear all files from a given folder. If subfolders exist, 
	 * the method calls itself recursively until all files, and directories, have been
	 * cleared. Log warnings will appear if any <code>delete</code> could not be executed
	 * successfully. 
	 * @param file the folder that should be cleared;
	 * @return <code>true</code> if the folder has been cleared, and deleted, successfully.
	 */
	private static boolean clearDirectory(File file){
		boolean result = false;
		if(file.isDirectory()){
			File[] files = file.listFiles();
			for (File inFolderFile : files) {
				if(inFolderFile.isFile()){
					inFolderFile.delete();
				} else if(inFolderFile.isDirectory()){
					boolean clearDirectory = clearDirectory(inFolderFile);
					if(!clearDirectory){
						log.warn(inFolderFile.toString() + " could not be cleared successfully.");
					}
				} else{
					log.warn(inFolderFile.toString() + " is neither a file nor a directory!");
				}
			}
			result = file.delete();
		} else{
			log.warn(file.toString() + " is not a directory!");
		}		
		return result;
	}
	
}
