
/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationV6IOTest.java
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


	/**
 * @author thibautd
 */
public class PopulationV6IOTest {
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	 @Test
	 void testCoord3dIO() {
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig() );

		final Person person = population.getFactory().createPerson(Id.createPersonId( "Donald Trump"));
		population.addPerson( person );

		final Plan plan = population.getFactory().createPlan();
		person.addPlan( plan );
		plan.addActivity(population.getFactory().createActivityFromCoord( "speech" , new Coord( 0 , 0 ) ));
		plan.addActivity(population.getFactory().createActivityFromCoord( "tweet" , new Coord( 0 , 0 , -100 ) ));

		final String file = utils.getOutputDirectory()+"/population.xml";
		new PopulationWriter( population ).writeV6( file );

		final Scenario readScenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new PopulationReader( readScenario ).readFile( file );

		final Person readPerson = readScenario.getPopulation().getPersons().get( Id.createPersonId( "Donald Trump" ) );
		final Activity readSpeach = (Activity) readPerson.getSelectedPlan().getPlanElements().get( 0 );
		final Activity readTweet = (Activity) readPerson.getSelectedPlan().getPlanElements().get( 1 );

		Assertions.assertFalse( readSpeach.getCoord().hasZ(),
				"did not expect Z value in "+readSpeach.getCoord() );

		Assertions.assertTrue( readTweet.getCoord().hasZ(),
				"did expect T value in "+readTweet.getCoord() );

		Assertions.assertEquals( -100,
				readTweet.getCoord().getZ(),
				MatsimTestUtils.EPSILON,
				"unexpected Z value in "+readTweet.getCoord() );
	}

	 @Test
	 void testEmptyPersonAttributesIO() {
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig() );

		final Person person = population.getFactory().createPerson(Id.createPersonId( "Donald Trump"));
		population.addPerson( person );

		final String file = utils.getOutputDirectory()+"/population.xml";
		new PopulationWriter( population ).writeV6( file );

		// just check everything works without attributes (dtd validation etc)
		final Scenario readScenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new PopulationReader( readScenario ).readFile( file );
	}

	 @Test
	 void testPersonAttributesIO() {
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig() );

		final Person person = population.getFactory().createPerson(Id.createPersonId( "Donald Trump"));
		population.addPerson( person );

		person.getAttributes().putAttribute( "brain" , false );
		person.getAttributes().putAttribute( "party" , "republican" );

		Map<String, Id<Vehicle>> vehiclesMap = new HashMap<>();
		vehiclesMap.put("car", Id.createVehicleId("limo"));
		VehicleUtils.insertVehicleIdsIntoAttributes(person, vehiclesMap);

		final String file = utils.getOutputDirectory()+"/population.xml";
		new PopulationWriter( population ).writeV6( file );

		final Scenario readScenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new PopulationReader( readScenario ).readFile( file );

		final Person readPerson = readScenario.getPopulation().getPersons().get( Id.createPersonId( "Donald Trump" ) );

		Assertions.assertEquals( person.getAttributes().getAttribute( "brain" ) ,
				readPerson.getAttributes().getAttribute( "brain" ),
				"Unexpected boolean attribute in " + readPerson.getAttributes() );

		Assertions.assertEquals( person.getAttributes().getAttribute( "party" ) ,
				readPerson.getAttributes().getAttribute( "party" ),
				"Unexpected String attribute in " + readPerson.getAttributes() );

		Assertions.assertEquals( VehicleUtils.getVehicleIds(person) ,
				VehicleUtils.getVehicleIds(readPerson),
				"Unexpected PersonVehicle attribute in " + readPerson.getAttributes() );
	}

	 @Test
	 void testActivityAttributesIO() {
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig() );

		final Person person = population.getFactory().createPerson(Id.createPersonId( "Donald Trump"));
		population.addPerson( person );

		final Plan plan = population.getFactory().createPlan();
		person.addPlan( plan );
		final Activity act = population.getFactory().createActivityFromCoord( "speech" , new Coord( 0 , 0 ) );
		plan.addActivity( act );

		act.getAttributes().putAttribute( "makes sense" , false );
		act.getAttributes().putAttribute( "length" , 1895L );

		final String file = utils.getOutputDirectory()+"/population.xml";
		new PopulationWriter( population ).writeV6( file );

		final Scenario readScenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new PopulationReader( readScenario ).readFile( file );

		final Person readPerson = readScenario.getPopulation().getPersons().get( Id.createPersonId( "Donald Trump" ) );
		final Activity readAct = (Activity) readPerson.getSelectedPlan().getPlanElements().get( 0 );

		Assertions.assertEquals( act.getAttributes().getAttribute( "makes sense" ) ,
				readAct.getAttributes().getAttribute( "makes sense" ),
				"Unexpected boolean attribute in " + readAct.getAttributes() );

		Assertions.assertEquals( act.getAttributes().getAttribute( "length" ) ,
				readAct.getAttributes().getAttribute( "length" ),
				"Unexpected Long attribute in " + readAct.getAttributes() );
	}

	 @Test
	 void testLegAttributesIO() {
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig() );

		final Person person = population.getFactory().createPerson(Id.createPersonId( "Donald Trump"));
		population.addPerson( person );

		final Plan plan = population.getFactory().createPlan();
		person.addPlan( plan );
		final Leg leg = population.getFactory().createLeg( "SUV" );
		plan.addActivity( population.getFactory().createActivityFromLinkId( "speech" , Id.createLinkId( 1 )));
		plan.addLeg( leg );
		plan.addActivity( population.getFactory().createActivityFromLinkId( "tweet" , Id.createLinkId( 2 )));

		leg.getAttributes().putAttribute( "mpg" , 0.000001d );
		leg.setRoutingMode(TransportMode.car);

		final String file = utils.getOutputDirectory()+"/population.xml";
		new PopulationWriter( population ).writeV6( file );

		final Scenario readScenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new PopulationReader( readScenario ).readFile( file );

		final Person readPerson = readScenario.getPopulation().getPersons().get( Id.createPersonId( "Donald Trump" ) );
		final Leg readLeg = (Leg) readPerson.getSelectedPlan().getPlanElements().get( 1 );

		Assertions.assertEquals(1, readLeg.getAttributes().size(), "Expected a single leg attribute.");
		Assertions.assertEquals( leg.getAttributes().getAttribute( "mpg" ) ,
				readLeg.getAttributes().getAttribute( "mpg" ),
				"Unexpected Double attribute in " + readLeg.getAttributes() );

		Assertions.assertEquals(TransportMode.car, readLeg.getRoutingMode(), "RoutingMode not set in Leg.");
	}

	 @Test
	 void testLegAttributesLegacyIO() {
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig() );

		final Person person = population.getFactory().createPerson(Id.createPersonId( "Donald Trump"));
		population.addPerson( person );

		final Plan plan = population.getFactory().createPlan();
		person.addPlan( plan );
		final Leg leg = population.getFactory().createLeg( "SUV" );
		plan.addActivity( population.getFactory().createActivityFromLinkId( "speech" , Id.createLinkId( 1 )));
		plan.addLeg( leg );
		plan.addActivity( population.getFactory().createActivityFromLinkId( "tweet" , Id.createLinkId( 2 )));

		leg.getAttributes().putAttribute( "mpg" , 0.000001d );
		leg.getAttributes().putAttribute(TripStructureUtils.routingMode, TransportMode.car);

		final String file = utils.getOutputDirectory()+"/population.xml";
		new PopulationWriter( population ).writeV6( file );

		final Scenario readScenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new PopulationReader( readScenario ).readFile( file );

		final Person readPerson = readScenario.getPopulation().getPersons().get( Id.createPersonId( "Donald Trump" ) );
		final Leg readLeg = (Leg) readPerson.getSelectedPlan().getPlanElements().get( 1 );

		Assertions.assertEquals(1, readLeg.getAttributes().size(), "Expected a single leg attribute.");
		Assertions.assertEquals( leg.getAttributes().getAttribute( "mpg" ) ,
				readLeg.getAttributes().getAttribute( "mpg" ),
				"Unexpected Double attribute in " + readLeg.getAttributes() );

		Assertions.assertEquals(TransportMode.car, readLeg.getRoutingMode(), "RoutingMode not set in Leg.");
	}

	 @Test
	 void testPlanAttributesIO() {
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig() );

		final Person person = population.getFactory().createPerson(Id.createPersonId( "Donald Trump"));
		population.addPerson( person );

		final Plan plan = population.getFactory().createPlan();
		person.addPlan( plan );
		final Leg leg = population.getFactory().createLeg( "SUV" );
		plan.addActivity( population.getFactory().createActivityFromLinkId( "speech" , Id.createLinkId( 1 )));
		plan.addLeg( leg );
		plan.addActivity( population.getFactory().createActivityFromLinkId( "tweet" , Id.createLinkId( 2 )));

		plan.getAttributes().putAttribute( "beauty" , 0.000001d );

		final String file = utils.getOutputDirectory()+"/population.xml";
		new PopulationWriter( population ).writeV6( file );

		final Scenario readScenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new PopulationReader( readScenario ).readFile( file );

		final Person readPerson = readScenario.getPopulation().getPersons().get( Id.createPersonId( "Donald Trump" ) );
		final Plan readPlan = readPerson.getSelectedPlan() ;

		Assertions.assertEquals( 				plan.getAttributes().getAttribute( "beauty" ) ,
				readPlan.getAttributes().getAttribute( "beauty" ) );
	}

	 @Test
	 void testPopulationAttributesIO() {
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig() );

		population.getAttributes().putAttribute( "type" , "candidates" );
		population.getAttributes().putAttribute( "number" , 2 );

		final String file = utils.getOutputDirectory()+"/population.xml";
		new PopulationWriter( population ).writeV6( file );

		final Scenario readScenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new PopulationReader( readScenario ).readFile( file );

		Assertions.assertEquals( population.getAttributes().getAttribute( "number" ) ,
				readScenario.getPopulation().getAttributes().getAttribute( "number" ),
				"Unexpected numeric attribute in " + readScenario.getPopulation().getAttributes() );

		Assertions.assertEquals( population.getAttributes().getAttribute( "type" ) ,
				readScenario.getPopulation().getAttributes().getAttribute( "type" ),
				"Unexpected String attribute in " + readScenario.getPopulation().getAttributes() );
	}

	 // see MATSIM-927, https://matsim.atlassian.net/browse/MATSIM-927
	 @Test
	 void testRouteIO() {
		Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		PopulationFactory pf = population.getFactory();

		Person person1 = pf.createPerson(Id.create("1", Person.class));
		Plan plan = pf.createPlan();
		Activity act1 = pf.createActivityFromCoord("home", new Coord(0, 0));
		act1.setEndTime(8*3600);
		Leg leg = pf.createLeg("special");
		GenericRouteImpl route = new GenericRouteImpl(Id.create("a", Link.class), Id.create("b", Link.class));
		route.setRouteDescription("can contain & some \" special > characters < .");
		leg.setRoute(route);
		Activity act2 = pf.createActivityFromCoord("work", new Coord(1000, 1000));

		plan.addActivity(act1);
		plan.addLeg(leg);
		plan.addActivity(act2);
		person1.addPlan(plan);
		population.addPerson(person1);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new PopulationWriter(population).write(out);

		// ----

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		new PopulationReader(scenario).parse(in);

		Assertions.assertEquals(route.getRouteDescription(), ((Leg) scenario.getPopulation().getPersons().get(person1.getId()).getSelectedPlan().getPlanElements().get(1)).getRoute().getRouteDescription());
	}

	 // inspired from MATSIM-927, https://matsim.atlassian.net/browse/MATSIM-927
	 @Test
	 void testSpecialCharactersIO() {
		Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		PopulationFactory pf = population.getFactory();

		population.setName("I\"m &<special>");

		Person person1 = pf.createPerson(Id.create("<oh>&\"😀", Person.class));
		Plan plan = pf.createPlan();
		Activity act1 = pf.createActivityFromLinkId("><&\"", Id.create(">><<\"&\"", Link.class));
		act1.setEndTime(8*3600);
		Leg leg = pf.createLeg("ho\"me>wo&rk");
		GenericRouteImpl route = new GenericRouteImpl(Id.create("a", Link.class), Id.create("b", Link.class));
		route.setRouteDescription("can contain & some \" special > characters < .");
		leg.setRoute(route);
		Activity act2 = pf.createActivityFromCoord("wo\'\"'\\\"rk", new Coord(1000, 1000));

		plan.addActivity(act1);
		plan.addLeg(leg);
		plan.addActivity(act2);
		person1.addPlan(plan);
		population.addPerson(person1);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new PopulationWriter(population).write(out);

		// ----

//		String xml = new String(out.toByteArray());
//		System.out.println(xml);

		// ----

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		new PopulationReader(scenario).parse(in);

		Person p1 = scenario.getPopulation().getPersons().get(person1.getId()); // this already checks the id
		Plan pl1 = p1.getSelectedPlan();
		Assertions.assertEquals(act1.getType(), ((Activity) pl1.getPlanElements().get(0)).getType());
		Assertions.assertEquals(act1.getLinkId(), ((Activity) pl1.getPlanElements().get(0)).getLinkId());

		Assertions.assertEquals(route.getRouteDescription(), ((Leg) scenario.getPopulation().getPersons().get(person1.getId()).getSelectedPlan().getPlanElements().get(1)).getRoute().getRouteDescription());
	}

	 @Test
	 void testSingleActivityLocationInfoIO() {
		Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		PopulationFactory pf = population.getFactory();

		Person person1 = pf.createPerson(Id.create("1", Person.class));
		Plan plan = pf.createPlan();
		Id<ActivityFacility> facilityId = Id.create("home_1", ActivityFacility.class);
		Activity act1 = pf.createActivityFromActivityFacilityId("home", facilityId);
		act1.setEndTime(8 * 3600);
		Leg leg1 = pf.createLeg("car");
		Coord coord = new Coord(1000, 1000);
		Activity act2 = pf.createActivityFromCoord("work", coord);
		act2.setEndTime(16 * 3600);
		Leg leg2 = pf.createLeg("car");
		Id<Link> linkId = Id.createLinkId("link_1");
		Activity act3 = pf.createActivityFromLinkId("work", linkId);
		act2.setEndTime(24 * 3600);

		plan.addActivity(act1);
		plan.addLeg(leg1);
		plan.addActivity(act2);
		plan.addLeg(leg2);
		plan.addActivity(act3);
		person1.addPlan(plan);
		population.addPerson(person1);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new PopulationWriter(population).write(out);

		// ----

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		new PopulationReader(scenario).parse(in);

		Person p1 = scenario.getPopulation().getPersons().get(person1.getId());
		Plan pp1 = p1.getPlans().get(0);

		Assertions.assertEquals(((Activity) pp1.getPlanElements().get(0)).getFacilityId(), facilityId);
		Assertions.assertNull(((Activity) pp1.getPlanElements().get(0)).getCoord());
		Assertions.assertNull(((Activity) pp1.getPlanElements().get(0)).getLinkId());

		Assertions.assertNull(((Activity) pp1.getPlanElements().get(2)).getFacilityId());
		Assertions.assertEquals(((Activity) pp1.getPlanElements().get(2)).getCoord(), coord);
		Assertions.assertNull(((Activity) pp1.getPlanElements().get(2)).getLinkId());

		Assertions.assertNull(((Activity) pp1.getPlanElements().get(4)).getFacilityId());
		Assertions.assertNull(((Activity) pp1.getPlanElements().get(4)).getCoord());
		Assertions.assertEquals(((Activity) pp1.getPlanElements().get(4)).getLinkId(), linkId);
	}

	 @Test
	 void testPopulationCoordinateTransformationIO() {
		String outputDirectory = utils.getOutputDirectory();

		// Create a population with CRS EPSG:25832
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		ProjectionUtils.putCRS(population, "EPSG:25832");
		final Person person = population.getFactory().createPerson(Id.createPersonId("Donald Trump"));
		population.addPerson(person);
		Plan plan = population.getFactory().createPlan();
		plan.addActivity(population.getFactory().createInteractionActivityFromCoord("home", new Coord(712568.0, 256600.0)));
		person.addPlan(plan);
		new PopulationWriter(population).write(outputDirectory + "output.xml");

		// Read in again, but with CRS EPSG:4326
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:4326");
		Scenario scenario = ScenarioUtils.createScenario(config);
		final String targetCRS = config.global().getCoordinateSystem();
		final String internalCRS = config.global().getCoordinateSystem();
		final PopulationReader reader = new PopulationReader(targetCRS, internalCRS, scenario);
		reader.putAttributeConverters(Collections.emptyMap());
		reader.readFile(outputDirectory + "output.xml");
		Person inputPerson = scenario.getPopulation().getPersons().values().iterator().next();
		Activity act = (Activity) inputPerson.getPlans().get(0).getPlanElements().get(0);
		Assertions.assertEquals(10.911495969392414, act.getCoord().getX(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2.3202288392002424, act.getCoord().getY(), MatsimTestUtils.EPSILON);

	}

}
