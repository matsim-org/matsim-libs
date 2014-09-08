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

package playground.andreas.P2.integration;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.pt.PtConstants;
import org.matsim.testcases.MatsimTestUtils;
import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.hook.PHook;
import playground.andreas.bvgScoringFunction.BvgScoringFunctionConfigGroup;
import playground.andreas.bvgScoringFunction.BvgScoringFunctionFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * Integration test of the minibus package
 * 
 * @author aneumann
 */
public class PControlerTest implements TabularFileHandler{
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	private ArrayList<String[]> pStatsResults = new ArrayList<String[]>();


	@Test
    public final void testPControler() {
		
		final String scenarioName = "corr_s1_0";
		final String numberOfIterations = "10";
		
		final String inputPath = utils.getClassInputDirectory();
		final String outputPath = utils.getOutputDirectory() + scenarioName + "/";
		
		final String configFile = inputPath + "config_" + scenarioName + ".xml";
		
		Config config = new Config();
		config.addModule(new PConfigGroup());
		ConfigUtils.loadConfig(config, configFile);
		
		config.getModule("controler").addParam("lastIteration", numberOfIterations);

        Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(false);
		
		// manipulate config
		PHook pHook = new PHook(controler);
		controler.addControlerListener(pHook);		
		controler.setScoringFunctionFactory(new BvgScoringFunctionFactory(controler.getConfig().planCalcScore(), new BvgScoringFunctionConfigGroup(controler.getConfig()), controler.getNetwork()));

		controler.run();
		
		// Check standard output files	
		List<String> filesToCheckFor = new LinkedList<String>();
		filesToCheckFor.add(outputPath + scenarioName + ".0.actsFromParatransitUsers.txt");
		filesToCheckFor.add(outputPath + scenarioName + ".pCoopLogger.txt");
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
        Assert.assertEquals("Number of +coops (first iteration)", "1", this.pStatsResults.get(1)[2]);
        Assert.assertEquals("Number of +routes (first iteration)", "1", this.pStatsResults.get(1)[4]);
        Assert.assertEquals("Number of +pax (first iteration)", "4304", this.pStatsResults.get(1)[6]);
        Assert.assertEquals("Number of +veh (first iteration)", "3", this.pStatsResults.get(1)[8]);
        Assert.assertEquals("Number of +budget (first iteration)", "91.96588888887833", this.pStatsResults.get(1)[10]);

        Assert.assertEquals("Number of coops (last iteration)", "3", this.pStatsResults.get(11)[1]);
        Assert.assertEquals("Number of routes (last iteration)", "4", this.pStatsResults.get(11)[3]);
        Assert.assertEquals("Number of pax (last iteration)", "7933", this.pStatsResults.get(11)[5]);
        Assert.assertEquals("Number of veh (last iteration)", "12", this.pStatsResults.get(11)[7]);
        Assert.assertEquals("Number of budget (last iteration)", "-2.935740740783333", this.pStatsResults.get(11)[9]);

        Assert.assertEquals("Number of +coops (last iteration)", "2", this.pStatsResults.get(11)[2]);
        Assert.assertEquals("Number of +routes (last iteration)", "3", this.pStatsResults.get(11)[4]);
        Assert.assertEquals("Number of +pax (last iteration)", "7782", this.pStatsResults.get(11)[6]);
        Assert.assertEquals("Number of +veh (last iteration)", "9", this.pStatsResults.get(11)[8]);
        Assert.assertEquals("Number of +budget (last iteration)", "9.182166666602782", this.pStatsResults.get(11)[10]);
	}

	@Override
	public void startRow(String[] row) {
		this.pStatsResults.add(row);	
	}		
}