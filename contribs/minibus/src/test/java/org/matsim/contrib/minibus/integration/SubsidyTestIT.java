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

public class SubsidyTestIT implements TabularFileHandler {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private final ArrayList<String[]> pStatsResults = new ArrayList<>();

	@Test
	public final void testSubsidyPControler() {
		
		Config config = ConfigUtils.loadConfig( utils.getClassInputDirectory() + "config.xml", new PConfigGroup() ) ;

		PConfigGroup pConfig = (PConfigGroup) config.getModules().get(PConfigGroup.GROUP_NAME);
		pConfig.setSubsidyApproach("perPassenger");
		String gridScenarioDirectory ="../../example-scenario/input/";
		config.network().setInputFile(gridScenarioDirectory  + "network.xml");
		config.transit().setVehiclesFile(gridScenarioDirectory + "transitVehicles.xml");
		config.transit().setTransitScheduleFile(gridScenarioDirectory + "transitSchedule_10min.xml");
		config.plans().setInputFile(gridScenarioDirectory + "population_1000_per_hour_each_from_6_to_10.xml.gz");
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setWriteEventsInterval(0);
		Scenario scenario = ScenarioUtils.loadScenario(config);	
		Controler controler = new Controler(scenario);
		
		controler.getConfig().controler().setCreateGraphs(true);
		
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		
		controler.addOverridingModule(new PModule()) ;
		controler.run();
		
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
		Assert.assertEquals("Number of budget (final iteration)", "202319997.4909444700", this.pStatsResults.get(2)[9]);
	}
	
	@Override
	public void startRow(String[] row) {
		this.pStatsResults.add(row);	
	}
	
}

