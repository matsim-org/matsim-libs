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

package playground.jjoubert.CommercialDemandGenerator;

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
	
	
	public MyCommercialChainAnalyser(double threshold, String vehicleStatisticsFile){
		this.log = Logger.getLogger(MyCommercialChainAnalyser.class);
		MyVehicleIdentifier mvi = new MyVehicleIdentifier(threshold);
		
		List<List<Integer>> list = mvi.buildVehicleLists(vehicleStatisticsFile, ",");
		this.withinList = list.get(0);
		this.throughList = list.get(1);
		
		this.withinMatrix = null;
		this.throughMatrix = null;
		log.info("Vehicle lists built.");
	}
	
	public void analyse(String sourceFolder, int maxActivities, int maxDuration){
		withinMatrix = new SparseDoubleMatrix3D(24, maxActivities+1, maxDuration+1);
		throughMatrix = new SparseDoubleMatrix3D(24, maxActivities+1, maxDuration+1);
		List<File> files = getFiles(sourceFolder);
		MyXmlConverter mxc = new MyXmlConverter(true);
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
			} else{
				throw new RuntimeException("Vehicle " + file.getName() + " is neither a 'within' nor a 'through' vehicle.");
			}
			
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
	}
	
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
	
	public void readMatrixFiles(String foldername, String studyArea){
		log.info("Reading 'within' chain characteristic matrix from file.");
		String withinFilename = foldername + studyArea + "_WithinChainMatrix.txt";
		this.withinMatrix = readMatrixFile(withinFilename);
		log.info("Reading 'through' chain characteristic matrix from file.");
		String throughFilename = foldername + studyArea + "_ThroughChainMatrix.txt";
		this.throughMatrix = readMatrixFile(throughFilename);
	}
	
	private SparseDoubleMatrix3D readMatrixFile(String filename) {
		SparseDoubleMatrix3D result = null;
		File f = new File(filename);
		if(f.exists()){
			try {
				Scanner input = new Scanner(new BufferedReader(new FileReader(f)));
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
