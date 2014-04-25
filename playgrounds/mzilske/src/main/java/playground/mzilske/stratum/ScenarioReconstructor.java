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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.mzilske.cdr.CompareMain;
import playground.mzilske.cdr.PopulationFromSightings;
import playground.mzilske.cdr.ZoneTracker;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

class ScenarioReconstructor implements Provider<Scenario> {

    @Inject
    Config config;

    @Inject @Named("groundTruthNetwork")
    Network network;

    @Inject
    CompareMain compareMain;

    @Inject
    ZoneTracker.LinkToZoneResolver linkToZoneResolver;

    public Scenario get() {


        ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
        scenario.setNetwork(network);

        PopulationFromSightings.createPopulationWithEndTimesAtLastSightings(scenario, linkToZoneResolver, compareMain.getSightingsPerPerson());
        PopulationFromSightings.preparePopulation(scenario, linkToZoneResolver, compareMain.getSightingsPerPerson());


        final ObjectAttributes personAttributes = scenario.getPopulation().getPersonAttributes();

        final String CLONE_FACTOR = "cloneScore";
        for (Person person : scenario.getPopulation().getPersons().values()) {
            personAttributes.putAttribute(person.getId().toString(), CLONE_FACTOR, 0.0);
        }
        for (Person person : scenario.getPopulation().getPersons().values()) {
            person.setSelectedPlan(new RandomPlanSelector<Plan>().selectPlan(person));
        }
        return scenario;
    }

    static class ConfigProvider implements Provider<Config> {
        public Config get() {
            final Config config = ConfigUtils.createConfig();
            ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
            config.strategy().setMaxAgentPlanMemorySize(100);
            config.planCalcScore().setWriteExperiencedPlans(true);
            config.controler().setLastIteration(1000);
            config.qsim().setFlowCapFactor(100);
            config.qsim().setStorageCapFactor(100);
            config.qsim().setRemoveStuckVehicles(false);

            StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(new IdImpl(1));
            stratSets.setModuleName("ccc") ;
            stratSets.setProbability(1.) ;
            config.strategy().addStrategySettings(stratSets) ;
            return config;
        }
    }
}
