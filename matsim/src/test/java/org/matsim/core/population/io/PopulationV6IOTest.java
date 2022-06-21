
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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class PopulationV6IOTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testCoord3dIO() {
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

		Assert.assertFalse( "did not expect Z value in "+readSpeach.getCoord() ,
				readSpeach.getCoord().hasZ() );

		Assert.assertTrue( "did expect T value in "+readTweet.getCoord() ,
				readTweet.getCoord().hasZ() );

		Assert.assertEquals( "unexpected Z value in "+readTweet.getCoord(),
				-100,
				readTweet.getCoord().getZ(),
				MatsimTestUtils.EPSILON );
	}

	@Test
	public void testEmptyPersonAttributesIO() {
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
	public void testPersonAttributesIO() {
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

		Assert.assertEquals( "Unexpected boolean attribute in " + readPerson.getAttributes(),
				person.getAttributes().getAttribute( "brain" ) ,
				readPerson.getAttributes().getAttribute( "brain" ) );

		Assert.assertEquals( "Unexpected String attribute in " + readPerson.getAttributes(),
				person.getAttributes().getAttribute( "party" ) ,
				readPerson.getAttributes().getAttribute( "party" ) );

		Assert.assertEquals( "Unexpected PersonVehicle attribute in " + readPerson.getAttributes(),
				VehicleUtils.getVehicleIds(person) ,
				VehicleUtils.getVehicleIds(readPerson) );
	}

	@Test
	public void testActivityAttributesIO() {
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

		Assert.assertEquals( "Unexpected boolean attribute in " + readAct.getAttributes(),
				act.getAttributes().getAttribute( "makes sense" ) ,
				readAct.getAttributes().getAttribute( "makes sense" ) );

		Assert.assertEquals( "Unexpected Long attribute in " + readAct.getAttributes(),
				act.getAttributes().getAttribute( "length" ) ,
				readAct.getAttributes().getAttribute( "length" ) );
	}

	@Test
	public void testLegAttributesIO() {
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

		final String file = utils.getOutputDirectory()+"/population.xml";
		new PopulationWriter( population ).writeV6( file );

		final Scenario readScenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new PopulationReader( readScenario ).readFile( file );

		final Person readPerson = readScenario.getPopulation().getPersons().get( Id.createPersonId( "Donald Trump" ) );
		final Leg readLeg = (Leg) readPerson.getSelectedPlan().getPlanElements().get( 1 );

		Assert.assertEquals( "Unexpected Double attribute in " + readLeg.getAttributes(),
				leg.getAttributes().getAttribute( "mpg" ) ,
				readLeg.getAttributes().getAttribute( "mpg" ) );
	}

	@Test
	public void testPlanAttributesIO() {
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

		Assert.assertEquals( 				plan.getAttributes().getAttribute( "beauty" ) ,
				readPlan.getAttributes().getAttribute( "beauty" ) );
	}

	@Test
	public void testPopulationAttributesIO() {
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig() );

		population.getAttributes().putAttribute( "type" , "candidates" );
		population.getAttributes().putAttribute( "number" , 2 );

		final String file = utils.getOutputDirectory()+"/population.xml";
		new PopulationWriter( population ).writeV6( file );

		final Scenario readScenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new PopulationReader( readScenario ).readFile( file );

		Assert.assertEquals( "Unexpected numeric attribute in " + readScenario.getPopulation().getAttributes(),
				population.getAttributes().getAttribute( "number" ) ,
				readScenario.getPopulation().getAttributes().getAttribute( "number" ) );

		Assert.assertEquals( "Unexpected String attribute in " + readScenario.getPopulation().getAttributes(),
				population.getAttributes().getAttribute( "type" ) ,
				readScenario.getPopulation().getAttributes().getAttribute( "type" ) );
	}

	// see MATSIM-927, https://matsim.atlassian.net/browse/MATSIM-927
	@Test
	public void testRouteIO() {
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

		Assert.assertEquals(route.getRouteDescription(), ((Leg) scenario.getPopulation().getPersons().get(person1.getId()).getSelectedPlan().getPlanElements().get(1)).getRoute().getRouteDescription());
	}

	// inspired from MATSIM-927, https://matsim.atlassian.net/browse/MATSIM-927
	@Test
	public void testSpecialCharactersIO() {
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
		Assert.assertEquals(act1.getType(), ((Activity) pl1.getPlanElements().get(0)).getType());
		Assert.assertEquals(act1.getLinkId(), ((Activity) pl1.getPlanElements().get(0)).getLinkId());

		Assert.assertEquals(route.getRouteDescription(), ((Leg) scenario.getPopulation().getPersons().get(person1.getId()).getSelectedPlan().getPlanElements().get(1)).getRoute().getRouteDescription());
	}

	@Test
	public void testSingleActivityLocationInfoIO() {
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

		Assert.assertEquals(((Activity) pp1.getPlanElements().get(0)).getFacilityId(), facilityId);
		Assert.assertNull(((Activity) pp1.getPlanElements().get(0)).getCoord());
		Assert.assertNull(((Activity) pp1.getPlanElements().get(0)).getLinkId());

		Assert.assertNull(((Activity) pp1.getPlanElements().get(2)).getFacilityId());
		Assert.assertEquals(((Activity) pp1.getPlanElements().get(2)).getCoord(), coord);
		Assert.assertNull(((Activity) pp1.getPlanElements().get(2)).getLinkId());

		Assert.assertNull(((Activity) pp1.getPlanElements().get(4)).getFacilityId());
		Assert.assertNull(((Activity) pp1.getPlanElements().get(4)).getCoord());
		Assert.assertEquals(((Activity) pp1.getPlanElements().get(4)).getLinkId(), linkId);
	}

}
