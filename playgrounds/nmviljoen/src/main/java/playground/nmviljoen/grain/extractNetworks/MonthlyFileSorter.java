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
import playground.southafrica.freight.digicore.extract.step2_sort.DigicoreFilesSorter;
import playground.southafrica.utilities.Header;

/**
 * Sorting a set of available vehicle files after they've been split using
 * {@link DigicoreFileSplitter}. Each of the months should have a folder 
 * containing unsorted vehicle files, and each of these will be sorted using
 * {@link DigicoreFilesSorter}.
 * 
 * @author jwjoubert
 */
public class MonthlyFileSorter {
	final private static Logger LOG = Logger.getLogger(MonthlyFileSorter.class);

	/**
	 * Executing the vehicle sorter for the 12 months March 2013 to February 2014.
	 *  
	 * @param args the following (all required) arguments, in this sequence:
	 * <ol>
	 * 		<li> the base folder containing the processed monthly folders.
	 * </ol>
	 */
	public static void main(String[] args) {
		Header.printHeader(MonthlyFileSorter.class.toString(), args);
		
		String processedFolder = args[0];
		int numberOfThreads = Integer.parseInt(args[1]);

		/* Set up multi-threaded infrastructure. */
		ExecutorService threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
		List<MonthSorter> jobs = new ArrayList<MonthSorter>();

		List<File> inputFolders = GrainUtils.getVehicleFolders(processedFolder);
		for(File month : inputFolders){
			LOG.info("------>  Sorting " + month.getAbsolutePath());
			/* Submit the job to be executed. */
			MonthSorter job = new MonthSorter(month.getAbsolutePath());
			threadExecutor.execute(job);
			jobs.add(job);
			LOG.info("------>  Done sorting ");
		}

		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}

		Header.printFooter();
	}

	
	private static class MonthSorter implements Runnable{
		private final String folder;
		
		public MonthSorter(String folder) {
			this.folder = folder;
		}

		@Override
		public void run() {
			DigicoreFilesSorter dfs = new DigicoreFilesSorter(this.folder);
			dfs.sortVehicleFiles();
		}
	}
	
}
