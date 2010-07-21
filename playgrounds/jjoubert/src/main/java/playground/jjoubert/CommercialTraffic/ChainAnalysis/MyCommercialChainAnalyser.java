/* *********************************************************************** *
 * project: org.matsim.*
 * MyCommercialChainAnalyser.java
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

package playground.jjoubert.CommercialTraffic.ChainAnalysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;

import cern.colt.matrix.impl.SparseDoubleMatrix3D;

import playground.jjoubert.CommercialTraffic.Chain;
import playground.jjoubert.CommercialTraffic.CommercialVehicle;
import playground.jjoubert.Utilities.MyVehicleIdentifier;
import playground.jjoubert.Utilities.MyXmlConverter;
import playground.jjoubert.Utilities.FileSampler.MyFileFilter;

/**
 * This class analyses all <code>XML</code> vehicle files from a given folder and generates 
 * two three-dimensional matrices, one for <i>within</i> and another for <i>through</i> 
 * vehicles. The three dimensions of the matrix is:
 * <ol>
 * 	<li> chain start time (hour of day);
 * 	<li> number of activities; and
 * 	<li> chain duration (in hours).
 * </ol>
 * 
 * @author johanwjoubert
 */
public class MyCommercialChainAnalyser {
	private Logger log;
	private List<Integer> withinList;
	private List<Integer> throughList;
	private SparseDoubleMatrix3D withinMatrix;
	private SparseDoubleMatrix3D throughMatrix;
	private final String studyArea; 
	
	/**
	 * The constructor creates an instance of the commercial chain analyser. A logger is 
	 * created and the study area for the analysis is set. The constructor then creates 
	 * two lists from the <code>VehicleStatistics</code> file: one for vehicle Ids that
	 * are considered <i>within</i>, and another for vehicle Ids considered <i>through</i>
	 * vehicles. The two matrices are set to <code>null</code>.
	 * @param studyArea a <code>String</code> indicating the area for which chain analyses
	 * 		are done.
	 * @param threshold the <code>Double</code> value that indicates the highest (inclusive)
	 * 		percentage of activities in the study area that a vehicle must have and still be
	 * 		considered <i>through</i>. Vehicles with a minor activity percentage <b>higher</b>
	 * 		than this threshold will be considered <i>within</i> traffic. 
	 * @param vehicleStatisticsFile the absolute pathname where the vehicle statistics file
	 * 		can be found.
	 */
	public MyCommercialChainAnalyser(String studyArea, double threshold, String vehicleStatisticsFile){
		this.log = Logger.getLogger(MyCommercialChainAnalyser.class);
		this.studyArea = studyArea;
		MyVehicleIdentifier mvi = new MyVehicleIdentifier(threshold);
		
		List<List<Integer>> list = mvi.buildVehicleLists(vehicleStatisticsFile, ",");
		this.withinList = list.get(0);
		this.throughList = list.get(1);
		
		this.withinMatrix = null;
		this.throughMatrix = null;
		log.info("Vehicle lists built.");
	}
	
	/**
	 * Checks if chain characteristic matrices already exist as text files. If they do,
	 * they are read. Otherwise, vehicle XML files are read and analysed according to 
	 * whether their Ids are in the <i>within</i> or <i>through</i> list. If a vehicle
	 * has 0% of its activities in the study area, it is omitted. Once all vehicles are
	 * analysed, the matrix files are written to text files. <br>
	 * <br>
	 * <b>Note:</b> The chain start time is <i>always</i> expressed in the hour of the
	 * 		day, and is hence 0-23, and is not configured as a parameter. 
	 * @param folderXML the absolute path where vehicle <code>XML</code> files are found.
	 * @param folderMatrices the absolute path where chain characteristic matrix files
	 * 		can possibly reside.
	 * @param maxActivities the maximum number of activities per chain. 
	 * @param maxDuration the maximum duration (in hours) of a single chain.
	 */
	public void analyse(String folderXML, String folderMatrices, int maxActivities, int maxDuration){
		withinMatrix = new SparseDoubleMatrix3D(24, maxActivities+1, maxDuration+1);
		throughMatrix = new SparseDoubleMatrix3D(24, maxActivities+1, maxDuration+1);
		
		log.info("Checking for the availability of chain characteristic matrices.");
		/*
		 * Check if matrix files already exist. If not, analyse and write new matrices.
		 * If they do exist, just read them in.
		 */
		File f1 = new File(folderMatrices + "/" + studyArea + "_WithinChainMatrix.txt");
		File f2 = new File(folderMatrices + "/" + studyArea + "_ThroughChainMatrix.txt");
		if(f1.exists() && f2.exists()){
			log.info("   Chain matrix files found.");
			this.readMatrixFiles(folderMatrices, studyArea);
		} else{
			log.info("   No chain matrix files found. Analyse XML vehicle files.");
			List<File> files = getFiles(folderXML);
			log.info("Analysing vehicle chains from " + files.size() + " XML files.");
			
			MyXmlConverter mxc = new MyXmlConverter(true);
			
			int fileCounter = 0;
			int fileMultiplier = 1;
			for (File file : files) {
				// Convert file into CommercialVehicle
				CommercialVehicle v = null;
				Object o = mxc.readObjectFromFile(file.getAbsolutePath());
				if(o instanceof CommercialVehicle){
					v = (CommercialVehicle) o;
				} else{
					throw new RuntimeException("Could not convert XML source file " + file.getName() + " to type CommercialVehicle.");
				}
				SparseDoubleMatrix3D m = null;
				if(withinList.contains(Integer.valueOf(v.getVehID()))){
					m = withinMatrix;
				} else if(throughList.contains(Integer.valueOf(v.getVehID()))){
					m = throughMatrix;
				} 
				/*
				 * I had the following in there... but removed it. If a vehicle has 0% activity
				 * in the study area, it is not considered a 'through' vehicle according to
				 * playground.jjoubert.Utilities.MyVehicleIdentifier.java
				 */
//			else{
//				throw new RuntimeException("Vehicle " + file.getName() + " is neither a 'within' nor a 'through' vehicle.");
//			}
				if(m != null){
					// Analyse each chain of the vehicle
					for (Chain chain : v.getChains()) {
						// Chain start time
						GregorianCalendar chainStartTime = chain.getActivities().get(0).getEndTime();
						Integer chainStartHour = chainStartTime.get(Calendar.HOUR_OF_DAY);
						if(chainStartHour==null){
							throw new RuntimeException("Finding index in 3D matrix: Chain start time is null.");
						}
						
						// Number of activities per chain (don't count two 'major' activities at either end
						Integer numberOfActivities = Math.min(maxActivities, chain.getActivities().size() - 2);
						if(numberOfActivities==null){
							throw new RuntimeException("Finding index in 3D matrix: Number of activities is null.");
						}
						
						// Chain duration
						GregorianCalendar chainEndTime = chain.getActivities().get(chain.getActivities().size()-1).getStartTime();
						Long durationMilliseconds = chainEndTime.getTimeInMillis() - chainStartTime.getTimeInMillis();
						Integer durationHours = Math.min(maxDuration, (int) (durationMilliseconds / (1000 * 60 * 60)));
						if(durationHours==null){
							throw new RuntimeException("Finding index in 3D matrix: Chain duration is null.");
						}
						double dummy = m.getQuick(chainStartHour, numberOfActivities, durationHours);
						m.setQuick(chainStartHour, numberOfActivities, durationHours, dummy+1);
					}
				}
				
				// Report progress.
				if(++fileCounter == fileMultiplier){
					log.info("   Vehicles processed: " + fileCounter);
					fileMultiplier *= 2;
				}
			}
			log.info("   Vehicles processed: " + fileCounter + " (Done)");
			this.writeMatrixFile(folderMatrices + studyArea + "_WithinChainMatrix.txt", withinMatrix);
			this.writeMatrixFile(folderMatrices + studyArea + "_ThroughChainMatrix.txt", throughMatrix);
		}
	}
	
	/**
	 * Write the matrices of both the <i>within</i> and the <i>through</i> vehicles's 
	 * chain characteristics to file. 
	 * @param foldername the absolute path of the folder where matrix files are written. 
	 * @param studyArea the study area.
	 */
	public void writeMatrixFiles(String foldername, String studyArea){
		String withinFilename = foldername + studyArea + "_WithinChainMatrix.txt";
		writeMatrixFile(withinFilename, withinMatrix);
		String throughFilename = foldername + studyArea + "_ThroughChainMatrix.txt";
		writeMatrixFile(throughFilename, throughMatrix);
	}
	
	private void writeMatrixFile(String filename, SparseDoubleMatrix3D matrix){
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename)));
			try{
				bw.write("SparseDoubleMatrix3D dimensions:");
				bw.newLine();
				bw.write("StartHour,NumberOfActivities,ChainDuration");
				bw.newLine();
				bw.write(String.valueOf(matrix.slices()));
				bw.write(",");
				bw.write(String.valueOf(matrix.rows()));
				bw.write(",");
				bw.write(String.valueOf(matrix.columns()));
				bw.newLine();
				bw.write("SparseDoubleMatrix3D data:");
				bw.newLine();
				bw.write("Slice,Row,Column,Value");
				bw.newLine();
				for (int a = 0; a < matrix.slices(); a++) {
					for (int b = 0; b < matrix.rows(); b++){
						for (int c = 0; c < matrix.columns(); c++){
							if(matrix.getQuick(a, b, c) > 0){
								bw.write(String.valueOf(a));
								bw.write(",");
								bw.write(String.valueOf(b));
								bw.write(",");
								bw.write(String.valueOf(c));
								bw.write(",");
								bw.write(String.valueOf((int) Math.round(matrix.getQuick(a, b, c))));
								bw.newLine();
							}
						}
					}
				}
				
			} finally{
				bw.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Reads the matrices of both the <i>within</i> and the <i>through</i> vehicles's 
	 * chain characteristics from file. 
	 * @param foldername the absolute path of the folder from where matrix files are read. 
	 * @param studyArea the study area.
	 */
	public void readMatrixFiles(String foldername, String studyArea){
		String withinFilename = foldername + studyArea + "_WithinChainMatrix.txt";
		log.info("Reading 'within' chain characteristic matrix from file: " + withinFilename);
		this.withinMatrix = readMatrixFile(withinFilename);
		String throughFilename = foldername + studyArea + "_ThroughChainMatrix.txt";
		log.info("Reading 'through' chain characteristic matrix from file: " + throughFilename);
		this.throughMatrix = readMatrixFile(throughFilename);
	}
	
	private SparseDoubleMatrix3D readMatrixFile(String filename) {
		SparseDoubleMatrix3D result = null;
		File f = new File(filename);
		if(f.exists()){
			try {
				Scanner input = new Scanner(new BufferedReader(new FileReader(f)));
				try{
					String line = input.nextLine();
					line = input.nextLine();
					line = input.nextLine();
					String[] dim = line.split(",");
					int dim1 = Integer.parseInt(dim[0]);
					int dim2 = Integer.parseInt(dim[1]);
					int dim3 = Integer.parseInt(dim[2]);
					result = new SparseDoubleMatrix3D(dim1, dim2, dim3);
					
					line = input.nextLine();
					line = input.nextLine();
					while(input.hasNextLine()){
						String [] entry = input.nextLine().split(",");
						if(entry.length == 4){
							result.setQuick(Integer.parseInt(entry[0]), 
									Integer.parseInt(entry[1]), 
									Integer.parseInt(entry[2]), 
									Double.parseDouble(entry[3]));
						}
					}					
				} finally{
					input.close();
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		} else{
			throw new RuntimeException("Can not read from matrix file " + filename);
		}
		return result;
	}

	private List<File> getFiles(String sourceFolder){
		List<File> result = new ArrayList<File>();
		File folder = new File(sourceFolder);
		if(!folder.exists() || !folder.isDirectory()){
			throw new RuntimeException("Given source folder " + sourceFolder + " is not a valid source directory!");
		}
		MyFileFilter mff = new MyFileFilter(".xml");
		File[] files = folder.listFiles(mff);
		for (File file : files) {
			result.add(file);
		}
		return result;		
	}
	

	public List<Integer> getWithinList() {
		return withinList;
	}
	
	public List<Integer> getThroughList() {
		return throughList;
	}
	
	public SparseDoubleMatrix3D getWithinMatrix() {
		return withinMatrix;
	}
	
	public SparseDoubleMatrix3D getThroughMatrix() {
		return throughMatrix;
	}
	
}
