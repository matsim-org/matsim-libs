/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ExternalModuleTest.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
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

package org.matsim.core.replanning.modules;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.testcases.MatsimTestUtils;

public class ExternalModuleTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    private Scenario scenario;
    private OutputDirectoryHierarchy outputDirectoryHierarchy;
    private Scenario originalScenario;

    @Before
    public void setUp() {
        scenario = ScenarioUtils.loadScenario(utils.loadConfig("test/scenarios/equil/config.xml"));
        originalScenario = ScenarioUtils.loadScenario(utils.loadConfig("test/scenarios/equil/config.xml"));
        outputDirectoryHierarchy = new OutputDirectoryHierarchy(utils.getOutputDirectory(), OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
    }

    @Test
    public void testNoOpExternalModule() {
        ExternalModule testee = new ExternalModule(new ExternalModule.ExeRunnerDelegate() {
            @Override
            public boolean invoke() {
                Population inPopulation = loadPopulation(outputDirectoryHierarchy.getTempPath()+"/test_plans.in.xml");
                new PopulationWriter(inPopulation).write(outputDirectoryHierarchy.getTempPath()+"/test_plans.out.xml");
                return true;
            }
        }, "test", outputDirectoryHierarchy, scenario);
        replanPopulation(scenario.getPopulation(), testee);
        Assert.assertTrue(PopulationUtils.equalPopulation(scenario.getPopulation(), originalScenario.getPopulation()));
    }

    @Test
    public void testPlanEmptyingExternalModule() {
        ExternalModule testee = new ExternalModule(new ExternalModule.ExeRunnerDelegate() {
            @Override
            public boolean invoke() {
                Population inPopulation = loadPopulation(outputDirectoryHierarchy.getTempPath()+"/test_plans.in.xml");
                for (Person person : inPopulation.getPersons().values()) {
                    person.getSelectedPlan().getPlanElements().clear();
                }
                new PopulationWriter(inPopulation).write(outputDirectoryHierarchy.getTempPath()+"/test_plans.out.xml");
                return true;
            }
        }, "test", outputDirectoryHierarchy, scenario);
        replanPopulation(scenario.getPopulation(), testee);
        Assert.assertFalse(PopulationUtils.equalPopulation(scenario.getPopulation(), originalScenario.getPopulation()));
    }

    private Population loadPopulation(String filename) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimPopulationReader(scenario).readFile(filename);
        return scenario.getPopulation();
    }

    private void replanPopulation(Population population, PlanStrategyModule testee) {
        testee.prepareReplanning(new ReplanningContext() {
            @Override
            public int getIteration() {
                return 0;
            }
        });
        for (Person person : population.getPersons().values()) {
            testee.handlePlan(person.getSelectedPlan());
        }
        testee.finishReplanning();
    }

}