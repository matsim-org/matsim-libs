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

package org.matsim.contrib.minibus.integration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.hook.PModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Integration test of the minibus package
 *
 * @author aneumann
 */
public class PControlerTestIT implements TabularFileHandler{

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	private final ArrayList<String[]> pStatsResults = new ArrayList<>();


	@Test
	final void testPControler() {

		final String scenarioName = "corr_s1_0";
		final int numberOfIterations = 10;

		final String inputPath = utils.getClassInputDirectory();
		final String outputPath = utils.getOutputDirectory() + scenarioName + "/";

		final String configFile = inputPath + "config_" + scenarioName + ".xml";

		// ---

		Config config = ConfigUtils.loadConfig(configFile, new PConfigGroup());

		config.controller().setLastIteration(numberOfIterations);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setCreateGraphs(false);
		config.controller().setOutputDirectory(outputPath);

		// ---

		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);

		// ---

		Controler controler = new Controler(scenario);

		// manipulate config
		controler.addOverridingModule(new PModule()) ;

		//		services.setScoringFunctionFactory(new BvgScoringFunctionFactory(services.getConfig().planCalcScore(), new BvgScoringFunctionConfigGroup(services.getConfig()), services.getNetwork()));
		// looks like the above was removed in head but I had a merge conflict.  kai, sep'14

		controler.run();

		// Check standard output files
		List<String> filesToCheckFor = new LinkedList<>();
		filesToCheckFor.add(outputPath + scenarioName + ".0.actsFromParatransitUsers.txt");
		filesToCheckFor.add(outputPath + scenarioName + ".pOperatorLogger.txt");
		filesToCheckFor.add(outputPath + scenarioName + ".pStats.txt");
		filesToCheckFor.add(outputPath + scenarioName + ".scorestats.csv");
		filesToCheckFor.add(outputPath + scenarioName + ".stopwatch.csv");
		filesToCheckFor.add(outputPath + scenarioName + ".traveldistancestats.csv");
		filesToCheckFor.add(outputPath + scenarioName + ".pStat_light.gexf.gz");
		filesToCheckFor.add(outputPath + scenarioName + ".pStat.gexf.gz");

		for (String filename : filesToCheckFor) {
			File f = new File(filename);
			Assertions.assertEquals(true, f.exists() && !f.isDirectory(), filename + " does not exist");
		}


		// Check pStats
		String filenameOfpStats = outputPath + scenarioName + ".pStats.txt";
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filenameOfpStats);
		tabFileParserConfig.setDelimiterTags(new String[] {"\t"});

		new TabularFileParser().parse(tabFileParserConfig, this);

		Assertions.assertEquals(12, this.pStatsResults.size(), "There a less than the expected number of " + (numberOfIterations + 2) + " lines in " + filenameOfpStats);

		// Check first iteration
		Assertions.assertEquals("1", this.pStatsResults.get(1)[1], "Number of coops (first iteration)");
		Assertions.assertEquals("1", this.pStatsResults.get(1)[3], "Number of routes (first iteration)");
		Assertions.assertEquals("3092", this.pStatsResults.get(1)[5], "Number of pax (first iteration)");
		Assertions.assertEquals("3", this.pStatsResults.get(1)[7], "Number of veh (first iteration)");
		Assertions.assertEquals("-149.4493333333", this.pStatsResults.get(1)[9], "Number of budget (first iteration)");

		Assertions.assertEquals("0", this.pStatsResults.get(1)[2], "Number of +coops (first iteration)");
		Assertions.assertEquals("0", this.pStatsResults.get(1)[4], "Number of +routes (first iteration)");
		Assertions.assertEquals("0", this.pStatsResults.get(1)[6], "Number of +pax (first iteration)");
		Assertions.assertEquals("0", this.pStatsResults.get(1)[8], "Number of +veh (first iteration)");
		Assertions.assertEquals("NaN", this.pStatsResults.get(1)[10], "Number of +budget (first iteration)");

		Assertions.assertEquals("3", this.pStatsResults.get(11)[1], "Number of coops (last iteration)");
		Assertions.assertEquals("3", this.pStatsResults.get(11)[3], "Number of routes (last iteration)");
		Assertions.assertEquals("6728", this.pStatsResults.get(11)[5], "Number of pax (last iteration)");
		Assertions.assertEquals("10", this.pStatsResults.get(11)[7], "Number of veh (last iteration)");
		Assertions.assertEquals("68.7117037037", this.pStatsResults.get(11)[9], "Number of budget (last iteration)");

		Assertions.assertEquals("2", this.pStatsResults.get(11)[2], "Number of +coops (last iteration)");
		Assertions.assertEquals("2", this.pStatsResults.get(11)[4], "Number of +routes (last iteration)");
		Assertions.assertEquals("6508", this.pStatsResults.get(11)[6], "Number of +pax (last iteration)");
		Assertions.assertEquals("7", this.pStatsResults.get(11)[8], "Number of +veh (last iteration)");
		Assertions.assertEquals("113.2005555555", this.pStatsResults.get(11)[10], "Number of +budget (last iteration)");
	}

	@Override
	public void startRow(String[] row) {
		this.pStatsResults.add(row);
	}
}
