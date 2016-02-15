/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingRouteIOTest.java
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
package eu.eunoiaproject.bikesharing.framework.scenario;

import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Arrays;

/**
 * @author thibautd
 */
public class BikeSharingRouteIOTest {


	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRouteIO() {
		Logger.getLogger(BikeSharingRouteFactory.class).setLevel( Level.TRACE );

		final BikeSharingRoute outRoute =
			new BikeSharingRoute(
					Id.createLinkId( "start_link" ),
					Id.createLinkId( "end_link" ) );

		outRoute.setOriginStation(
				Id.create(
					"origin",
					BikeSharingFacility.class ) );

		outRoute.setDestinationStation(
				Id.create(
					"destination",
					BikeSharingFacility.class ) );

		outRoute.setLinkIds(
				outRoute.getStartLinkId(),
				Arrays.asList(
					Id.createLinkId( "some_link" ),
					Id.createLinkId( "some_other_link" ),
					Id.createLinkId( "yet_another_link" ),
					Id.createLinkId( "so_many?" ),
					Id.createLinkId( "really?" ),
					Id.createLinkId( "enough!" ),
					Id.createLinkId( "ok,_but_this_is_the_last_one" ) ),
				outRoute.getEndLinkId() );
		outRoute.setDistance( 1953 );
		outRoute.setTravelTime( 9785 );

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		BikeSharingScenarioUtils.configurePopulationFactory( sc );

		sc.getPopulation().addPerson( wrapInPlan( outRoute ) );

		final String file = utils.getOutputDirectory() + "/plans.xml";
		new PopulationWriter( sc.getPopulation(), sc.getNetwork() ).write( file );

		final Scenario insc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		BikeSharingScenarioUtils.configurePopulationFactory( insc );

		new MatsimPopulationReader( insc ).readFile( file );

		final BikeSharingRoute inRoute = unWrap( insc.getPopulation() );

		assertSame( outRoute, inRoute );
	}

	private void assertSame(
			final BikeSharingRoute outRoute,
			final BikeSharingRoute inRoute ) {
		Assert.assertEquals(
				"unexpected start link",
				outRoute.getStartLinkId(),
				inRoute.getStartLinkId() );

		Assert.assertEquals(
				"unexpected links",
				outRoute.getLinkIds(),
				inRoute.getLinkIds() );

		Assert.assertEquals(
				"unexpected end link",
				outRoute.getEndLinkId(),
				inRoute.getEndLinkId() );

		Assert.assertEquals(
				"unexpected origin station",
				outRoute.getOriginStation(),
				inRoute.getOriginStation() );

		Assert.assertEquals(
				"unexpected destination station",
				outRoute.getDestinationStation(),
				inRoute.getDestinationStation() );

		Assert.assertEquals(
				"unexpected distance",
				outRoute.getDistance(),
				inRoute.getDistance(),
				MatsimTestUtils.EPSILON );

		Assert.assertEquals(
				"unexpected travel time",
				outRoute.getTravelTime(),
				inRoute.getTravelTime(),
				MatsimTestUtils.EPSILON );
	}

	private BikeSharingRoute unWrap( final Population population ) {
		final Person person = population.getPersons().get( Id.createPersonId( "p" ) );
		final Plan plan = person.getSelectedPlan();
		final Leg leg = (Leg) plan.getPlanElements().get( 1 );

		Assert.assertEquals(
				"unexpected route type",
				BikeSharingRoute.class,
				leg.getRoute().getClass() );

		return (BikeSharingRoute) leg.getRoute();
	}

	private Person wrapInPlan( final BikeSharingRoute outRoute ) {
		final Person person = PopulationUtils.createPerson(Id.createPersonId("p"));

		final Plan plan = new PlanImpl( person );
		person.addPlan( plan );

		plan.addActivity( new ActivityImpl( "stuff" ,  outRoute.getStartLinkId() ) );

		final Leg leg = new LegImpl( BikeSharingConstants.MODE );
		leg.setRoute( outRoute );
		plan.addLeg( leg );

		plan.addActivity( new ActivityImpl( "stuff" ,  outRoute.getEndLinkId() ) );

		return person;
	}
}

