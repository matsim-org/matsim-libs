/* *********************************************************************** *
 * project: org.matsim.*
 * Phase2ActivityTypeAdjuster.java
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
package playground.southafrica.projects.erAfrica;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.southafrica.population.census2011.capeTown.CapeTownScenarioCleaner;
import playground.southafrica.population.utilities.activityTypeManipulation.CapeTownActivityTypeManipulator_2017;
import playground.southafrica.utilities.Header;

/**
 * Once the population is generated using {@link Phase1PopulationBuilder} and 
 * the activity durations were analysed in R, this class adjusts the activity
 * types, and generates a {@link Config} object.
 * 
 * This class requires that a network file has also been copied into the root
 * folder.
 * 
 * @author jwjoubert
 */
public class Phase2ActivityTypeAdjuster {
	final private static Logger LOG = Logger.getLogger(Phase2ActivityTypeAdjuster.class);

	/**
	 * Executing the final activity type adjustment. If the folder structure 
	 * have not been tempered with, this method only needs to be given the root
	 * folder.
	 *  
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(Phase2ActivityTypeAdjuster.class.toString(), args);
		String root = args[0];
		root += root.endsWith("/") ? "" : "/";
		
		/* Do the magic. */
		step1ActivtyTypeAdjustment(root);
		step2CleanupScenario(root);
		
		Header.printFooter();
	}
	
	public static void step1ActivtyTypeAdjustment(String root){
		LOG.info("Adjusting the activity types...");
		String[] typeArgs = {
				root + "persons.xml.gz",
				root + "durations/decilesPersons.csv",
				root + "persons.xml.gz",
				root + "commercial.xml.gz",
				root + "durations/decilesFreight.csv",
				root + "commercial.xml.gz",
				root + "config.xml.gz"
		};
		CapeTownActivityTypeManipulator_2017.run(typeArgs);
		LOG.info("Done adjusting types.");
	}
	
	public static void step2CleanupScenario(String root){
		LOG.info("Cleaning up the scenario...");
		String[] cleanupArgs = {
				root,
				TransformationFactory.HARTEBEESTHOEK94_LO19,
				TransformationFactory.HARTEBEESTHOEK94_LO19,
				TransformationFactory.HARTEBEESTHOEK94_LO19,
				root + "network.xml.gz"
		};
		CapeTownScenarioCleaner.run(cleanupArgs);
		LOG.info("Done cleaning up.");
	}
}
