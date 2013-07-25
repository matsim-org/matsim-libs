/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.southafrica.freight.digicore.analysis.activity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.analysis.postClustering.ClusteredChainGenerator;
import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.utils.DigicoreUtils;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

public class ActivityAnalyser {
	final private static Logger LOG = Logger.getLogger(ActivityAnalyser.class);
	private final ExecutorService threadExecutor;
	
	/**
	 * Simple class to perform a number of analysis on activity times and 
	 * durations. The objective is to prepare data for R graphics.
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ActivityAnalyser.class.toString(), args);
		String xmlFolder = args[0];
		String vehicleIdFile = args[1];
		String outputFile = args[2];
		int analysis = Integer.parseInt(args[3]);
		int numberOfThreads = Integer.parseInt(args[4]);
		
		ActivityAnalyser aa = new ActivityAnalyser(numberOfThreads);
		aa.analyse(analysis, xmlFolder, vehicleIdFile, outputFile);
		
		Header.printFooter();
	}
	
	
	/**
	 * Constructor with multiple thread handling. If you only require a single 
	 * thread, you can also use {@link #ActivityAnalyser()}.
	 */
	public ActivityAnalyser(int numberOfThreads) {
		/* Set up the multithreaded analysis. */
		this.threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
	}
	
	
	/**
	 * Constructor with only a single thread. If you require multithreaded 
	 * analysis, you have to use {@link #ActivityAnalyser(int)}.
	 */
	public ActivityAnalyser() {
		this(1);
	}
	
	
	/**
	 * Perform a variety of analysis on digicore vehicle files.
	 * @param the folder where {@link DigicoreVehicle} files can be found;
	 * @param analysis a value indicating the specific analysis to perform. The
	 * 		  following values apply:
	 * 		  <ol>
	 * 			<li> minor activity start time;
	 * 			<li> percentage of all activities associated with a facility, i.e.
	 * 				 having a facilityId; 
	 * 		  </ol>
	 * @param input the file containing {@link DigicoreVehicle} {@link Id}s that
	 * 		  will be used as filter. If this is null, or the file is not 
	 * 		  readable, then all files will be used from the given inputfolder.
	 * @param output the file to which the relevant analysis will be written.
	 * 		  This will typically then be further analysed using R. 
	 */
	public void analyse(int analysis, String xmlFolder, String input, String output){
		/* Check xml folder. */ 
		File folder = new File(xmlFolder);
		if(!folder.exists() || !folder.canRead() || !folder.isDirectory()){
			throw new IllegalArgumentException("Cannot read from " + xmlFolder);
		}
		
		/* Check (optional) input. */
		List<File> vehicleFiles = null;
		if(input == null){
			/* Read all the vehicle files from the xml folder. */
			vehicleFiles = FileUtils.sampleFiles(folder, Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));
		} else{
			File f = new File(input);
			if(input.equalsIgnoreCase("null") || !f.exists() || !f.isFile() || !f.canRead()){
				/* Read all the vehicle files from the xml folder. */
				vehicleFiles = FileUtils.sampleFiles(folder, Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));
			} else{
				/* The input location is assumed to be a file containing the Ids of
				 * those vehicles that should be taken into account. */
				try {
					vehicleFiles = DigicoreUtils.readDigicoreVehicleIds(input, xmlFolder);
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Could not read vehicle Ids from " + input);
				}
			}
		}

		switch (analysis) {
		case 1:
			runMinorActivityStartTimeAnalysis(vehicleFiles, output);
			break;
		case 2:
			runActivitiesWithFacilityIdAnalysis(vehicleFiles, output);
		default:
			break;
		}
	}

	
	/**
	 * Method to analyse a start time distribution of all the `minor' 
	 * {@link DigicoreActivity}s of given {@link DigicoreVehicle} files. The 
	 * start time is expressed as the hour of the day. 
	 * @param vehicles the list of {@link File}s that must be analysed. Each of
	 * 		  these should point to a {@link File} from where a 
	 * 		  {@link DigicoreVehicle} can be read.
	 * @param output the file to where the start time distribution will be 
	 *  	  written to. For each hour of the day, the total number of 
	 *  	  activities that started in that hour will be written. 
	 */
	public void runMinorActivityStartTimeAnalysis(List<File> vehicles, String output){
		LOG.info("Performing minor activity start time analysis (" + vehicles.size() + " vehicle files)");
		Counter counter = new Counter("   vehicles completed # ");
		List<ActivityStartTimeRunable> listOfJobs = new ArrayList<ActivityStartTimeRunable>(vehicles.size());
		
		/* Execute the multi-threaded analysis. */
		for(File file : vehicles){
			ActivityStartTimeRunable job = new ActivityStartTimeRunable(file, counter, "minor");
			this.threadExecutor.execute(job);
			listOfJobs.add(job);
		}
		
		this.threadExecutor.shutdown();
		while(!this.threadExecutor.isTerminated()){
		}
		counter.printCounter();
		
		/* Consolidate the output */
		Map<String, Integer> map = new TreeMap<String, Integer>();
		for(ActivityStartTimeRunable job : listOfJobs){
			Map<String, Integer> thisMap = job.getStartTimeMap();
			for(String s : thisMap.keySet()){
				if(!map.containsKey(s)){
					map.put(s, thisMap.get(s));
				} else{
					int oldCount = map.get(s);
					map.put(s, oldCount + thisMap.get(s));
				}
			}
		}
		
		/* Write the output to file. */
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		try{
			for(String s : map.keySet()){
				bw.write(s);
				bw.write(",");
				bw.write(String.valueOf(map.get(s)));
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + output);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
		
		LOG.info("Done with minor activity start time analysis.");
	}
	
	
	/**
	 * Method to calculate for each vehicle the percentage of its activities,
	 * both major and minor, that are associated with a facility. That is, it 
	 * has a facility Id. This analysis will only make sense on vehicle files
	 * that have been produced from, for example, {@link ClusteredChainGenerator}  
	 * @param vehicles the list of {@link File}s that must be analysed. Each of
	 * 		  these should point to a {@link File} from where a 
	 * 		  {@link DigicoreVehicle} can be read.
	 * @param output the file to where the percentages, to 4 decimal places, 
	 * 		  will be written. The output file is a comma-separated value (CSV)
	 * 		  file with two columns: the first indicating the vehicle {@link Id}
	 * 		  and the second the percentage of that vehicle's activities, over
	 * 		  all activity chains, that are associated with a facility 
	 * 		  {@link Id}.
	 */
	public void runActivitiesWithFacilityIdAnalysis(List<File> vehicles, String output){
		LOG.info("Performing analysis on the percentage of activities with facility Ids (" + vehicles.size() + " vehicle files)");
		Counter counter = new Counter("   vehicles completed # ");
		List<Future<Tuple<Id, Double>>> listOfJobs = new ArrayList<Future<Tuple<Id,Double>>>();
		
		/* Execute the multi-threaded analysis. */
		for(File file : vehicles){
			ActivityWithFacilityIdCallable job = new ActivityWithFacilityIdCallable(file, counter);
			Future<Tuple<Id, Double>> result = this.threadExecutor.submit(job);
			listOfJobs.add(result);
		}
		
		this.threadExecutor.shutdown();
		while(!this.threadExecutor.isTerminated()){
		}
		counter.printCounter();
		
		/* Consolidate the output */
		Map<Id, Double> map = new TreeMap<Id, Double>();
		for(Future<Tuple<Id, Double>> job : listOfJobs){
			Tuple<Id, Double> tuple = null;
			try {
				tuple = job.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException("Could get the results to consolidate from the multi-threaded run.");
			} catch (ExecutionException e) {
				e.printStackTrace();
				throw new RuntimeException("Could get the results to consolidate from the multi-threaded run.");
			}
			map.put(tuple.getFirst(), tuple.getSecond());
		}
		
		/* Write the output to file. */
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		try{
			for(Id id : map.keySet()){
				bw.write(String.format("%s,%.4f\n", id.toString(), map.get(id)));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + output);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
		
		LOG.info("Done performing analysis on the percentage of activities with facility Ids.");
	}
	
}
