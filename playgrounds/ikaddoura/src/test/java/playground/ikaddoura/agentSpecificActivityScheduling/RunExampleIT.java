/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.agentSpecificActivityScheduling;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.ScoreStatsControlerListener.ScoreItem;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
* @author ikaddoura
*/

public class RunExampleIT {

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public void test1() {
		
		final String configFile = testUtils.getPackageInputDirectory() + "/config.xml";
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile, new AgentSpecificActivitySchedulingConfigGroup()));
	
		AgentSpecificActivitySchedulingConfigGroup asasConfigGroup = (AgentSpecificActivitySchedulingConfigGroup) scenario.getConfig().getModules().get(AgentSpecificActivitySchedulingConfigGroup.GROUP_NAME);
		asasConfigGroup.setTolerance(0.);
		
		String outputDirectory = testUtils.getOutputDirectory() + "/";
		scenario.getConfig().controler().setOutputDirectory(outputDirectory);
		
		Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new AgentSpecificActivitySchedulingModule(scenario));
		
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();

		final int index = scenario.getConfig().controler().getLastIteration() - scenario.getConfig().controler().getFirstIteration();
		double executedScore = controler.getScoreStats().getScoreHistory().get( ScoreItem.executed ).get(index);
		Assert.assertEquals("Wrong average executed score.", 132.59084365011148, executedScore, MatsimTestUtils.EPSILON);
		
	}
	
}

