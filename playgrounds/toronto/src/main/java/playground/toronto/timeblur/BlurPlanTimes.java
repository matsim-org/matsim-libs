/* *********************************************************************** *
 * project: org.matsim.*
 * BlurPlanTimes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.toronto.timeblur;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;


public class BlurPlanTimes {

	private static void blurTAM(final String inputPlansFile, final String inputNetworkFile, final String outputPlansFile, int mutationRange) {
		System.out.println("running blurTAM...");
		System.out.println("  inputPlansFile:   "+inputPlansFile);
		System.out.println("  inputNetworkFile: "+inputNetworkFile);
		System.out.println("  outputPlansFile:  "+outputPlansFile);
		System.out.println("  mutationRange:    "+mutationRange);
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(inputNetworkFile);
		PopulationImpl population = (PopulationImpl) scenario.getPopulation();
		population.setIsStreaming(true);
		PersonBlurTimes pbt = new PersonBlurTimes(scenario.getConfig(), mutationRange);
		population.addAlgorithm(pbt);
		PopulationWriter pw = new PopulationWriter(population, network);
		pw.startStreaming(outputPlansFile);
		population.addAlgorithm(pw);
		new MatsimPopulationReader(scenario).readFile(inputPlansFile);
		population.printPlansCount();
		pw.closeStreaming();
		System.out.println("done.");
	}

	public static void blurUniformlyInTimeBin(final String inputPlansFile, final String inputNetworkFile, final String outputPlansFile, int binSize) {
		System.out.println("running blurUniformlyInTimeBin...");
		System.out.println("  inputPlansFile:   "+inputPlansFile);
		System.out.println("  inputNetworkFile: "+inputNetworkFile);
		System.out.println("  outputPlansFile:  "+outputPlansFile);
		System.out.println("  binSize:          "+binSize);
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(inputNetworkFile);
		PopulationImpl population = (PopulationImpl) scenario.getPopulation();
		population.setIsStreaming(true);
		PersonUniformBlurTimesPerTimeBin pubtptb = new PersonUniformBlurTimesPerTimeBin(binSize);
		population.addAlgorithm(pubtptb);
		PopulationWriter pw = new PopulationWriter(population, network);
		pw.startStreaming(outputPlansFile);
		population.addAlgorithm(pw);
		new MatsimPopulationReader(scenario).readFile(inputPlansFile);
		population.printPlansCount();
		pw.closeStreaming();
		System.out.println("done.");
	}

	public static void blurMutationRangeInTimeBin(final String inputPlansFile, final String inputNetworkFile, final String outputPlansFile, int mutationRange, int binSize) {
		System.out.println("running blurMutationRangeInTimeBin...");
		System.out.println("  inputPlansFile:   "+inputPlansFile);
		System.out.println("  inputNetworkFile: "+inputNetworkFile);
		System.out.println("  outputPlansFile:  "+outputPlansFile);
		System.out.println("  mutationRange:    "+mutationRange);
		System.out.println("  binSize:          "+binSize);
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(inputNetworkFile);
		PopulationImpl population = (PopulationImpl) scenario.getPopulation();
		population.setIsStreaming(true);
		PersonBlurTimesPerTimeBin pbtptb = new PersonBlurTimesPerTimeBin(mutationRange,binSize);
		population.addAlgorithm(pbtptb);
		PopulationWriter pw = new PopulationWriter(population, network);
		pw.startStreaming(outputPlansFile);
		population.addAlgorithm(pw);
		new MatsimPopulationReader(scenario).readFile(inputPlansFile);
		population.printPlansCount();
		pw.closeStreaming();
		System.out.println("done.");
	}

	//////////////////////////////////////////////////////////////////////

	private static void printUsage() {
		System.out.println();
		System.out.println("BlurPlanTimes");
		System.out.println();
		System.err.println("Usage: BlurPlanTimes [-m mutationRange | -t timeBinSize] inputPopulationFile inputNetworkFile outputPopulationFile");
		System.err.println();
		System.err.println("options:");
		System.err.println("  none:             mutates departure times with TAM while 'mutationRange' = 1800 seconds");
		System.err.println("  -m mutationRange: mutates departure times with TAM with given 'mutationRange' ([1..n] seconds)");
		System.err.println("  -t timeBinSize:   mutates all given departure times within the same 'timeBinSize' ([1..n] seconds) uniformly");
		System.err.println("If both options are given, the departure times will be mutated according to TAM but within the same 'timeBinSize'");
		System.err.println();
		System.out.println("---------------------");
		System.out.println("2009, matsim.org");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {
		if (args.length == 3) {
			blurTAM(args[0],args[1],args[2],1800);
		}
		else if (args.length == 7) {
			int mutationRange; int timeBinSize;
			if (args[0].equals("-m") && args[2].equals("-t")) {
				mutationRange = Integer.parseInt(args[1]);
				timeBinSize = Integer.parseInt(args[3]);
			}
			else if (args[0].equals("-t") && args[2].equals("-m")) {
				timeBinSize = Integer.parseInt(args[1]);
				mutationRange = Integer.parseInt(args[3]);
			}
			else { printUsage(); return; }
			blurMutationRangeInTimeBin(args[4],args[5],args[6],mutationRange,timeBinSize);
		}
		else if (args.length == 5) {
			if (args[0].equals("-m")) {
				int mutationRange = Integer.parseInt(args[1]);
				blurTAM(args[2],args[3],args[4],mutationRange);
			}
			else if (args[0].equals("-t")) {
				int timeBinSize = Integer.parseInt(args[1]);
				blurUniformlyInTimeBin(args[2],args[3],args[4],timeBinSize);
			}
			else { printUsage(); return; }
		}
		else { printUsage(); return; }
	}
}
