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

package org.matsim.contrib.locationchoice.bestresponse;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;

public class SamplerTest extends MatsimTestCase {

    private DestinationChoiceBestResponseContext context;
    private Scenario scenario;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String configFile = this.getPackageInputDirectory() + "/config.xml";
        Config config = ConfigUtils.loadConfig(configFile, new DestinationChoiceConfigGroup());
        scenario = ScenarioUtils.loadScenario(config);
        this.context = new DestinationChoiceBestResponseContext(this.scenario);
        this.context.init();
    }

    public void testSampler() {
        DestinationSampler sampler = new DestinationSampler(
                context.getPersonsKValuesArray(), context.getFacilitiesKValuesArray(),
                (DestinationChoiceConfigGroup) scenario.getConfig().getModule("locationchoice"));
        assertTrue(sampler.sample(context.getFacilityIndex(Id.create(1, ActivityFacility.class)), context.getPersonIndex(Id.create(1, Person.class))));
        assertTrue(!sampler.sample(context.getFacilityIndex(Id.create(1, ActivityFacility.class)), context.getPersonIndex(Id.create(2, Person.class))));
    }

}
