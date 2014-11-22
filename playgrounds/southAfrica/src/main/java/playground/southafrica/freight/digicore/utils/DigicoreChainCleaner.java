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

/**
 * 
 */
package playground.southafrica.freight.digicore.utils;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.analysis.postClustering.ClusteredChainGenerator;
import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;
import playground.southafrica.freight.digicore.io.DigicoreVehicleWriter;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * Class to read all the {@link DigicoreVehicle}s in a given folder, and adapt
 * all the vehicle's activity chains ({@link DigicoreChain}s) by merging all
 * consecutive activities ({@link DigicoreActivity}) that occur at the same 
 * facility, i.e. having the same facility {@link Id}. The output is written to
 * folder called 'clean/' inside the given folder containing the vehicle files.
 * 
 * <br><br><b>Note:</b> Vehicle activities will only be associated with facilities
 * if the class {@link ClusteredChainGenerator} has been executed. That is, this
 * class must be run on the output folder, typically called 'xml2', which in turn
 * is produced by the class {@link ClusteredChainGenerator}.
 * 
 * @author jwjoubert
 */
public class DigicoreChainCleaner {
	final private static Logger LOG = Logger.getLogger(DigicoreChainCleaner.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(DigicoreChainCleaner.class.toString(), args);
		String xmlFolder = args[0];
		int numberOfThreads = Integer.parseInt(args[1]);		
		
		/* Check the output folder is empty. */
		File outputFolder = new File(xmlFolder + (xmlFolder.endsWith("/") ? "" : "/") + "clean/");
		if(outputFolder.exists()){
			LOG.warn("The output folder exists and will be overwritten: " + outputFolder.getAbsolutePath());
			FileUtils.delete(outputFolder);
		}
		outputFolder.mkdirs();
		
		/* Get vehicle files. */
		List<File> vehicleFiles = FileUtils.sampleFiles(new File(xmlFolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));
		
		/* Execute the multi-threaded jobs */
		ExecutorService threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
		Counter threadCounter = new Counter("   vehicles completed: ");
		
		for(File vehicleFile : vehicleFiles){
			RunnableChainCleaner rcr = new RunnableChainCleaner(vehicleFile, threadCounter, outputFolder);
			threadExecutor.execute(rcr);
		}
		
		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}
		threadCounter.printCounter();
		
		Header.printFooter();
	}
	
	
	public static class RunnableChainCleaner implements Runnable {
		private final File vehicleFile;
		private Counter counter;
		private String outputFolder;
		private int numberOfActivitiesChanged = 0;
		
		public RunnableChainCleaner(File vehicleFile, Counter threadCounter, File outputFolder) {
			this.vehicleFile = vehicleFile;
			this.counter = threadCounter;
			this.outputFolder = outputFolder.getAbsolutePath();
		}

		public void run() {
			/* Read the vehicle from file. */
			DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
			dvr.parse(vehicleFile.getAbsolutePath());
			DigicoreVehicle dv = dvr.getVehicle();
			
			int chainIndex = 0;
			while(chainIndex < dv.getChains().size()){
				DigicoreChain chain = dv.getChains().get(chainIndex);
				
				cleanChain(chain);
				
				if(chain.size() < 2){
					/* It no longer is a valid chain, remove it. */
					dv.getChains().remove(chainIndex);
				} else{
					/* Move to the next chain. */
					chainIndex++;
				}
			}
			
			/* Write the vehicle to file, if it has at least one chain. */
			if(dv.getChains().size() > 0){
				DigicoreVehicleWriter dvw = new DigicoreVehicleWriter();
				dvw.write(outputFolder + "/" + dv.getId() + ".xml.gz",  dv);

				/*TODO Remove after debugging. */
				LOG.info("   ==> " + dv.getId().toString() + " -> " + numberOfActivitiesChanged);
			}
			
			counter.incCounter();
		}
		
		public DigicoreChain cleanChain(DigicoreChain chain){
			int activityIndex = 0;
			while(activityIndex < chain.getAllActivities().size()-1){
				DigicoreActivity thisActivity = chain.get(activityIndex);
				DigicoreActivity nextActivity = chain.get(activityIndex+1);
				
				if( thisActivity.getFacilityId() != null &&
						nextActivity.getFacilityId() != null &&
						thisActivity.getFacilityId().toString().equalsIgnoreCase(nextActivity.getFacilityId().toString()) ){
					/* Merge the two activities. */
					numberOfActivitiesChanged++;
					thisActivity.setEndTime( nextActivity.getEndTime() );
					chain.remove( activityIndex+1 );						
				} else{
					activityIndex++;
				}
			}
			return chain;
		}
	} 
	
	

}
