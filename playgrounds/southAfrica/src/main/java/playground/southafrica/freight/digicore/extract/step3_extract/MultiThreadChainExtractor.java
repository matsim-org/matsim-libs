/* *********************************************************************** *
 * project: org.matsim.*
 * MyMultiThreadChainExtractor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.southafrica.freight.digicore.extract.step3_extract;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

public class MultiThreadChainExtractor {
	private final static Logger log = Logger.getLogger(MultiThreadChainExtractor.class);
	private final ExecutorService threadExecutor;

	/**
	 * @param args the string arguments in the following order:
	 * <ol>
	 * 	<li> root directory for original VehicleFiles (".txt.gz");
	 * 	<li> absolute path of file with On and off statuses;
	 * 	<li> absolute path of XML (output) folder;
	 * 	<li> number of threads;
	 * 	<li> threshold (seconds) to distinguish between minor and major 
	 * 		 activities (typically 5 hours, or 18000 sec, according to Joubert
	 * 		 and Axhausen (2011) );
	 * 	<li> threshold (seconds) of minimum activity duration (we used 60 seconds);
	 * 	<li> coordinate reference system, typically "WGS84_SA_Albers" for South 
	 * 		 Africa (as captured in {@link MGC}).
	 * </ol>
	 */
	public static void main(String[] args) {
		Header.printHeader(MultiThreadChainExtractor.class.toString(), args);
		
		log.info("===============================================================================");
		log.info(" Extracting activity chains from sorted Digicore vehicle files.");
		log.info("-------------------------------------------------------------------------------");

		String inputFolderName = args[0];
		String statusFilename = args[1];
		String outputFolderName = args[2];
		String threads = args[3];
		String thresholdMinorMajor = args[4];
		String thresholdActivity = args[5];
		
		String crs = args[6];
		if(!crs.equalsIgnoreCase(TransformationFactory.HARTEBEESTHOEK94_LO29)){
			log.warn("Expected " + TransformationFactory.HARTEBEESTHOEK94_LO29 + " as coordinate reference system.");
		}

		/* Check that file folder exists and is readable. */
		File folder = new File(inputFolderName);
		if(!folder.canRead() || !folder.isDirectory()){
			throw new RuntimeException("Cannot read from " + folder.getAbsolutePath());
		}
		log.info("      Vehicle file folder: " + folder.getAbsolutePath());
		
		/* Check that output folder exists and is writable. */
		File outputFolder = new File(outputFolderName);
		if(!outputFolder.canWrite() || !outputFolder.isDirectory()){
			throw new RuntimeException("Cannot write to " + outputFolder.getAbsolutePath());			
		}
		
		/* Sample all sorted files from given folder. */
		List<File> fileList = FileUtils.sampleFiles(folder, Integer.MAX_VALUE, FileUtils.getFileFilter(".txt.gz"));
		log.info("          Number of files: " + fileList.size());
		log.info(" Number of threads to use: " + threads);
		log.info("-------------------------------------------------------------------------------");
		
		/* Read the ignition-ON and -OFF statuses from file */
		DigicoreStatusReader dsr = null;
		try {
			dsr = new DigicoreStatusReader(statusFilename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		/* Set up counter. */
		Counter threadCounter = new Counter("Vehicles processed: ");
		
		/* Create extractor, and assign each vehicle file to the thread pool. */
		MultiThreadChainExtractor extractorExecutor = new MultiThreadChainExtractor(Integer.parseInt(threads));
		for(File file : fileList){
			DigicoreChainExtractor dce = new DigicoreChainExtractor(
					file, 
					outputFolder, 
					Double.parseDouble(thresholdMinorMajor), 
					Double.parseDouble(thresholdActivity), 
					dsr.getStartSignals(), 
					dsr.getStopSignals(), 
					crs, 
					threadCounter);
			extractorExecutor.threadExecutor.execute(dce);
		}
		extractorExecutor.threadExecutor.shutdown();
		while(!extractorExecutor.threadExecutor.isTerminated()){
		}
		threadCounter.printCounter();
		Header.printFooter();
	}
	
	public MultiThreadChainExtractor(int nThreads){
		threadExecutor = Executors.newFixedThreadPool(nThreads);
		
	}

}

