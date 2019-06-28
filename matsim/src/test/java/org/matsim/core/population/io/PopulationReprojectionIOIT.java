
/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationReprojectionIOIT.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.population.io;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class PopulationReprojectionIOIT {
	private static final String INITIAL_CRS = TransformationFactory.DHDN_GK4;
	private static final String TARGET_CRS = "EPSG:3857";
	private static final CoordinateTransformation transformation =
			TransformationFactory.getCoordinateTransformation(
					INITIAL_CRS,
					TARGET_CRS);

	private static final String NET_FILE = "network.xml.gz";
	private static final String BASE_FILE = "plans_hwh_1pct.xml.gz";

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testInput_V4() {
		final String testFile = new File(utils.getOutputDirectory() + "/plans.xml.gz").getAbsolutePath();

		// create test file in V4 format
		Config config = ConfigUtils.createConfig(ExamplesUtils.getTestScenarioURL("berlin"));
		final Scenario scenario = ScenarioUtils.createScenario(config);

		new MatsimNetworkReader(scenario.getNetwork()).parse(IOUtils.newUrl(config.getContext(), NET_FILE));
		new PopulationReader(scenario).parse(IOUtils.newUrl(config.getContext(), BASE_FILE));
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeV4(testFile);

		testConversionAtInput(testFile);
	}

	@Test
	public void testInput_V5() {
		final String testFile = new File(utils.getOutputDirectory() + "/plans.xml.gz").getAbsolutePath();

		// create test file in V5 format
		Config config = ConfigUtils.createConfig(ExamplesUtils.getTestScenarioURL("berlin"));
		final Scenario scenario = ScenarioUtils.createScenario(config);
		// necessary for v4...
		new MatsimNetworkReader(scenario.getNetwork()).parse(IOUtils.newUrl(config.getContext(), NET_FILE));
		new PopulationReader(scenario).parse(IOUtils.newUrl(config.getContext(), BASE_FILE));
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeV5(testFile);

		testConversionAtInput(testFile);
	}

	@Test
	public void testOutput_V4() {
		final String testFile = new File(utils.getOutputDirectory() + "/plans.xml.gz").getAbsolutePath();

		// read test population
		Config config = ConfigUtils.createConfig(ExamplesUtils.getTestScenarioURL("berlin"));
		final Scenario originalScenario = ScenarioUtils.createScenario(config);
		// necessary for v4...
		new MatsimNetworkReader(originalScenario.getNetwork()).parse(IOUtils.newUrl(config.getContext(), NET_FILE));
		new PopulationReader(originalScenario).parse(IOUtils.newUrl(config.getContext(), BASE_FILE));

		// write test population with conversion
		new PopulationWriter(
				transformation,
				originalScenario.getPopulation(),
				originalScenario.getNetwork()).writeV4( testFile );

		// read converted population
		final Scenario reprojectedScenario = ScenarioUtils.createScenario(config);
		// necessary for v4...
		new MatsimNetworkReader(reprojectedScenario.getNetwork()).parse(IOUtils.newUrl(config.getContext(), NET_FILE));
		new PopulationReader(reprojectedScenario).readFile(testFile);

		assertPopulationCorrectlyTransformed( originalScenario.getPopulation() , reprojectedScenario.getPopulation() );
	}

	@Test
	public void testOutput_V5() {
		final String testFile = new File(utils.getOutputDirectory() + "/plans.xml.gz").getAbsolutePath();

		// read test population
		Config config = ConfigUtils.createConfig(ExamplesUtils.getTestScenarioURL("berlin"));
		final Scenario originalScenario = ScenarioUtils.createScenario(config);
		// necessary for v4...
		new MatsimNetworkReader(originalScenario.getNetwork()).parse(IOUtils.newUrl(config.getContext(), NET_FILE));
		new PopulationReader(originalScenario).parse(IOUtils.newUrl(config.getContext(), BASE_FILE));

		// write test population with conversion
		new PopulationWriter(
				transformation,
				originalScenario.getPopulation(),
				originalScenario.getNetwork()).writeV5( testFile );

		// read converted population
		final Scenario reprojectedScenario = ScenarioUtils.createScenario(config);
		new PopulationReader(reprojectedScenario).readFile(testFile);

		assertPopulationCorrectlyTransformed( originalScenario.getPopulation() , reprojectedScenario.getPopulation() );
	}

	@Test
	public void testWithControlerAndAttributes() {
		// accept a rounding error of 1 cm.
		// this is used both to compare equality and non-equality, so the more we accept difference between input
		// and output coordinates, the more we require the internally reprojected coordinates to be different.
		// It is thus OK to use a reasonably "high" tolerance compared to usual double comparisons.
		final double epsilon = 0.01;

		// read test population
		URL berlin = ExamplesUtils.getTestScenarioURL("berlin");

		final Scenario originalScenario =
				ScenarioUtils.createScenario(
						ConfigUtils.createConfig(
								getOutputURL()
						));

		new MatsimNetworkReader(originalScenario.getNetwork()).parse(IOUtils.newUrl(berlin, NET_FILE));
		new PopulationReader(originalScenario).parse(IOUtils.newUrl(berlin, BASE_FILE));
		final Config config = ConfigUtils.createConfig(berlin);

		ProjectionUtils.putCRS(originalScenario.getNetwork(), INITIAL_CRS);
		ProjectionUtils.putCRS(originalScenario.getPopulation(), INITIAL_CRS);

		new PopulationWriter(originalScenario.getPopulation()).write(utils.getOutputDirectory()+BASE_FILE);
		new NetworkWriter(originalScenario.getNetwork()).write(utils.getOutputDirectory()+NET_FILE);

		config.global().setCoordinateSystem( TARGET_CRS );

		config.network().setInputFile( NET_FILE );
		config.plans().setInputFile( BASE_FILE );

		// TODO: test also with loading from Controler C'tor?
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		for ( Id<Person> id : originalScenario.getPopulation().getPersons().keySet() ) {
			final Person originalPerson = originalScenario.getPopulation().getPersons().get( id );
			final Person internalPerson = scenario.getPopulation().getPersons().get( id );

			final List<Activity> originalActivities = TripStructureUtils.getActivities( originalPerson.getSelectedPlan() , EmptyStageActivityTypes.INSTANCE );
			final List<Activity> reprojectedActivities = TripStructureUtils.getActivities( internalPerson.getSelectedPlan() , EmptyStageActivityTypes.INSTANCE );

			Assert.assertEquals(
					"unexpected number of activities in reprojected plan",
					originalActivities.size(),
					reprojectedActivities.size() );

			final Iterator<Activity> originalIterator = originalActivities.iterator();
			final Iterator<Activity> reprojectedIterator = reprojectedActivities.iterator();

			while ( originalIterator.hasNext() ) {
				final Activity o = originalIterator.next();
				final Activity r = reprojectedIterator.next();

				Assert.assertNotEquals(
						"No coordinates transform performed!",
						transformation.transform(o.getCoord()),
						r);
			}
		}

		Assert.assertEquals(
				"wrong CRS information after loading",
				TARGET_CRS,
				ProjectionUtils.getCRS(scenario.getPopulation()));

		// do not perform ANY mobsim run
		config.controler().setLastIteration( -1 );
		final String outputDirectory = utils.getOutputDirectory()+"/output/";
		config.controler().setOutputDirectory( outputDirectory );
		final Controler controler = new Controler( scenario );
		controler.run();

		final Scenario dumpedScenario = ScenarioUtils.createScenario( config );
		new PopulationReader( dumpedScenario ).readFile( outputDirectory+"/output_plans.xml.gz" );

		for ( Id<Person> id : originalScenario.getPopulation().getPersons().keySet() ) {
			final Person internalPerson = scenario.getPopulation().getPersons().get( id );
			final Person dumpedPerson = dumpedScenario.getPopulation().getPersons().get( id );

			final List<Activity> internalActivities = TripStructureUtils.getActivities( internalPerson.getSelectedPlan() , EmptyStageActivityTypes.INSTANCE );
			final List<Activity> reprojectedActivities = TripStructureUtils.getActivities( dumpedPerson.getSelectedPlan() , EmptyStageActivityTypes.INSTANCE );

			Assert.assertEquals(
					"unexpected number of activities in reprojected plan",
					internalActivities.size(),
					reprojectedActivities.size() );

			final Iterator<Activity> internalIterator = internalActivities.iterator();
			final Iterator<Activity> reprojectedIterator = reprojectedActivities.iterator();

			while ( internalIterator.hasNext() ) {
				final Activity o = internalIterator.next();
				final Activity r = reprojectedIterator.next();

				Assert.assertEquals(
						"coordinates were reprojected for dump",
						o.getCoord().getX(),
						r.getCoord().getX(),
						epsilon );
				Assert.assertEquals(
						"coordinates were reprojected for dump",
						o.getCoord().getY(),
						r.getCoord().getY(),
						epsilon );
			}
		}
	}

	private URL getOutputURL() {
		try {
			return new File(utils.getOutputDirectory()).toURI().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testWithControlerAndConfigParameters() {
		// accept a rounding error of 1 cm.
		// this is used both to compare equality and non-equality, so the more we accept difference between input
		// and output coordinates, the more we require the internally reprojected coordinates to be different.
		// It is thus OK to use a reasonably "high" tolerance compared to usual double comparisons.
		final double epsilon = 0.01;

		// read test population
		URL berlin = ExamplesUtils.getTestScenarioURL("berlin");
		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig(berlin));
		// necessary for v4...
		new MatsimNetworkReader(originalScenario.getNetwork()).parse(IOUtils.newUrl(berlin, NET_FILE));
		new PopulationReader(originalScenario).parse(IOUtils.newUrl(berlin, BASE_FILE));
		final Config config = ConfigUtils.createConfig(berlin);

		// specify config
		// need to reproject network as well...
		config.network().setInputCRS( INITIAL_CRS );
		config.plans().setInputCRS( INITIAL_CRS );
		config.global().setCoordinateSystem( TARGET_CRS );

		config.network().setInputFile( NET_FILE );
		config.plans().setInputFile( BASE_FILE );

		// TODO: test also with loading from Controler C'tor?
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		for ( Id<Person> id : originalScenario.getPopulation().getPersons().keySet() ) {
			final Person originalPerson = originalScenario.getPopulation().getPersons().get( id );
			final Person internalPerson = scenario.getPopulation().getPersons().get( id );

			final List<Activity> originalActivities = TripStructureUtils.getActivities( originalPerson.getSelectedPlan() , EmptyStageActivityTypes.INSTANCE );
			final List<Activity> reprojectedActivities = TripStructureUtils.getActivities( internalPerson.getSelectedPlan() , EmptyStageActivityTypes.INSTANCE );

			Assert.assertEquals(
					"unexpected number of activities in reprojected plan",
					originalActivities.size(),
					reprojectedActivities.size() );

			final Iterator<Activity> originalIterator = originalActivities.iterator();
			final Iterator<Activity> reprojectedIterator = reprojectedActivities.iterator();

			while ( originalIterator.hasNext() ) {
				final Activity o = originalIterator.next();
				final Activity r = reprojectedIterator.next();

				Assert.assertNotEquals(
						"No coordinates transform performed!",
						transformation.transform(o.getCoord()),
						r);
			}
		}

		// do not perform ANY mobsim run
		config.controler().setLastIteration( -1 );
		final String outputDirectory = utils.getOutputDirectory()+"/output/";
		config.controler().setOutputDirectory( outputDirectory );
		final Controler controler = new Controler( scenario );
		controler.run();

		final Scenario dumpedScenario = ScenarioUtils.createScenario( config );
		new PopulationReader( dumpedScenario ).readFile( outputDirectory+"/output_plans.xml.gz" );

		for ( Id<Person> id : originalScenario.getPopulation().getPersons().keySet() ) {
			final Person internalPerson = scenario.getPopulation().getPersons().get( id );
			final Person dumpedPerson = dumpedScenario.getPopulation().getPersons().get( id );

			final List<Activity> internalActivities = TripStructureUtils.getActivities( internalPerson.getSelectedPlan() , EmptyStageActivityTypes.INSTANCE );
			final List<Activity> reprojectedActivities = TripStructureUtils.getActivities( dumpedPerson.getSelectedPlan() , EmptyStageActivityTypes.INSTANCE );

			Assert.assertEquals(
					"unexpected number of activities in reprojected plan",
					internalActivities.size(),
					reprojectedActivities.size() );

			final Iterator<Activity> internalIterator = internalActivities.iterator();
			final Iterator<Activity> reprojectedIterator = reprojectedActivities.iterator();

			while ( internalIterator.hasNext() ) {
				final Activity o = internalIterator.next();
				final Activity r = reprojectedIterator.next();

				Assert.assertEquals(
						"coordinates were reprojected for dump",
						o.getCoord().getX(),
						r.getCoord().getX(),
						epsilon );
				Assert.assertEquals(
						"coordinates were reprojected for dump",
						o.getCoord().getY(),
						r.getCoord().getY(),
						epsilon );
			}
		}
	}

	public void testConversionAtInput(final String inputFile) {
		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Scenario reprojectedScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		// necessary for v4...
		URL network = IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("berlin"), NET_FILE);
		new MatsimNetworkReader(originalScenario.getNetwork()).parse(network);
		new MatsimNetworkReader(reprojectedScenario.getNetwork()).parse(network);

		new PopulationReader(originalScenario).readFile(inputFile);
		new PopulationReader( INITIAL_CRS, TARGET_CRS, reprojectedScenario).readFile(inputFile);

		final Population originalPopulation = originalScenario.getPopulation();
		final Population reprojectedPopulation = reprojectedScenario.getPopulation();

		assertPopulationCorrectlyTransformed( originalPopulation , reprojectedPopulation );
	}

	private void assertPopulationCorrectlyTransformed(
			final Population originalPopulation,
			final Population reprojectedPopulation) {
		Assert.assertEquals(
				"unexpected size of reprojected population",
				originalPopulation.getPersons().size(),
				reprojectedPopulation.getPersons().size());

		for (Id<Person> personId : originalPopulation.getPersons().keySet()) {
			final Person originalPerson = originalPopulation.getPersons().get(personId);
			final Person reprojectedPerson = reprojectedPopulation.getPersons().get(personId);

			assertPlanCorrectlyTransformed(originalPerson.getSelectedPlan(), reprojectedPerson.getSelectedPlan());
		}
	}

	private void assertPlanCorrectlyTransformed(
			final Plan originalPlan,
			final Plan reprojectedPlan) {
		final List<Activity> originalActivities = TripStructureUtils.getActivities( originalPlan , EmptyStageActivityTypes.INSTANCE );
		final List<Activity> reprojectedActivities = TripStructureUtils.getActivities( reprojectedPlan , EmptyStageActivityTypes.INSTANCE );

		Assert.assertEquals(
				"unexpected number of activities in reprojected plan",
				originalActivities.size(),
				reprojectedActivities.size() );

		final Iterator<Activity> originalIterator = originalActivities.iterator();
		final Iterator<Activity> reprojectedIterator = reprojectedActivities.iterator();

		while ( originalIterator.hasNext() ) {
			final Activity o = originalIterator.next();
			final Activity r = reprojectedIterator.next();

			assertIsCorrectlyTransformed( o.getCoord() , r.getCoord() );
		}
	}

	private void assertIsCorrectlyTransformed( final Coord original , final Coord transformed ) {
		final Coord target = transformation.transform(original);

		Assert.assertEquals(
				"wrong reprojected value",
				target,
				transformed);
	}

}
