/* *********************************************************************** *
 * project: org.matsim.*
 * BuildCapeTownPopulation.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.southafrica.projects.erAfrica;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * This class tries to, in a repeatable and reproducible manner, generate an
 * entire synthetic population for the City of Cape Town. The idea is that this
 * is a single-run process with no manual intervention.
 * 
 * FIXME This is (possibly) still called Phase1PopulationBuilder in the R 
 * documentation.
 * 
 * @author jwjoubert
 */
public class CapeTownPopulationBuilder {
	final private static Logger LOG = Logger.getLogger(CapeTownPopulationBuilder.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(CapeTownPopulationBuilder.class.toString(), args);

		String censusPopulationFolder = args[0]; /* This should be in WGS84_SA_Albers. */
		String outputFolder = args[1];
		outputFolder += outputFolder.endsWith("/") ? "" : "/";

		setup(outputFolder, censusPopulationFolder);

		Header.printFooter();
	}



	/**
	 * Ensure there is a tmp folder for the intermediary file, and it only 
	 * contains the original, unedited population.  
	 */
	private static void setup(String outputFolder, String censusFolder){
		/* Empty and create a temporary folder. */
		File tmpFolder = new File(outputFolder + "tmp/");
		if(tmpFolder.exists()){
			LOG.warn("Temporary folder will be deleted!");
			LOG.warn("Deleting " + tmpFolder.getAbsolutePath());
			FileUtils.delete(tmpFolder);
		}
		tmpFolder.mkdirs();

		/* Copy the population from then Treasury 2014 data. */
		try {
			FileUtils.copyFile(
					new File(censusFolder + "population_withPlans.xml.gz"), 
					new File(outputFolder + "tmp/persons.xml.gz"));
			FileUtils.copyFile(
					new File(censusFolder + "populationAttributes.xml.gz"), 
					new File(outputFolder + "tmp/personAttributes.xml.gz"));
			FileUtils.copyFile(
					new File(censusFolder + "households.xml.gz"), 
					new File(outputFolder + "tmp/households.xml.gz"));
			FileUtils.copyFile(
					new File(censusFolder + "householdAttributes_withPlanHome.xml.gz"), 
					new File(outputFolder + "tmp/householdAttributes.xml.gz"));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot copy base treasure files.");
		}
	}

}
