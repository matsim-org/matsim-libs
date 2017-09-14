/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.core.utils.misc;

import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class PopulationUtilsTest {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	
	@Test
	public void testEmptyPopulation() {
		Scenario s1 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario s2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Assert.assertTrue(PopulationUtils.equalPopulation(s1.getPopulation(), s2.getPopulation()));
	}
	
	@Test
	public void testEmptyPopulationVsOnePerson() {
		Scenario s1 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario s2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Person person = s2.getPopulation().getFactory().createPerson(Id.create("1", Person.class));
		s2.getPopulation().addPerson(person);
		Assert.assertFalse(PopulationUtils.equalPopulation(s1.getPopulation(), s2.getPopulation()));
		Assert.assertFalse(PopulationUtils.equalPopulation(s2.getPopulation(), s1.getPopulation()));
	}
	
	@Test
	public void testCompareBigPopulationWithItself() {
		Scenario s1 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		String netFileName = "test/scenarios/berlin/network.xml";
		String popFileName = "test/scenarios/berlin/plans_hwh_1pct.xml.gz";
		new MatsimNetworkReader(s1.getNetwork()).readFile(netFileName);
		new PopulationReader(s1).readFile(popFileName);
		Assert.assertTrue(PopulationUtils.equalPopulation(s1.getPopulation(), s1.getPopulation()));
	}

}
