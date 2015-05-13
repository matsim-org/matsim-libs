/* *********************************************************************** *
 * project: org.matsim.*
 * DynamicGroupIdentifierTest.java
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
package playground.thibautd.socnetsim.framework.replanning.grouping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.framework.population.SocialNetwork;
import playground.thibautd.socnetsim.framework.population.SocialNetworkImpl;

/**
 * @author thibautd
 */
public class DynamicGroupIdentifierTest {

	@Test
	public void testNGroupsNoJointPlansNoSocialNet() {
		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		scenario.addScenarioElement( JointPlans.ELEMENT_NAME , new JointPlans() );

		final SocialNetwork socnet = new SocialNetworkImpl();
		scenario.addScenarioElement( SocialNetwork.ELEMENT_NAME , socnet );

		final int nPersons = 100;

		for ( int i=0; i < nPersons; i++ ) {
			final Person p = scenario.getPopulation().getFactory().createPerson( Id.create( "person-"+i , Person.class ) );
			scenario.getPopulation().addPerson( p );
		}

		socnet.addEgos( scenario.getPopulation().getPersons().keySet() );

		test( new Fixture( scenario , nPersons ) );
	}

	@Test
	public void testNGroupsNoJointPlansCompleteSocialNet() {
		//Logger.getLogger( DynamicGroupIdentifier.class ).setLevel( Level.TRACE );
		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		scenario.addScenarioElement( JointPlans.ELEMENT_NAME , new JointPlans() );

		final SocialNetwork socnet = new SocialNetworkImpl();
		scenario.addScenarioElement( SocialNetwork.ELEMENT_NAME , socnet );

		final int nPersons = 100;

		for ( int i=0; i < nPersons; i++ ) {
			final Person p = scenario.getPopulation().getFactory().createPerson( Id.create( "person-"+i , Person.class ) );
			scenario.getPopulation().addPerson( p );
		}

		socnet.addEgos( scenario.getPopulation().getPersons().keySet() );
		for ( Id ego : scenario.getPopulation().getPersons().keySet() ) {
			for ( Id alter : scenario.getPopulation().getPersons().keySet() ) {
				if ( ego == alter ) continue;
				// no need to add bidirectional, as other direction was or will be considered...
				// but prefer to set the social network "reflective", and this makes
				// the monodirectional method fail
				socnet.addBidirectionalTie( ego , alter );
			}
		}

		test( new Fixture( scenario , nPersons / 2 ) );
	}

	@Test
	public void testNGroupsWithJointPlansNoSocialNet() {
		//Logger.getLogger( DynamicGroupIdentifier.class ).setLevel( Level.TRACE );
		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );

		final JointPlans jointPlans = new JointPlans();
		scenario.addScenarioElement( JointPlans.ELEMENT_NAME , jointPlans );

		final SocialNetwork socnet = new SocialNetworkImpl();
		scenario.addScenarioElement( SocialNetwork.ELEMENT_NAME , socnet );

		final int nGroups = 100;
		final int nPersonsPerGroup = 5;

		for ( int i=0; i < nGroups; i++ ) {
			final Person[] persons = new Person[ nPersonsPerGroup ];
			for ( int j=0; j < nPersonsPerGroup; j++ ) {
				final Person p = scenario.getPopulation().getFactory().createPerson( Id.createPersonId( "person-"+i+"."+j ) );
				persons[ j ] = p;
				scenario.getPopulation().addPerson( p );
			}

			for ( int j=1; j < persons.length; j++ ) {
				final Plan plan1 = scenario.getPopulation().getFactory().createPlan();
				final Plan plan2 = scenario.getPopulation().getFactory().createPlan();

				final Person person1 = persons[ j - 1 ];
				final Person person2 = persons[ j ];

				person1.addPlan( plan1 );
				person2.addPlan( plan2 );

				final Map<Id<Person>, Plan> jointPlan = new HashMap< >();
				jointPlan.put( person1.getId() , plan1 );
				jointPlan.put( person2.getId() , plan2 );

				jointPlans.addJointPlan(
						jointPlans.getFactory().createJointPlan(
							jointPlan ) );
			}
		}

		socnet.addEgos( scenario.getPopulation().getPersons().keySet() );

		test( new Fixture( scenario , nGroups ) );
	}

	@Test
	public void testNGroupsWithJointPlansCompleteSocialNet() {
		//Logger.getLogger( DynamicGroupIdentifier.class ).setLevel( Level.TRACE );
		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );

		final JointPlans jointPlans = new JointPlans();
		scenario.addScenarioElement( JointPlans.ELEMENT_NAME , jointPlans );

		final SocialNetwork socnet = new SocialNetworkImpl();
		scenario.addScenarioElement( SocialNetwork.ELEMENT_NAME , socnet );

		final int nGroups = 100;
		final int nPersonsPerGroup = 5;

		for ( int i=0; i < nGroups; i++ ) {
			final Person[] persons = new Person[ nPersonsPerGroup ];
			for ( int j=0; j < nPersonsPerGroup; j++ ) {
				final Person p = scenario.getPopulation().getFactory().createPerson( Id.createPersonId( "person-"+i+"."+j ) );
				persons[ j ] = p;
				scenario.getPopulation().addPerson( p );
			}

			for ( int j=1; j < persons.length; j++ ) {
				final Plan plan1 = scenario.getPopulation().getFactory().createPlan();
				final Plan plan2 = scenario.getPopulation().getFactory().createPlan();

				final Person person1 = persons[ j - 1 ];
				final Person person2 = persons[ j ];

				person1.addPlan( plan1 );
				person2.addPlan( plan2 );

				final Map<Id<Person>, Plan> jointPlan = new HashMap< >();
				jointPlan.put( person1.getId() , plan1 );
				jointPlan.put( person2.getId() , plan2 );

				jointPlans.addJointPlan(
						jointPlans.getFactory().createJointPlan(
							jointPlan ) );
			}
		}

		socnet.addEgos( scenario.getPopulation().getPersons().keySet() );
		for ( Id ego : scenario.getPopulation().getPersons().keySet() ) {
			for ( Id alter : scenario.getPopulation().getPersons().keySet() ) {
				if ( ego == alter ) continue;
				// no need to add bidirectional, as other direction was or will be considered...
				// but prefer to set the social network "reflective", and this makes
				// the monodirectional method fail
				socnet.addBidirectionalTie( ego , alter );
			}
		}

		test( new Fixture( scenario , nGroups / 2 ) );
	}

	private void test( final Fixture fixture ) {
		final GroupIdentifier testee = new DynamicGroupIdentifier( fixture.scenario );

		final Collection<ReplanningGroup> groups = testee.identifyGroups( fixture.scenario.getPopulation() );
		Assert.assertEquals(
				"unexpected number of groups",
				fixture.expectedNGroups,
				groups.size() );

		int n = 0;
		for ( ReplanningGroup g : groups ) n += g.getPersons().size();

		Assert.assertEquals(
				"unexpected number of persons in groups",
				fixture.scenario.getPopulation().getPersons().size(),
				n );
	}

	private static class Fixture {
		public final Scenario scenario;
		public final int expectedNGroups;

		public Fixture(
				final Scenario scenario,
				final int expectedNGroups) {
			this.scenario = scenario;
			this.expectedNGroups = expectedNGroups;
		}
	}
}

