
/* *********************************************************************** *
 * project: org.matsim.*
 * JDEQSimPluginTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.mobsim.qsim.jdeqsimengine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

 /**
 * Created by michaelzilske on 19/03/14.
 */
public class JDEQSimPluginTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private QSim prepareQSim(Scenario scenario, EventsManager eventsManager) {
        return new QSimBuilder(scenario.getConfig()) //
        	.addQSimModule(new JDEQSimModule()) //
        	.configureQSimComponents( components -> {
        		components.addNamedComponent(JDEQSimModule.COMPONENT_NAME);
        	} ) //
        	.build(scenario, eventsManager);
	}

	 @Test
	 void testRunsAtAll() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
        eventsManager.initProcessing();
        PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();

        QSim qsim = prepareQSim(scenario, eventsManager);
        qsim.run();
    }

	 @Test
	 void testRunsEquil() {
		Scenario scenario = ScenarioUtils.loadScenario(utils.loadConfig("test/scenarios/equil/config.xml"));
        EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
        eventsManager.initProcessing();
        PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();

        QSim qsim = prepareQSim(scenario, eventsManager);
        qsim.run();
    }

}
