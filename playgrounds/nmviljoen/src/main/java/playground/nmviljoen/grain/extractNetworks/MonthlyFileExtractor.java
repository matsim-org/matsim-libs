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
import java.util.List;

import org.apache.log4j.Logger;

import playground.nmviljoen.grain.GrainUtils;
import playground.southafrica.freight.digicore.extract.step3_extract.MyMultiThreadChainExtractor;
import playground.southafrica.utilities.Header;

/**
 * Extracting the activity chains for a set of available and sorted monthly 
 * vehicle files, applying the extraction procedures of 
 * {@link MyMultiThreadChainExtractor} to each folder.
 * 
 * @author jwjoubert
 */
public class MonthlyFileExtractor {
	final private static Logger LOG = Logger.getLogger(MonthlyFileExtractor.class);

	/**
	 * Executing the vehicle activity chain extractor for the 12 months March 
	 * 2013 to February 2014.
	 *  
	 * @param args the following (all required) arguments, in this sequence:
	 * <ol>
	 * 		<li> the folder containing the processed monthly folders; and
	 * 		<li> the number of threads over which the job will be spread. 
	 * </ol>
	 */
	public static void main(String[] args) {
		Header.printHeader(MonthlyFileExtractor.class.toString(), args);
		
		String processedFolder = args[0];
		int numberOfThreads = Integer.parseInt(args[1]);
		String vehicleStatusFile = args[2];
		
		List<File> inputFiles = GrainUtils.getVehicleFolders(processedFolder);
		for(File month : inputFiles){
			LOG.info("====> Processing month " + month.getAbsolutePath());
			
			/* Create the output folder. */
			String outputFolder = month.getParentFile() + (month.getParentFile().getAbsolutePath().endsWith("/") ? "" : "/") + "xml/";
			boolean created = new File(outputFolder).mkdirs();
			if(!created){
				LOG.error("Could not create the output folder " + outputFolder);
				LOG.error("====> Skipping the month.");
			} else{
				String[] sa = {
						month.getAbsolutePath(),
						vehicleStatusFile,
						outputFolder,
						String.valueOf(numberOfThreads),
						String.valueOf(60*60*5),			/* 5 hours. */
						String.valueOf(60),					/* 60 seconds */
						"WGS84_SA_Albers"
				};
				
				MyMultiThreadChainExtractor.main(sa);
				LOG.info("====> Done processing month.");
			}
		}
		
		Header.printFooter();
	}

}
