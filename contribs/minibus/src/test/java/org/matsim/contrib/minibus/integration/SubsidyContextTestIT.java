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
import org.junit.Ignore;
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

public class SubsidyContextTestIT implements TabularFileHandler {
	

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private final ArrayList<String[]> pStatsResults = new ArrayList<>();
	private String gridScenarioDirectory ="../../example-scenario/input/";
	
	@Ignore
	@Test
	public final void testDefaultPControler() {
			
		Config config1 = ConfigUtils.loadConfig( utils.getClassInputDirectory() + "config.xml", new PConfigGroup() ) ;
		
		config1.network().setInputFile(gridScenarioDirectory  + "network.xml");
		config1.transit().setVehiclesFile(gridScenarioDirectory + "transitVehicles.xml");
		config1.transit().setTransitScheduleFile(gridScenarioDirectory + "transitSchedule_10min.xml");
		config1.plans().setInputFile(gridScenarioDirectory + "population_1000_per_hour_each_from_6_to_10.xml.gz");
		config1.controler().setOutputDirectory(utils.getOutputDirectory());
		
		Scenario scenario1 = ScenarioUtils.loadScenario(config1);	
		Controler controler1 = new Controler(scenario1);
		
		controler1.getConfig().controler().setCreateGraphs(true);
		controler1.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		
		controler1.addOverridingModule(new PModule()) ;
		controler1.run();
		
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
		Assert.assertEquals("Number of coops (final iteration)", "10", this.pStatsResults.get(31)[1]);
		Assert.assertEquals("Number of routes (final iteration)", "13", this.pStatsResults.get(31)[3]);
		Assert.assertEquals("Number of pax (final iteration)", "17163", this.pStatsResults.get(31)[5]);
		Assert.assertEquals("Number of veh (final iteration)", "583", this.pStatsResults.get(31)[7]);
		Assert.assertEquals("Number of budget (final iteration)", "-7123.2705000000", this.pStatsResults.get(31)[9]);

		Assert.assertEquals("Number of +coops (final iteration)", "4", this.pStatsResults.get(31)[2]);
		Assert.assertEquals("Number of +routes (final iteration)", "6", this.pStatsResults.get(31)[4]);
		Assert.assertEquals("Number of +pax (final iteration)", "3610", this.pStatsResults.get(31)[6]);
		Assert.assertEquals("Number of +veh (final iteration)", "16", this.pStatsResults.get(31)[8]);		
	}
	
	@Ignore
	@Test
	public final void testSubsidyPControler() {
		
		Config config2 = ConfigUtils.loadConfig( utils.getClassInputDirectory() + "config.xml", new PConfigGroup() ) ;
		
		PConfigGroup pConfig2 = (PConfigGroup) config2.getModules().get(PConfigGroup.GROUP_NAME);
		pConfig2.setSubsidyApproach("perPassenger");
		
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
		Assert.assertEquals("Number of coops (final iteration)", "10", this.pStatsResults.get(31)[1]);
		Assert.assertEquals("Number of routes (final iteration)", "62", this.pStatsResults.get(31)[3]);
		Assert.assertEquals("Number of pax (final iteration)", "17210", this.pStatsResults.get(31)[5]);
		Assert.assertEquals("Number of veh (final iteration)", "761", this.pStatsResults.get(31)[7]);
		Assert.assertEquals("Number of budget (final iteration)", "-1876.5780555555", this.pStatsResults.get(31)[9]);

		Assert.assertEquals("Number of +coops (final iteration)", "4", this.pStatsResults.get(31)[2]);
		Assert.assertEquals("Number of +routes (final iteration)", "51", this.pStatsResults.get(31)[4]);
		Assert.assertEquals("Number of +pax (final iteration)", "9951", this.pStatsResults.get(31)[6]);
		Assert.assertEquals("Number of +veh (final iteration)", "260", this.pStatsResults.get(31)[8]);		
	}
	
	@Override
	public void startRow(String[] row) {
		this.pStatsResults.add(row);	
	}
	
}

