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
import playground.southafrica.freight.digicore.utils.DigicoreChainCleaner;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetworkBuilder;
import playground.southafrica.utilities.Header;

/**
 * Building a path-dependent complex network from Digicore activity chains
 * after they were cleaned using {@link DigicoreChainCleaner}. For each of
 * the 12 months this class executes an instance of {@link PathDependentNetworkBuilder}.
 * 
 * @author jwjoubert
 */
public class MonthlyFilePathDependentNetworkBuilder {
	final private static Logger LOG = Logger.getLogger(MonthlyFilePathDependentNetworkBuilder.class);

	/**
	 * Executing the path dependent network building for the 12 months March 
	 * 2013 to February 2014.
	 *  
	 * @param args the following (all required) arguments, in this sequence:
	 * <ol>
	 * 		<li> the folder containing the processed monthly folders.
	 * </ol>
	 */
	public static void main(String[] args) {
		Header.printHeader(MonthlyFilePathDependentNetworkBuilder.class.toString(), args);
		
		String processedFolder = args[0];

		List<File> inputFiles = GrainUtils.getMonthlyOutputFolders(processedFolder);
		for(File month : inputFiles){
			LOG.info("====> Processing month " + month.getAbsolutePath());
			
			String thisMonthCleanXmlFolder = month.getAbsolutePath() 
					+ (month.getAbsolutePath().endsWith("/") ? "" : "/") 
					+ "20_20/xml2/clean/";
			String thisMonthNetworkFile = month.getAbsolutePath() 
					+ (month.getAbsolutePath().endsWith("/") ? "" : "/") 
					+ "20_20/20_20_pathDependentNetwork.xml.gz";
			
			String[] sa = {
					thisMonthCleanXmlFolder,
					thisMonthNetworkFile,
					"Longitudinal data for GrainBiz using 20_20"
			};
			
			PathDependentNetworkBuilder.main(sa);
			LOG.info("====> Done processing month.");
		}
		
		Header.printFooter();
	}

}
