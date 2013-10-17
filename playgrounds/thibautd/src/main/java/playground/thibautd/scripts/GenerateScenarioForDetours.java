/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateScenarioForDetours.java
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
package playground.thibautd.scripts;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.households.Household;
import org.matsim.households.HouseholdImpl;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsWriterV10;

import playground.thibautd.config.NonFlatConfigWriter;
import playground.thibautd.socnetsim.run.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.run.GroupReplanningConfigGroup.StrategyParameterSet;
import playground.thibautd.utils.UniqueIdFactory;

/**
 * @author thibautd
 */
public class GenerateScenarioForDetours {
	private static final int N_COUPLES_PER_HH = 5;
	private static final int N_HH_PER_DEST = 10;
	private static final int N_WORK = 1000;
	private static final int LENGTH_DETOUR = 100;

	private static final int LINK_CAPACITY = 9999999;
	private static final double FREESPEED = 70 / 3.6;

	private static final String P_WORK_PREFIX = "worklink-";

	private static final double X_HOME = 0;
	private static final double X_WORK = 10000;

	private static final Id homeLinkId = new IdImpl( "home_sweet_home" );
	private static final Id driverWorkLinkId = new IdImpl( "work_bitter_work" );

	public static void main(final String[] args) {
		final String outputNetwork = args[ 0 ];
		final String outputPopulation = args[ 1 ];
		final String outputHouseholds = args[ 2 ];
		final String outputConfig = args[ 3 ];

		final Config config = ConfigUtils.createConfig();
		createConfig(
				config,
				outputNetwork,
				outputPopulation,
				outputHouseholds );

		final ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario( config );
		createNetwork( sc.getNetwork() );
		createPopulation( sc.getPopulation() , sc.getHouseholds() );

		new PopulationWriter( sc.getPopulation() , sc.getNetwork() ).write( outputPopulation );
		new HouseholdsWriterV10( sc.getHouseholds() ).writeFile( outputHouseholds );
		new NetworkWriter( sc.getNetwork() ).write( outputNetwork );
		new NonFlatConfigWriter( config ).write( outputConfig );
	}

	private static void createConfig(
			final Config config,
			final String outputNetwork,
			final String outputPopulation,
			final String outputHouseholds) {
		config.network().setInputFile( outputNetwork );
		config.plans().setInputFile( outputPopulation );

		config.scenario().setUseHouseholds( true );
		config.households().setInputFile( outputHouseholds );

		/* scope of work params */ {
			final ActivityParams params = new ActivityParams( "work" );
			params.setTypicalDuration( 9 * 3600 );
			config.planCalcScore().addActivityParams( params );
		}
		/* scope of home params */ {
			final ActivityParams params = new ActivityParams( "home" );
			params.setTypicalDuration( 15 * 3600 );
			config.planCalcScore().addActivityParams( params );
		}

		config.controler().setOutputDirectory( "output/detourtestscenario/" );

		final GroupReplanningConfigGroup strategies = new GroupReplanningConfigGroup();
		config.addModule( strategies );

		strategies.setUseLimitedVehicles( false );

		/* scope of select exp beta */ {
			final StrategyParameterSet set = new StrategyParameterSet();
			strategies.addStrategyParameterSet( set );

			set.setStrategyName( "SelectExpBeta" );
			set.setWeight( 0.6 );
		}

		/* scope of mode choice */ {
			final StrategyParameterSet set = new StrategyParameterSet();
			strategies.addStrategyParameterSet( set );

			set.setStrategyName( "SubtourModeChoice" );
			set.setWeight( 0.1 );
		}
	
		/* scope of joint trip mutation */ {
			final StrategyParameterSet set = new StrategyParameterSet();
			strategies.addStrategyParameterSet( set );

			set.setStrategyName( "CliqueJointTripMutator" );
			set.setWeight( 0.1 );
		}

		/* scope of re-route (though not necessary with this network) */ {
			final StrategyParameterSet set = new StrategyParameterSet();
			strategies.addStrategyParameterSet( set );

			set.setStrategyName( "ReRoute" );
			set.setWeight( 0.1 );
		}
	}

	private static void createPopulation(
			final Population population,
			final Households households) {
		final Random random = new Random( 1 );
		final UniqueIdFactory householdIdFactory = new UniqueIdFactory( "household-" );
		final UniqueIdFactory personIdFactory = new UniqueIdFactory( "person-" );

	 	for ( int workPlaceCount = 1; workPlaceCount <= N_WORK; workPlaceCount++ ) {
			for ( int nh = 0; nh < N_HH_PER_DEST; nh++ ) {
				final Household household =
					households.getFactory().createHousehold(
							householdIdFactory.createNextId() );
				((HouseholdsImpl) households).addHousehold( household );

				final Id passengerWorkLinkId = new IdImpl( P_WORK_PREFIX + workPlaceCount );

				final List<Id> members = new ArrayList<Id>();
				((HouseholdImpl) household).setMemberIds( members );
				
				for ( int coupleCount = 0; coupleCount < N_COUPLES_PER_HH; coupleCount++ ) {
					/* driver scope */ {
						final Person driver =
							population.getFactory().createPerson(
									personIdFactory.createNextId() );
						((PersonImpl) driver).setCarAvail( "always" );
						driver.addPlan(
								createPlan(
									random,
									population.getFactory(),
									"car",
									driverWorkLinkId ) );
						population.addPerson( driver );
						members.add( driver.getId() );
					}

					/* passenger scope */ {
						final Person passenger =
							population.getFactory().createPerson(
									personIdFactory.createNextId() );
						((PersonImpl) passenger).setCarAvail( "never" );
						passenger.addPlan(
								createPlan(
									random,
									population.getFactory(),
									"pt",
									passengerWorkLinkId ) );
						population.addPerson( passenger );
						members.add( passenger.getId() );
					}
				}
			}
		}
	}

	private static Plan createPlan(
			final Random random,
			final PopulationFactory factory,
			final String mode,
			final Id workLinkId) {
		final Plan plan = factory.createPlan();

		final Activity a1 =
				factory.createActivityFromLinkId(
					"home",
					homeLinkId );
		plan.addActivity( a1 );
		a1.setEndTime( random.nextDouble() * 24 * 3600 );

		plan.addLeg(
				factory.createLeg(
					mode ) );
		final Activity a2 =
				factory.createActivityFromLinkId(
					"work",
					workLinkId );
		plan.addActivity( a2 );
		a2.setEndTime( random.nextDouble() * 24 * 3600 );

		plan.addLeg(
				factory.createLeg(
					mode ) );
		plan.addActivity(
				factory.createActivityFromLinkId(
					"home",
					homeLinkId ) );

		return plan;
	}

	private static void createNetwork(final Network network) {
		final UniqueIdFactory nodeIdFactory = new UniqueIdFactory( "node-" );
		final UniqueIdFactory linkIdFactory = new UniqueIdFactory( "link-" );

		final Node homeOriginNode =
			network.getFactory().createNode(
				nodeIdFactory.createNextId(),
				new CoordImpl( X_HOME , 0 ) );
		network.addNode( homeOriginNode );
		final Node homeDestinationNode =
			network.getFactory().createNode(
				nodeIdFactory.createNextId(),
				new CoordImpl( X_HOME , 0 ) );
		network.addNode( homeDestinationNode );
		final Node workOriginNode =
			network.getFactory().createNode(
				nodeIdFactory.createNextId(),
				new CoordImpl( X_WORK , 0 ) );
		network.addNode( workOriginNode );
		final Node workDestinationNode =
			network.getFactory().createNode(
				nodeIdFactory.createNextId(),
				new CoordImpl( X_WORK , 0 ) );
		network.addNode( workDestinationNode );

		network.addLink(
				network.getFactory().createLink(
					homeLinkId,
					homeOriginNode,
					homeDestinationNode ) );
		network.addLink(
				network.getFactory().createLink(
					driverWorkLinkId,
					workOriginNode,
					workDestinationNode ) );

		network.addLink(
				network.getFactory().createLink(
					linkIdFactory.createNextId(),
					workDestinationNode,
					homeOriginNode ) );

		for ( int i=1; i <= N_WORK; i++ ) {
			final Node node =
				network.getFactory().createNode(
						nodeIdFactory.createNextId(),
						new CoordImpl(
							X_WORK,
							i * LENGTH_DETOUR ) );
			network.addNode( node );

			network.addLink(
					network.getFactory().createLink(
						linkIdFactory.createNextId(),
						homeDestinationNode,
						node ) );
			network.addLink(
					network.getFactory().createLink(
						new IdImpl( P_WORK_PREFIX + i ),
						node,
						workOriginNode ) );
			network.addLink(
					network.getFactory().createLink(
						linkIdFactory.createNextId(),
						workDestinationNode,
						node ) );
		}

		for ( Link l : network.getLinks().values() ) {
			l.setCapacity( LINK_CAPACITY );
			l.setLength(
					CoordUtils.calcDistance( 
						l.getFromNode().getCoord(),
						l.getToNode().getCoord() ) );
			l.setFreespeed( FREESPEED );
		}
	}
}

