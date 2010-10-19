/* *********************************************************************** *
 * project: org.matsim.*
 * MyCommercialDemandGenerator01.java
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

package playground.jjoubert.CommercialDemandGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.log4j.Logger;

import playground.jjoubert.CommercialTraffic.ActivityLocations;
import playground.jjoubert.CommercialTraffic.Chain;
import playground.jjoubert.CommercialTraffic.Vehicle;
import playground.jjoubert.Utilities.MyVehicleIdentifier;
import playground.jjoubert.Utilities.MyXmlConverter;
import cern.colt.matrix.impl.DenseDoubleMatrix3D;

public class MyCommercialDemandGenerator01 {
	private final Logger log;
	private final String root;
	private final String studyArea;
	private final double activityThreshold;
	private List<Integer> withinList;
	private List<Integer> throughList;

	private final static int dimensionStart = 24; 		// values 00h00m00 - 23h59m59
	private final static int dimensionActivities = 21; 	// index '0' should never be used
	private final static int dimensionDuration = 49; 	// index '0' should never be used

	

	public MyCommercialDemandGenerator01(String root, String studyArea, double activitythreshold) {
		log = Logger.getLogger(MyCommercialDemandGenerator01.class);
		this.root = root;
		this.studyArea = studyArea;
		this.activityThreshold = activitythreshold;
		withinList = null;
		throughList = null;
	}
	
	/**
	 * 
	 * @throws RuntimeException when not being able to create <i>within</i> or 
	 * 		<i>through</i> vehicle lists using the <tt>MyVehicleIdentifier</tt> class.
	 */
	public void createPlans(){
		log.info("Start creating plans.");
		if(withinList==null || throughList==null){
			log.warn("Can not create plans if 'within' and 'through' vehicles have not been defined!");
			throw new RuntimeException("First run method `buildVehicleLists(String vehicleStatisticsFilename)'");
		}

		ArrayList<DenseDoubleMatrix3D> matrices = extractMatrices();
		DenseDoubleMatrix3D withinMatrix = matrices.get(0);
		DenseDoubleMatrix3D throughMatrix = matrices.get(1);
		//TODO Build CDFs for chain characteristics

		//TODO Build CDFs for 'major' and 'minor' locations (based on clusters).
		log.info("Plans completed successfully.");
	}
	
	/**
	 * This method creates two {@link ArrayList}s, one for <i>within</i> vehicles 
	 * and one for <i>through</i> vehicles. They are both <i>required</i> before plans 
	 * can be generated.
	 * @param vehicleStatistics the filename of the vehicle statistics generated for the
	 * 		study area. This file was generated from running {@link ActivityLocations}. 
	 * 		For details of the file format, see {@link MyVehicleIdentifier}. 
	 */
	public void buildVehicleLists(){
		String vehicleStatistics = root + studyArea + "/Activities/" + studyArea + "VehicleStats.txt";
		log.info("Building 'within' and 'through' vehicle lists.");
		MyVehicleIdentifier mvi = new MyVehicleIdentifier(activityThreshold);
		try{
			List<List<Integer>> lists = mvi.buildVehicleLists(vehicleStatistics, ",");
			withinList = lists.get(0);
			throughList = lists.get(1);
		} finally{
			if(withinList==null || throughList==null){
				throw new RuntimeException("Could not create 'within' or 'through' vehicle lists!!");
			}
		}
		log.info("Completed buidling vehicle lists.");		
	}
	
	/**
	 * This method checks if matrix files exist that contains the activity counts for 
	 * processed vehicle files. If both <i>within</i> and <i>through</i>  matrix files
	 * exist, they are read in directly; if not, another method is called that analyses 
	 * the vehicle chains and returns the two matrices. The newly created matrices are 
	 * then written to file for future use. 
	 * @return An <code>ArrayList</code> containing two <code>DenseDoubleMatrix3D</code>,
	 * 		one for <i>within</i> vehicles, and the other for <i>through</i> vehicles.
	 */
	private ArrayList<DenseDoubleMatrix3D> extractMatrices(){
		log.info("Extracting activity count matrices from vehicles.");
		ArrayList<DenseDoubleMatrix3D> result = new ArrayList<DenseDoubleMatrix3D>(2);

		/* 
		 * Check if matrix files exist. Create if not. 
		 */
		String withinMatrixFilename = root + studyArea + "/" + studyArea + "WithinMatrixFile.txt";
		File withinMatrixFile = new File(withinMatrixFilename);
		String throughMatrixFilename = root + studyArea + "/" + studyArea + "ThroughMatrixFile.txt";
		File throughMatrixFile = new File(throughMatrixFilename);
		MyXmlConverter xmlConverter = new MyXmlConverter(false);
		DenseDoubleMatrix3D withinMatrix = null;
		DenseDoubleMatrix3D throughMatrix = null;
		if(withinMatrixFile.exists() && throughMatrixFile.exists()){
			/*
			 * If both matrices exist, then read them in.
			 */
			log.info("Matrix files found. Reading from files.");
			Object objectWithin = xmlConverter.readObjectFromFile(withinMatrixFilename);
			if(objectWithin instanceof DenseDoubleMatrix3D){
				withinMatrix = (DenseDoubleMatrix3D) objectWithin;
			} else{
				log.warn("The read 'within' object was not of type DenseDoubleMatrix3D!!");
			}
			Object objectThrough = xmlConverter.readObjectFromFile(throughMatrixFilename);
			if(objectThrough instanceof DenseDoubleMatrix3D){
				throughMatrix = (DenseDoubleMatrix3D) objectThrough;
			} else{
				log.warn("The read 'through' object was not of type DenseDoubleMatrix3D!!");
			}
			log.info("Matrices read sucessfully.");
		} else{
			/*
			 * If either of the two matrices do not exist, the vehicle files must be
			 * processed again, and once the matrices are established, they are written
			 * to file for future use.
			 */
			log.info("Matric files not found. Extracting from vehicle XML file.");
			ArrayList<DenseDoubleMatrix3D> matrices = extractChainProperties();
			withinMatrix = matrices.get(0);
			xmlConverter.writeObjectToFile(matrices.get(0), withinMatrixFilename);
			throughMatrix = matrices.get(1);
			xmlConverter.writeObjectToFile(matrices.get(1), throughMatrixFilename);
			log.info("Matrix files created and written successfully.");
		}
		result.add(withinMatrix);
		result.add(throughMatrix);
		log.info("Matrices extracted successfully.");
		return result;
	}
		
//		// Build CDF for chain start times		
//		CumulativeDistribution cdfStartTime = convertMatrixToStartTimeCDF(matrix);
//		// Build empty CDF for number of activities
//		ArrayList<CumulativeDistribution> cdfNumberOfActivities = new ArrayList<CumulativeDistribution>();
//		for(int a = 0; a < matrix.size(); a++){
//			cdfNumberOfActivities.add(null);
//		}
//		// Build an empty CDF for chain duration
//		ArrayList<ArrayList<CumulativeDistribution>> cdfDuration = new ArrayList<ArrayList<CumulativeDistribution>>();
//		for(int a = 0; a < matrix.size(); a++){
//			ArrayList<CumulativeDistribution> cdfDurationn = new ArrayList<CumulativeDistribution>();
//			for(int b = 0; b < matrix.get(a).size(); b++){
//				cdfDurationn.add(null);
//			}
//			cdfDuration.add(cdfDurationn);
//		}
//		return result;
//	}

	private ArrayList<DenseDoubleMatrix3D> extractChainProperties() {
		DenseDoubleMatrix3D withinMatrix = new DenseDoubleMatrix3D(dimensionStart, dimensionActivities, dimensionDuration);
		DenseDoubleMatrix3D throughMatrix = new DenseDoubleMatrix3D(dimensionStart, dimensionActivities, dimensionDuration);
		ArrayList<DenseDoubleMatrix3D> result = new ArrayList<DenseDoubleMatrix3D>(2);
		result.add(withinMatrix);
		result.add(throughMatrix);		
		
		// Process XML files
//		String xmlSource = root + studyArea + "/XML/";
		String xmlSource = root + "Temp/XML/";
		File xmlFolder = new File(xmlSource);
		assert( xmlFolder.isDirectory() ) : "The XML source is not a valid folder!";
		
		log.info("Processing XML files...");
		int fileCounter = 0;
		int fileMultiplier = 1;
		for (File file : xmlFolder.listFiles()) {
			if(file.isFile() && !file.getName().startsWith(".")){
				// Check if the vehicle is considered 'within'
				int vehicleNumber = Integer.parseInt(file.getName().substring(0, file.getName().indexOf(".")));
				if(withinList.contains(vehicleNumber)){
					withinMatrix = extractWithinChains(withinMatrix, xmlSource, file);
				} else if(throughList.contains(vehicleNumber)){
					throughMatrix = extractThroughChains(throughMatrix, xmlSource, file);
				} else{
					log.warn("Vehicle is neither 'within' nor 'through'!!");
				}
				// Update progress
				if(++fileCounter == fileMultiplier){
					log.info("  ... files processed: " + fileCounter);
					fileMultiplier *= 2;
				}
			}
		}
		log.info("... Files processed: " + fileCounter + " (Done)");
		return result;
	}

	private DenseDoubleMatrix3D extractThroughChains(DenseDoubleMatrix3D throughMatrix, String xmlSource, File file) {
		//TODO Extract properties for 'through' vehicles.
		return null;
	}

	private DenseDoubleMatrix3D extractWithinChains(DenseDoubleMatrix3D withinMatrix, String xmlSource, File file) {
		// Convert XML file to Vehicle
		MyXmlConverter mxc = new MyXmlConverter(true);
		Object obj = mxc.readObjectFromFile(xmlSource + file.getName());
		Vehicle vehicle = null;
		if(obj instanceof Vehicle){
			vehicle = (Vehicle) obj;
		}else{
			log.warn("Could not convert vehicle XML file to type: Vehicle!");
		}

		// Analyze each chain
		for (Chain chain : vehicle.getChains()) {
			// Chain start time
			GregorianCalendar chainStart = chain.getActivities().get(0).getEndTime();
			Integer index1 = chainStart.get(Calendar.HOUR_OF_DAY);
			
			// Number of activities
			Integer index2 = null;
			index2 = Math.min(20, chain.getActivities().size() - 2); // Do not count the two major activities at either end of the chain.
			
			// Chain duration
			Integer index3 = null;
			GregorianCalendar chainEnd = chain.getActivities().get(chain.getActivities().size() - 1).getStartTime();
			Long durationMilliseconds = chainEnd.getTimeInMillis() - chainStart.getTimeInMillis();
			Integer durationHours = (int) (durationMilliseconds / (1000 * 60 * 60) );
			index3 = Math.min(47, durationHours);						
			
			withinMatrix.setQuick(index1, index2, index3, withinMatrix.getQuick(index1, index2, index3)+1 );
		}
		return withinMatrix;
	}

	public double getActivityThreshold() {
		return activityThreshold;
	}
	
	public List<Integer> getWithinList() {
		return withinList;
	}
	

	public List<Integer> getThroughList() {
		return throughList;
	}
	
	public String getRoot() {
		return root;
	}

	public String getStudyArea() {
		return studyArea;
	}



}
