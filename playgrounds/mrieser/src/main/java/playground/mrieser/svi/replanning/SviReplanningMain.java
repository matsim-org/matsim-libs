/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.svi.replanning;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author mrieser
 */
public class SviReplanningMain {

	private static void printUsage() {
		System.out.println("SviReplanning inPopulation outPopulation zonesDescription inMatrices outMatrices");
		System.out.println("");
		System.out.println("Arguments:");
		System.out.println("  inPopulation:     an existing MATSim plans/population file used as input.");
		System.out.println("  outPopulation:    filename of a not yet existing file where the modified");
		System.out.println("                    MATSim population is written to.");
		System.out.println("  zonesDescription: filename of an existing ESRI Shape file containing the");
		System.out.println("                    zones used to assign coordinates to zones.");
		System.out.println("  inMatrices:       name of an existing directory where matrices");
		System.out.println("                    containing travel times and other data can be found.");
		System.out.println("  outMatrices:      name of a directory where new travel demand matrices");
		System.out.println("                    are written to.");
	}

	public static void main(final String[] args) {
		if (args.length != 5) {
			printUsage();
			return;
		}

		String inputPopulationFilename = args[0];
		String outputPopulationFilename = args[1];
		String zonesFilename = args[2]; // not yet clear which format: object-attributes-lookup, or shape-file?
		String inputMatricesDirname = args[3];
		String outputMatricesDirname = args[4];

		if (inputPopulationFilename.equals(outputPopulationFilename)) {
			System.err.println("Input and Output population file must be different.");
			return;
		}

		File inputPopulationFile = new File(inputPopulationFilename);
		File outputPopulationFile = new File(outputPopulationFilename);
		File zonesFile = new File(zonesFilename);

		if (!inputPopulationFile.exists()) {
			System.err.println("Input population file does not exist.");
			return;
		}
		if (outputPopulationFile.exists()) {
			System.err.println("Output population file already exists. Will NOT overwrite it. Aborting.");
			return;
		}
		if (!zonesFile.exists()) {
			System.err.println("zones file does not exist.");
			return;
		}

		// read zones description
		// xTODO [MR] not yet clear which format: object-attributes-lookup, or shape-file?
		/* attributes-format preferred, because more efficient, but unclear how to store
		 * variable-length information (varying number of acts per plan/person) with single
		 * lookup (only person-id given). Shape-file results in cleaner, but slower, process,
		 * when zone-relation needs to be calculated every time again.
		 *
		 * for the moment, stay with the slower but cleaner shape files
		 */
		ShapeFileReader shpReader = new ShapeFileReader();
		shpReader.readFileAndInitialize(zonesFilename);

		// read matrices
		// TODO [MR] need description of format

		// stream and replan population
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationImpl pop = (PopulationImpl) scenario.getPopulation();
		pop.setIsStreaming(true);

//		SviReplanner replanner = new SviReplanner();
//		DynamicODDemandCollector ods = new DynamicODDemandCollector();
//		pop.addAlgorithm(replanner);
//		pop.addAlgorithm(ods);

		new MatsimPopulationReader(scenario).parse(inputPopulationFilename);

		// write new demand matrices
		// TODO [MR] need description of format

		// finish
		System.out.println("All done.");
	}
}
