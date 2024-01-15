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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class ExternalModuleTest {

    @RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

    private Scenario scenario;
    private OutputDirectoryHierarchy outputDirectoryHierarchy;
    private Scenario originalScenario;

    @BeforeEach
    public void setUp() {
        scenario = ScenarioUtils.loadScenario(utils.loadConfig("test/scenarios/equil/config.xml"));
        originalScenario = ScenarioUtils.loadScenario(utils.loadConfig("test/scenarios/equil/config.xml"));
        final String outputDirectory = utils.getOutputDirectory();
        outputDirectoryHierarchy = new OutputDirectoryHierarchy(
                outputDirectory, OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists, this.scenario.getConfig().controller().getCompressionType());
        scenario.getConfig().controller().setOutputDirectory( outputDirectory );
//        scenario.getConfig().controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );

//        // the following includes all retrofittings that were added later:
//        com.google.inject.Injector injector = Injector.createInjector( scenario.getConfig(), new AbstractModule(){
//            @Override public void install(){
//                install( new NewControlerModule() );
//                install( new ControlerDefaultCoreListenersModule() );
//                install( new ControlerDefaultsModule() );
//                install( new ScenarioByInstanceModule( scenario ) );
//            }
//        } );;
//        PrepareForSim prepareForSim = injector.getInstance( PrepareForSim.class ) ;
//        prepareForSim.run();
    }

	@Test
	void testNoOpExternalModule() {
        ExternalModule testee = new ExternalModule(new ExternalModule.ExeRunnerDelegate() {
            @Override
            public boolean invoke() {
                Population inPopulation = loadPopulation(outputDirectoryHierarchy.getTempPath()+"/test_plans.in.xml");
                new PopulationWriter(inPopulation).write(outputDirectoryHierarchy.getTempPath()+"/test_plans.out.xml");
                return true;
            }
        }, "test", outputDirectoryHierarchy, scenario);
        replanPopulation(scenario.getPopulation(), testee);
        Assertions.assertTrue(PopulationUtils.equalPopulation(scenario.getPopulation(), originalScenario.getPopulation()));
    }

	@Test
	void testPlanEmptyingExternalModule() {
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
        Assertions.assertFalse(PopulationUtils.equalPopulation(scenario.getPopulation(), originalScenario.getPopulation()));
    }

    private Population loadPopulation(String filename) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(filename);
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
