/* *********************************************************************** *
 * project: org.matsim.*
 * Fixture
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.oneagent;

import java.lang.reflect.Method;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.testcases.MatsimTestUtils;


/**
 * @author dgrether
 *
 */
public class Fixture {

	static final Id<Link> linkId1 = Id.create(1, Link.class);
	static final Id<Link> linkId2 = Id.create(2, Link.class);
	static final Id<SignalPlan> signalPlanId2 = Id.create(2, SignalPlan.class);
	static final Id<SignalSystem> signalSystemId2 = Id.create(2, SignalSystem.class);
	static final Id<SignalGroup> signalGroupId100 = Id.create(100, SignalGroup.class);

	public Scenario createAndLoadTestScenario(Boolean useIntergreens){
		MatsimTestUtils testUtils = new MatsimTestUtils();
		try {
			Method m = this.getClass().getMethod("createAndLoadTestScenario", Boolean.class);
			testUtils.initWithoutJUnitForFixture(this.getClass(), m);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
		Config conf = ConfigUtils.createConfig(testUtils.classInputResourcePath());
		conf.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		ActivityParams params = new ActivityParams("h");
		params.setTypicalDuration(24.0 * 3600.0);
		conf.planCalcScore().addActivityParams(params);

		StrategySettings settings = new StrategySettings(Id.create("1", StrategySettings.class));
		settings.setStrategyName("ChangeExpBeta");
		settings.setWeight(1.0);
		conf.strategy().addStrategySettings(settings);
		conf.network().setInputFile("network.xml.gz");
		conf.network().setLaneDefinitionsFile("testLaneDefinitions_v2.0.xml");
		conf.plans().setInputFile("plans1Agent.xml");
		conf.qsim().setUseLanes(true);
		conf.qsim().setStuckTime(1000);
		conf.qsim().setStartTime(0.0);
		conf.qsim().setUsingFastCapacityUpdate(false);
		SignalSystemsConfigGroup signalsConfig = ConfigUtils.addOrGetModule(conf, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		signalsConfig.setUseSignalSystems(true);
		
		if (useIntergreens) {
			signalsConfig.setIntergreenTimesFile("testIntergreenTimes_v1.0.xml");
			signalsConfig.setUseIntergreenTimes(true);
			signalsConfig.setActionOnIntergreenViolation(SignalSystemsConfigGroup.ActionOnSignalSpecsViolation.EXCEPTION);
		}			

		this.setSignalSystemConfigValues(signalsConfig, testUtils);
		Scenario scenario = ScenarioUtils.loadScenario(conf);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(conf).loadSignalsData());
		
		return scenario;
	}

	private void setSignalSystemConfigValues(SignalSystemsConfigGroup signalsConfig, MatsimTestUtils testUtils){
		signalsConfig.setSignalSystemFile("testSignalSystems_v2.0.xml");
		signalsConfig.setSignalGroupsFile("testSignalGroups_v2.0.xml");
		signalsConfig.setSignalControlFile("testSignalControl_v2.0.xml");
		signalsConfig.setAmberTimesFile("testAmberTimes_v1.0.xml");
	}

	
}
