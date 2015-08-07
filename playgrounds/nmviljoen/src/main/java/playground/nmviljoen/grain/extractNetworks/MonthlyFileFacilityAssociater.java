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
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import playground.nmviljoen.grain.GrainUtils;
import playground.southafrica.freight.digicore.analysis.postClustering.ClusteredChainGenerator;
import playground.southafrica.utilities.Header;

/**
 * Associating the clustered facilities resulting from {@link MonthlyFileClusterer}
 * with the activities in the activity chains.
 * 
 * @author jwjoubert
 */
public class MonthlyFileFacilityAssociater {
	final private static Logger LOG = Logger.getLogger(MonthlyFileFacilityAssociater.class);

	/**
	 * Executing the vehicle activity clustering for the 12 months March 
	 * 2013 to February 2014.
	 *  
	 * @param args the following (all required) arguments, in this sequence:
	 * <ol>
	 * 		<li> the folder containing the processed monthly folders;
	 * 		<li> the shapefile within which activities will be clustered. Activities
	 * 			 outside the shapefile are ignored. NOTE: It is actually recommended
	 * 			 that smaller demarcation areas, such as the Geospatial Analysis 
	 * 			 Platform (GAP) zones, be used.
	 * 		<li> field of the shapefile that will be used as identifier; and
	 * 		<li> the number of threads over which the job will be spread. 
	 * </ol>
	 */
	public static void main(String[] args) {
		Header.printHeader(MonthlyFileFacilityAssociater.class.toString(), args);
		
		String processedFolder = args[0];
		String shapefile = args[1];
		int idField = Integer.parseInt(args[2]);
		int numberOfThreads = Integer.parseInt(args[3]);

		List<File> inputFiles = GrainUtils.getMonthlyOutputFolders(processedFolder);
		for(File month : inputFiles){
			LOG.info("====> Processing month " + month.getAbsolutePath());
			
			String thisXmlFolder = month.getAbsolutePath() + (month.getAbsolutePath().endsWith("/") ? "" : "/") + "xml/";
			String thisOutputFolder = month.getAbsolutePath() + (month.getAbsolutePath().endsWith("/") ? "" : "/");

			String[] sa = {
					thisXmlFolder,
					thisOutputFolder,
					String.valueOf(numberOfThreads),
					shapefile,
					String.valueOf(idField),
			};

			try {
				ClusteredChainGenerator.main(sa);
			} catch (IOException e) {
				e.printStackTrace();
				LOG.error("Could not complete month!!");
			}
			LOG.info("====> Done processing month.");
		}
		
		Header.printFooter();
	}

}
