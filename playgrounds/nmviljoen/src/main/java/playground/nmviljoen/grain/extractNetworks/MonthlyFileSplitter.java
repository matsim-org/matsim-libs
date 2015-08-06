/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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
/**
 * 
 */
package playground.nmviljoen.grain.extractNetworks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import playground.nmviljoen.grain.GrainUtils;
import playground.southafrica.freight.digicore.extract.step1_split.DigicoreFileSplitter;
import playground.southafrica.utilities.Header;

/**
 * Split a set of available monthly files, applying the extraction procedures 
 * of {@link DigicoreFileSplitter} to each file.
 * 
 * @author jwjoubert
 */
public class MonthlyFileSplitter {
	final private static Logger LOG = Logger.getLogger(MonthlyFileSplitter.class);

	/**
	 * Executing the vehicle extractor for the 12 months March 2013 to 
	 * February 2014.
	 *  
	 * @param args the following (all required) arguments, in this sequence:
	 * <ol>
	 * 		<li> the folder containing the raw monthly GPS trace files; and
	 * 		<li> the folder where the different monthly output folders, each
	 * 			 containing a <code>Vehicles/</code> folder, will be created.
	 * 		<li> the number of threads over which the job will be spread. Each
	 * 			 month will be assigned to a thread. 
	 * </ol>
	 */
	public static void main(String[] args) {
		Header.printHeader(MonthlyFileSplitter.class.toString(), args);
		
		String rawFolder = args[0];
		String outputFolder = args[1];
		int numberOfThreads = Integer.parseInt(args[2]);
		
		/* Set up multi-threaded infrastructure. */
		ExecutorService threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
		List<MonthSplitter> jobs = new ArrayList<MonthlyFileSplitter.MonthSplitter>();
		
		List<File> inputFiles = GrainUtils.getRawGpsTraceFiles(rawFolder);
		for(File month : inputFiles){
			/* Extract the specific month, and generate it's output folder.
			 * Note: If it already exists, it will not first be DELETED, but 
			 * an error comment is written to the log file, and the month is
			 * skipped. */
			String theMonth = month.getName().substring(0, month.getName().indexOf("."));
			LOG.info("Processing month: " + theMonth);
			String monthFolder = outputFolder + (outputFolder.endsWith("/") ? "" : "/") + theMonth + "/";
			File thisMonthFolder = new File(monthFolder);
			if(thisMonthFolder.isDirectory() && thisMonthFolder.exists()){
				LOG.error("Not allowed to delete the output folder " + monthFolder);
				LOG.error("The month " + theMonth + " will be skipped!!");
			} else{
				/* Create the folder for the month's output. */
				boolean dir = thisMonthFolder.mkdirs();
				if(!dir){
					LOG.error("Could not create the month " + theMonth + "'s folder!!");
					LOG.error("The month " + theMonth + " will be skipped!!");
				} else{
					/* Run the Digicore file splitter... and hold your breath! */
					String[] sa = {
							month.getAbsolutePath(), 	// Input file.
							monthFolder, 				// Folder where output is written.
							"0", 						// The line number where we start to read the input file.
							"5", 						// Field number: Vehicle Id.
							"0", 						// Field number: Time stamp.
							"2",						// Field number: Longitude.
							"1",						// Field number: Latitude. 
							"4",						// Field number: Ignition status.
							"3"};						// Field number: Speed.
					
					/* Submit the job to be executed. */
					MonthSplitter job = new MonthSplitter(sa);
					threadExecutor.execute(job);
					jobs.add(job);
				}
			}
		}
		
		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}

		Header.printFooter();
	}

	/**
	 * Class to handle the multi-threaded execution of splitting a raw GPS 
	 * trace file into its individual vehicle files.
	 * 
	 * @author jwjoubert
	 */
	private static class MonthSplitter implements Runnable{
		private String[] arguments;
		
		public MonthSplitter(String[] arguments) {
			this.arguments = arguments;
		}

		@Override
		public void run() {
			DigicoreFileSplitter.main(arguments);
		}
	}

}
