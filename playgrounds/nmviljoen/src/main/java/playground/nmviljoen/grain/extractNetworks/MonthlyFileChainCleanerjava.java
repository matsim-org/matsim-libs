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
import playground.southafrica.freight.digicore.analysis.postClustering.ClusteredChainGenerator;
import playground.southafrica.freight.digicore.utils.DigicoreChainCleaner;
import playground.southafrica.utilities.Header;

/**
 * Cleaning the monthly activity chains by running {@link DigicoreChainCleaner} 
 * on each month's <code>./xml2/</code> activity chains resulting from running
 * {@link ClusteredChainGenerator}.
 * 
 * @author jwjoubert
 */
public class MonthlyFileChainCleanerjava {
	final private static Logger LOG = Logger.getLogger(MonthlyFileChainCleanerjava.class);

	/**
	 * Executing the vehicle activity clustering for the 12 months March 
	 * 2013 to February 2014.
	 *  
	 * @param args the following (all required) arguments, in this sequence:
	 * <ol>
	 * 		<li> the folder containing the processed monthly folders;
	 * 		<li> the number of threads over which the job will be spread. 
	 * </ol>
	 */
	public static void main(String[] args) {
		Header.printHeader(MonthlyFileChainCleanerjava.class.toString(), args);
		
		String processedFolder = args[0];
		int numberOfThreads = Integer.parseInt(args[1]);

		List<File> inputFiles = GrainUtils.getMonthlyOutputFolders(processedFolder);
		for(File month : inputFiles){
			LOG.info("====> Processing month " + month.getAbsolutePath());
			
			String thisXmlFolder = month.getAbsolutePath() 
					+ (month.getAbsolutePath().endsWith("/") ? "" : "/") 
					+ "20_20/xml2/";

			String[] sa = {
					thisXmlFolder,
					String.valueOf(numberOfThreads)
			};
			
			DigicoreChainCleaner.main(sa);
			LOG.info("====> Done processing month.");
		}
		
		Header.printFooter();
	}

}
