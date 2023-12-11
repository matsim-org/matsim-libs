/* *********************************************************************** *
 * project: org.matsim.*
 * PermissibleModesCalculatorImplTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.population.algorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.population.algorithms.PermissibleModesCalculatorImpl;

/**
 * @author thibautd
 */
public class PermissibleModesCalculatorImplTest {
	private static class Fixture {
		public final String name;
		public final Plan plan;
		public final boolean carAvail;

		public Fixture(
				final String name,
				final Plan plan,
				final boolean carAvail ) {
			this.name = name;
			this.plan = plan;
			this.carAvail = carAvail;
		}
	}
	private final List<Fixture> fixtures = new ArrayList<Fixture>();

	@AfterEach
	public void clean() {
		fixtures.clear();
	}

	@BeforeEach
	public void fixtureWithNothing() {
		String name = "no information";
		Person person = PopulationUtils.getFactory().createPerson(Id.create(name, Person.class));
		Plan plan = PopulationUtils.createPlan(person);
		fixtures.add( new Fixture( name , plan , true ) );
	}

	@BeforeEach
	public void fixtureWithNoLicense() {
		String name = "no License";
		Person person = PopulationUtils.getFactory().createPerson(Id.create(name, Person.class));
		Plan plan = PopulationUtils.createPlan(person);
		PersonUtils.setLicence(person, "no");
		fixtures.add( new Fixture( name , plan , false ) );
	}

	@BeforeEach
	public void fixtureWithNoCar() {
		String name = "no car" ;
		Person person = PopulationUtils.getFactory().createPerson(Id.create(name, Person.class));
		Plan plan = PopulationUtils.createPlan(person);
		PersonUtils.setCarAvail(person, "never");
		fixtures.add( new Fixture( name , plan , false ) );
	}

	@BeforeEach
	public void fixtureWithCarSometimes() {
		String name = "car sometimes";
		Person person = PopulationUtils.getFactory().createPerson(Id.create(name, Person.class));
		Plan plan = PopulationUtils.createPlan(person);
		PersonUtils.setCarAvail(person, "sometimes");
		fixtures.add( new Fixture( name , plan , true ) );
	}

	@Test
	void testWhenConsideringCarAvailability() throws Exception {
		final List<String> modesWithCar = Arrays.asList(TransportMode.car, "rail", "plane");
		final List<String> modesWithoutCar = Arrays.asList("rail", "plane");
		Config config = ConfigUtils.createConfig();
		config.subtourModeChoice().setModes(modesWithCar.toArray(new String[0]));
		config.subtourModeChoice().setConsiderCarAvailability(true);

		final PermissibleModesCalculator calculatorWithCarAvailability =
				new PermissibleModesCalculatorImpl(config);

		for (Fixture f : fixtures) {
			assertListsAreCompatible(
					f.name,
					f.carAvail ? modesWithCar : modesWithoutCar,
					calculatorWithCarAvailability.getPermissibleModes(f.plan));
		}
	}

	@Test
	void testWhenNotConsideringCarAvailability() throws Exception {
		final List<String> modesWithCar = Arrays.asList(TransportMode.car, "rail", "plane");
		Config config = ConfigUtils.createConfig();
		config.subtourModeChoice().setModes(modesWithCar.toArray(new String[0]));
		config.subtourModeChoice().setConsiderCarAvailability(false);
		final PermissibleModesCalculator calculatorWithCarAvailability =
				new PermissibleModesCalculatorImpl(config);

		for (Fixture f : fixtures) {
			assertListsAreCompatible(
					f.name,
					modesWithCar,
					calculatorWithCarAvailability.getPermissibleModes(f.plan));
		}
	}

	private static void assertListsAreCompatible(
			final String fixtureName,
			final List<String> expected,
			final Collection<String> actual) {
		assertEquals(
				expected.size(),
				actual.size(),
				expected+" and "+actual+" have incompatible sizes for fixture "+fixtureName);

		assertTrue(
				expected.containsAll( actual ),
				expected+" and "+actual+" are not compatible for fixture "+fixtureName);
	}
}

