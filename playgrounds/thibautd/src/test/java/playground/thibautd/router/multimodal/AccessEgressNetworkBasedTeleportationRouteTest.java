/* *********************************************************************** *
 * project: org.matsim.*
 * AccessEgressNetworkBasedTeleportationRouteTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.router.multimodal;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Arrays;

/**
 * @author thibautd
 */
public class AccessEgressNetworkBasedTeleportationRouteTest {

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private static AccessEgressNetworkBasedTeleportationRoute createRoute() {
		final AccessEgressNetworkBasedTeleportationRoute route =
			new AccessEgressNetworkBasedTeleportationRoute();

		route.setStartLinkId(Id.create( "bouh", Link.class ) );
		route.setEndLinkId( Id.create( "bwa", Link.class ) );
		route.setDistance( 12364 );
		// test with special values --- JSON is restrictive on numbers
		route.setAccessTime( Double.NaN );
		route.setEgressTime( Double.POSITIVE_INFINITY );
		route.setLinkTime( 4638309 );
		int i = 0;
		route.setLinks( Arrays.asList(
			Id.create( "link-"+(i++), Link.class ),
			Id.create( "link-"+(i++), Link.class ),
			Id.create( "link-"+(i++), Link.class ),
			Id.create( "link-"+(i++), Link.class ),
			Id.create( "link-"+(i++), Link.class ),
			Id.create( "link-"+(i++), Link.class ),
			Id.create( "link-"+(i++), Link.class ),
			Id.create( "link-"+(i++), Link.class )
			) );
		
		return route;
	}

	@Test
	public void testIO() {
		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		final Population pop = sc.getPopulation();

		((PopulationFactoryImpl) pop.getFactory()).setRouteFactory(
				AccessEgressNetworkBasedTeleportationRoute.class,
					new AccessEgressNetworkBasedTeleportationRouteFactory( ) );

		final Person person = pop.getFactory().createPerson( Id.create( "jojo" , Person.class ) );
		pop.addPerson( person );
		final Plan plan = pop.getFactory().createPlan();
		person.addPlan( plan );

		final AccessEgressNetworkBasedTeleportationRoute route = createRoute();
		plan.addActivity( pop.getFactory().createActivityFromLinkId( "bou" , route.getStartLinkId() ) );
		final Leg leg = pop.getFactory().createLeg( "shloumpf" );
		leg.setRoute( route );
		plan.addLeg( leg );
		plan.addActivity( pop.getFactory().createActivityFromLinkId( "bou" , route.getEndLinkId() ) );

		final String file = utils.getOutputDirectory() +"/plans.xml";
		new PopulationWriter( sc.getPopulation() , sc.getNetwork() ).write( file );

		final Scenario insc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );

		((PopulationFactoryImpl) insc.getPopulation().getFactory()).setRouteFactory(
				AccessEgressNetworkBasedTeleportationRoute.class,
					new AccessEgressNetworkBasedTeleportationRouteFactory( ) );

		new MatsimPopulationReader( insc ).readFile( file );

		final Person inperson = insc.getPopulation().getPersons().get( person.getId() );
		final Leg inleg = (Leg) inperson.getSelectedPlan().getPlanElements().get( 1 );

		assertEquals( route,
				(AccessEgressNetworkBasedTeleportationRoute) inleg.getRoute() );
	}

	@Test
	public void testClone() {
		final AccessEgressNetworkBasedTeleportationRoute route = createRoute();
		final AccessEgressNetworkBasedTeleportationRoute clone = route.clone();
		assertEquals( route , clone );
	}

	private void assertEquals(
			final AccessEgressNetworkBasedTeleportationRoute route,
			final AccessEgressNetworkBasedTeleportationRoute clone) {
		Assert.assertEquals(
				"unexpected start link",
				route.getStartLinkId(),
				clone.getStartLinkId() );

		Assert.assertEquals(
				"unexpected end link",
				route.getEndLinkId(),
				clone.getEndLinkId() );

		Assert.assertEquals(
				"unexpected distance",
				route.getDistance(),
				clone.getDistance(),
				MatsimTestUtils.EPSILON );

		Assert.assertEquals(
				"unexpected access time",
				route.getAccessTime(),
				clone.getAccessTime(),
				MatsimTestUtils.EPSILON );

		Assert.assertEquals(
				"unexpected egress time",
				route.getEgressTime(),
				clone.getEgressTime(),
				MatsimTestUtils.EPSILON );

		Assert.assertEquals(
				"unexpected link time",
				route.getLinkTime(),
				clone.getLinkTime(),
				MatsimTestUtils.EPSILON );

		Assert.assertEquals(
				"unexpected links",
				route.getLinks(),
				clone.getLinks() );
	}
}

