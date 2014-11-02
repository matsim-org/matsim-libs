/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ScenarioReconstructor.java
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

package playground.mzilske.stratum;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.mzilske.cdr.CompareMain;
import playground.mzilske.cdr.PopulationFromSightings;
import playground.mzilske.cdr.Sightings;
import playground.mzilske.cdr.SightingsImpl;
import playground.mzilske.cdr.ZoneTracker;

class ScenarioReconstructor implements Provider<Scenario> {

    @Inject
    Config config;

    @Inject @Named("groundTruthNetwork")
    Network network;

    @Inject
    CompareMain compareMain;

    @Inject
    ZoneTracker.LinkToZoneResolver linkToZoneResolver;



    @Override
		public Scenario get() {
        ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
        scenario.setNetwork(network);

        Sightings sightings = new SightingsImpl(compareMain.getSightingsPerPerson());
        PopulationFromSightings.createPopulationWithRandomEndTimesInPermittedWindow(scenario, linkToZoneResolver, sightings);
        PopulationFromSightings.preparePopulation(scenario, linkToZoneResolver, sightings);

        for (Person person : scenario.getPopulation().getPersons().values()) {
            person.setSelectedPlan(new RandomPlanSelector<Plan, Person>().selectPlan(person));
        }
        return scenario;
    }

    static class ConfigProvider implements Provider<Config> {

        @Inject @Named("outputDirectory")
        String outputDirectory;

        @Override
				public Config get() {
            final Config config = ConfigUtils.createConfig();
            CadytsConfigGroup cadyts = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
            cadyts.setVarianceScale(0.00000000001);
            cadyts.setMinFlowStddev_vehPerHour(10.0);

            config.controler().setOutputDirectory(outputDirectory);
            config.controler().setLastIteration(50);
            config.qsim().setFlowCapFactor(100);
            config.qsim().setStorageCapFactor(100);
            config.qsim().setRemoveStuckVehicles(false);
            {
                StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(Id.create(1, StrategySettings.class));
                stratSets.setModuleName("SelectExpBeta");
                stratSets.setProbability(0.9);
                config.strategy().addStrategySettings(stratSets);
            }
            {
                StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(Id.create(2, StrategySettings.class));
                stratSets.setModuleName("SelectRandom");
                stratSets.setProbability(0.1);
                stratSets.setDisableAfter(10);
                config.strategy().addStrategySettings(stratSets);
            }
            return config;
        }
    }
}
