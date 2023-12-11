/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.core.scoring.functions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Collection;
import java.util.Random;
import java.util.SplittableRandom;

public class PersonScoringParametersFromPersonAttributesIT {

    @RegisterExtension
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@SuppressWarnings("unchecked")
	@Test
	void testSetAttributeAndRunEquil(){
        Config config = testUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
        config.controller().setOutputDirectory(testUtils.getOutputDirectory());
        config.controller().setLastIteration(0);
        config.scoring().setPerforming_utils_hr(0.0d);
        config.scoring().getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(0.0d);
        config.plans().setInputFile("plans2.xml");

        Scenario scenario = ScenarioUtils.loadScenario(config);

        SplittableRandom splittableRandom = new SplittableRandom(config.global().getRandomSeed());
        PersonSpecificScoringAttributesSetter.setLogNormalModeConstant(
                (Collection<Person>) scenario.getPopulation().getPersons().values(),
                TransportMode.car, -1.0, 1.0, splittableRandom);

        Random random = MatsimRandom.getRandom();
        scenario.getPopulation().getPersons().values().forEach(
                person -> PersonUtils.setIncome(person, random.nextInt(10000)));

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(ScoringParametersForPerson.class).to(PersonScoringParametersFromPersonAttributes.class).asEagerSingleton();
            }
        });

        controler.run();

        for (Person person: scenario.getPopulation().getPersons().values()) {
            double score = person.getSelectedPlan().getScore();
            double personSpecificModeConstant = Double.parseDouble(PersonUtils.getModeConstants(person).get(TransportMode.car));

            // each person has 3 legs -> score should 3 x personSpecificModeConstant since all other scoring parameters are 0
            Assertions.assertEquals(personSpecificModeConstant, score / 3, MatsimTestUtils.EPSILON, "Score deviates from what is expected given the personSpecificModeConstant.");
            Assertions.assertTrue(personSpecificModeConstant < 0.0d,
                    "personSpecificModeConstant has value 0 or higher, this should never happen with log normal distribution for given mean -1 and sigma 1");
        }
    }
}
