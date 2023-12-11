/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * SamplerTest.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.contrib.locationchoice.frozenepsilons;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;

public class SamplerTest {

    @RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

    private DestinationChoiceContext context;
    private Scenario scenario;

    @BeforeEach
    public void setUp() throws Exception {
        Config config = ConfigUtils.loadConfig("test/scenarios/chessboard/config.xml", new FrozenTastesConfigGroup() );
        ConfigUtils.loadConfig(config, utils.getPackageInputDirectory() + "/config.xml");
        scenario = ScenarioUtils.loadScenario(config);
        this.context = new DestinationChoiceContext(this.scenario);
        this.context.init();
    }

	@Test
	void testSampler() {
        DestinationSampler sampler = new DestinationSampler(
                context.getPersonsKValuesArray(), context.getFacilitiesKValuesArray(), ConfigUtils.addOrGetModule( scenario.getConfig(), FrozenTastesConfigGroup.class ) ) ;
        assertTrue(sampler.sample(context.getFacilityIndex(Id.create(1, ActivityFacility.class)), context.getPersonIndex(Id.create(1, Person.class))));
        assertTrue(!sampler.sample(context.getFacilityIndex(Id.create(1, ActivityFacility.class)), context.getPersonIndex(Id.create(2, Person.class))));
    }

}
