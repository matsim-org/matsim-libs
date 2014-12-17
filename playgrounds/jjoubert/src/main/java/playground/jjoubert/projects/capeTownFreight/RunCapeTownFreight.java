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
package playground.jjoubert.projects.capeTownFreight;

import java.io.File;

import org.apache.log4j.Logger;

import playground.southafrica.population.freight.RunNationalFreight;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * Executes a single run of the Cape Town freight population.
 *
 * @author jwjoubert
 */
public class RunCapeTownFreight {
	final private static Logger LOG = Logger.getLogger(RunCapeTownFreight.class);

	public static void main(String[] args) {
		Header.printHeader(RunCapeTownFreight.class.toString(), args);
		
		String workspace = args[0] + (args[0].endsWith("/") ? "" : "/");
		
		String network = workspace + "coct-data/matsim/CapeTown_full.xml.gz";
		String population = workspace + "coct-data/matsim/capeTownCommercial.xml.gz";
		String populationAttributes = workspace + "coct-data/matsim/capeTownCommercialAttributes.xml.gz";
		String outputFolder = workspace + "coct-data/matsim/output/";
		String pathDependentNetwork = workspace + "coct-data/matsim/pathDependentNetwork.xml.gz";
		
		/* Check if output folder exists, and DELETE if it is there. */
		File f = new File(outputFolder);
		if(f.exists() && f.isDirectory()){
			LOG.warn("Deleting the output folder " + outputFolder);
			FileUtils.delete(f);
		}
		
		String[] setup = {network, population, populationAttributes, outputFolder, pathDependentNetwork};
		
		LOG.info("Running the class playground.southafrica.population.freight.RunNationalFreight.java");
		RunNationalFreight.main(setup);
		
		Header.printFooter();
	}

}
