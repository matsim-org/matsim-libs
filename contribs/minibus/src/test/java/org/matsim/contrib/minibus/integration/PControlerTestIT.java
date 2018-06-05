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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.hook.PModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
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

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private final ArrayList<String[]> pStatsResults = new ArrayList<>();


	@Test
	public final void testPControler() {

		final String scenarioName = "corr_s1_0";
		final int numberOfIterations = 10;

		final String inputPath = utils.getClassInputDirectory();
		final String outputPath = utils.getOutputDirectory() + scenarioName + "/";

		final String configFile = inputPath + "config_" + scenarioName + ".xml";
		
		// ---

		Config config = ConfigUtils.loadConfig(configFile, new PConfigGroup());

		config.controler().setLastIteration(numberOfIterations);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setCreateGraphs(false);
		config.controler().setOutputDirectory(outputPath);
		
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
		filesToCheckFor.add(outputPath + scenarioName + ".scorestats.txt");
		filesToCheckFor.add(outputPath + scenarioName + ".stopwatch.txt");
		filesToCheckFor.add(outputPath + scenarioName + ".traveldistancestats.txt");
		filesToCheckFor.add(outputPath + scenarioName + ".pStat_light.gexf.gz");
		filesToCheckFor.add(outputPath + scenarioName + ".pStat.gexf.gz");

		for (String filename : filesToCheckFor) {
			File f = new File(filename);
			Assert.assertEquals(filename + " does not exist", true, f.exists() && !f.isDirectory());
		}


		// Check pStats
		String filenameOfpStats = outputPath + scenarioName + ".pStats.txt";
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filenameOfpStats);
		tabFileParserConfig.setDelimiterTags(new String[] {"\t"});

		new TabularFileParser().parse(tabFileParserConfig, this);

		Assert.assertEquals("There a less than the expected number of " + (numberOfIterations + 2) + " lines in " + filenameOfpStats, 12, this.pStatsResults.size());

		// Check first iteration
		Assert.assertEquals("Number of coops (first iteration)", "1", this.pStatsResults.get(1)[1]);
		Assert.assertEquals("Number of routes (first iteration)", "1", this.pStatsResults.get(1)[3]);
		Assert.assertEquals("Number of pax (first iteration)", "3399", this.pStatsResults.get(1)[5]);
		Assert.assertEquals("Number of veh (first iteration)", "3", this.pStatsResults.get(1)[7]);
		Assert.assertEquals("Number of budget (first iteration)", "-118.7326666667", this.pStatsResults.get(1)[9]);

		Assert.assertEquals("Number of +coops (first iteration)", "0", this.pStatsResults.get(1)[2]);
		Assert.assertEquals("Number of +routes (first iteration)", "0", this.pStatsResults.get(1)[4]);
		Assert.assertEquals("Number of +pax (first iteration)", "0", this.pStatsResults.get(1)[6]);
		Assert.assertEquals("Number of +veh (first iteration)", "0", this.pStatsResults.get(1)[8]);
		Assert.assertEquals("Number of +budget (first iteration)", "NaN", this.pStatsResults.get(1)[10]);

		Assert.assertEquals("Number of coops (last iteration)", "2", this.pStatsResults.get(11)[1]);
		Assert.assertEquals("Number of routes (last iteration)", "3", this.pStatsResults.get(11)[3]);
		Assert.assertEquals("Number of pax (last iteration)", "7134", this.pStatsResults.get(11)[5]);
		Assert.assertEquals("Number of veh (last iteration)", "10", this.pStatsResults.get(11)[7]);
		Assert.assertEquals("Number of budget (last iteration)", "86.6193333333", this.pStatsResults.get(11)[9]);

		Assert.assertEquals("Number of +coops (last iteration)", "1", this.pStatsResults.get(11)[2]);
		Assert.assertEquals("Number of +routes (last iteration)", "2", this.pStatsResults.get(11)[4]);
		Assert.assertEquals("Number of +pax (last iteration)", "7055", this.pStatsResults.get(11)[6]);
		Assert.assertEquals("Number of +veh (last iteration)", "9", this.pStatsResults.get(11)[8]);
		Assert.assertEquals("Number of +budget (last iteration)", "100.5174444444", this.pStatsResults.get(11)[10]);
	}

	@Override
	public void startRow(String[] row) {
		this.pStatsResults.add(row);	
	}		
}
