/* *********************************************************************** *
 * project: org.matsim.*
 * TurnkeyExtractor.java
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

/**
 * 
 */
package playground.southafrica.freight.digicore.extract;

import java.io.File;

import org.apache.log4j.Logger;

import playground.southafrica.freight.digicore.extract.step1_split.DigicoreFileSplitter;
import playground.southafrica.freight.digicore.extract.step2_sort.DigicoreFilesSorter;
import playground.southafrica.freight.digicore.extract.step3_extract.MyMultiThreadChainExtractor;
import playground.southafrica.freight.digicore.extract.step4_collate.DigicoreVehicleCollator;
import playground.southafrica.utilities.Header;

/**
 * Class to perform all three extraction phases for a given month:
 * <ol>
 * 		<li> split the raw data into unique vehicle files;
 * 		<li> sorting the records chronologically;
 * 		<li> extracting the activity chains; and finally
 * 		<li> combine them into a single container.
 * </ol> 
 * 
 * @author jwjoubert
 */
public class TurnkeyExtractor {
	final private static Logger LOG = Logger.getLogger(TurnkeyExtractor.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(TurnkeyExtractor.class.toString(), args);
		
		String inputFile = args[0];
		String outputFolder = args[1];
		outputFolder += outputFolder.endsWith("/") ? "" : "/";
		
		TurnkeyExtractor.extract(inputFile, outputFolder);
		
		Header.printFooter();
	}

	private TurnkeyExtractor() {
		/* Hide constructor. */
	}
	
	private static void extract(String inputFile, String outputFolder){
		LOG.info("Executing the turnkey extraction... this may take some time.");

		/* Splitting */
		String[] splitArgs = {inputFile, outputFolder, "2", "5", "0", "1", "2", "4", "3"};
		DigicoreFileSplitter.main(splitArgs );
		
		/* Sorting */
		String[] sortArgs = {outputFolder + "Vehicles/"};
		DigicoreFilesSorter.main(sortArgs);
		
		/* Extracting */
		boolean createdXmlFolder = new File(outputFolder + "xml/").mkdirs();
		if(!createdXmlFolder){
			LOG.error("Could not create the ./xml/ folder.");
		}
		String[] extractArgs = {
				outputFolder + "Vehicles/",
				"/home/share/data/digicore/2009/status.txt",
				outputFolder + "xml/",
				"40",
				"18000",
				"60",
				"WGS84_SA_Albers"};
		MyMultiThreadChainExtractor.main(extractArgs);
		
		/* Collating */
		String[] collateArgs = {outputFolder + "xml/", outputFolder + "digicoreVehicles.xml.gz", "WGS84_SA_Albers", "true"};
		DigicoreVehicleCollator.main(collateArgs);
		
		LOG.info("Done with the turnkey extraction.");
	}
}
