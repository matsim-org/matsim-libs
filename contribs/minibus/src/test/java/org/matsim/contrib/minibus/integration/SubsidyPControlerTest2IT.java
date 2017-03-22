/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.testcases.MatsimTestUtils;

/**
 * 
 * Tests the functionality of adding subsidies to the operator's score.
 * 
* @author ikaddoura
*/

public class SubsidyPControlerTest2IT implements TabularFileHandler {
	
	private static final String gridScenarioDirectory = "http://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/atlantis/minibus/input/";

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private final ArrayList<String[]> pStatsResults = new ArrayList<>();

	@Test
	public final void testSubsidyPControler() {
		
		Config config2 = ConfigUtils.loadConfig( utils.getClassInputDirectory() + "config.xml", new PConfigGroup() ) ;
		
		PConfigGroup pConfig2 = (PConfigGroup) config2.getModules().get(PConfigGroup.GROUP_NAME);
		pConfig2.setWelfareMaximization(true);
		pConfig2.addParam("operatorType", "WelfareCarefulMultiPlanOperator");
		
		config2.network().setInputFile(gridScenarioDirectory  + "network.xml");
		config2.transit().setVehiclesFile(gridScenarioDirectory + "transitVehicles.xml");
		config2.transit().setTransitScheduleFile(gridScenarioDirectory + "transitSchedule_10min.xml");
		config2.plans().setInputFile(gridScenarioDirectory + "population_1000_per_hour_each_from_6_to_10.xml.gz");
		config2.controler().setOutputDirectory(utils.getOutputDirectory());
		
		Scenario scenario2 = ScenarioUtils.loadScenario(config2);	
		Controler controler2 = new Controler(scenario2);
		
		controler2.getConfig().controler().setCreateGraphs(true);
		controler2.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		
		controler2.addOverridingModule(new PModule()) ;
		controler2.run();
		
		// Check standard output files	
		
		List<String> filesToCheckFor = new LinkedList<>();
		filesToCheckFor.add(utils.getOutputDirectory() + "0.actsFromParatransitUsers.txt");
		filesToCheckFor.add(utils.getOutputDirectory() + "pOperatorLogger.txt");
		filesToCheckFor.add(utils.getOutputDirectory() + "pStats.txt");
		filesToCheckFor.add(utils.getOutputDirectory() + "scorestats.txt");
		filesToCheckFor.add(utils.getOutputDirectory() + "stopwatch.txt");
		filesToCheckFor.add(utils.getOutputDirectory() + "traveldistancestats.txt");
		filesToCheckFor.add(utils.getOutputDirectory() + "pStat_light.gexf.gz");
		filesToCheckFor.add(utils.getOutputDirectory() + "pStat.gexf.gz");

		for (String filename : filesToCheckFor) {
			File f = new File(filename);
			Assert.assertEquals(filename + " does not exist", true, f.exists() && !f.isDirectory());
		}

		// Check pStats
		String filenameOfpStats = utils.getOutputDirectory() + "pStats.txt";
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filenameOfpStats);
		tabFileParserConfig.setDelimiterTags(new String[] {"\t"});

		new TabularFileParser().parse(tabFileParserConfig, this);

		// Check final iteration
		Assert.assertEquals("Number of budget (final iteration)", "196899047.9701666800", this.pStatsResults.get(2)[9]);	
	}
	
	@Override
	public void startRow(String[] row) {
		this.pStatsResults.add(row);	
	}
	
}

