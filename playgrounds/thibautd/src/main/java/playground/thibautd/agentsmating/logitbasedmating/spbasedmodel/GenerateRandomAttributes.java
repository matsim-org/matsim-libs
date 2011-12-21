/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateRandomAttributes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.agentsmating.logitbasedmating.spbasedmodel;

import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Creates a test population from the argument population, by generating random
 * values for missing attributes.
 *
 * @author thibautd
 */
public class GenerateRandomAttributes {
	private static final Random random = new Random(195312);

	private static final double alwaysWeight = 8;
	private static final double neverWeight = 1;
	private static final double sometimesWeight = 1;
	private static final double totalAvailWeight = alwaysWeight + neverWeight + sometimesWeight;

	public static void main(final String[] args) {
		String configFileName = args[ 0 ];

		Config config = ConfigUtils.loadConfig( configFileName );
		Scenario scenario = ScenarioUtils.loadScenario( config );

		for (Person person : scenario.getPopulation().getPersons().values()) {
			PersonImpl personImpl = (PersonImpl) person;

			if (personImpl.getAge() < 0) personImpl.setAge( randomAge() );
			if (personImpl.getSex() == null) personImpl.setSex( randomSex() );
			if (personImpl.getTravelcards() == null) randomTravelCard(personImpl);
			if (personImpl.getCarAvail() == null) personImpl.setCarAvail( randomCarAvail() );
			if (personImpl.getLicense() == null) personImpl.setLicence( randomLicense() );
		}

		new PopulationWriter(
				scenario.getPopulation(),
				scenario.getNetwork()).write( config.plans().getInputFile()+".filled" );
	}

	private static int randomAge() {
		return random.nextInt( 100 );
	}

	private static String randomSex() {
		return random.nextBoolean() ? "m" : "f";
	}

	private static void randomTravelCard(final PersonImpl person) {
		if (random.nextBoolean()) {
			person.addTravelcard("unknown");
		}
	}

	private static String randomCarAvail() {
		double choice = random.nextDouble() * totalAvailWeight;

		if (choice < alwaysWeight) return "always";
		if (choice < alwaysWeight + sometimesWeight) return "sometimes";
		return "never";
	}

	private static String randomLicense() {
		return random.nextBoolean() ? "yes" : "no";
	}
}

