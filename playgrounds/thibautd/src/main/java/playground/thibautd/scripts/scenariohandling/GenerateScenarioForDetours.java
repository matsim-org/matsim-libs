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
package playground.thibautd.scripts.scenariohandling;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.socnetsim.usage.replanning.GroupReplanningConfigGroup;
import org.matsim.contrib.socnetsim.usage.replanning.GroupReplanningConfigGroup.StrategyParameterSet;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.HouseholdsConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.households.Household;
import org.matsim.households.HouseholdImpl;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsWriterV10;
import playground.thibautd.utils.UniqueIdFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * @author thibautd
 */
public class GenerateScenarioForDetours {
	private static final int N_COUPLES_PER_HH = 5;
	private static final int N_HH_PER_DEST = 5;
	private static final int N_WORK = 500;
	private static final int LENGTH_DETOUR = 100;

	private static final int LINK_CAPACITY = Integer.MAX_VALUE;
	private static final double FREESPEED = 50 / 3.6;

	private static final String P_WORK_PREFIX = "worklink-";

	private static final double X_HOME = 0;
	private static final double X_WORK = 10000;

	private static final Id<Link> homeLinkId = Id.create( "home_sweet_home"  , Link.class);
	private static final Id<Link> driverWorkLinkId = Id.create( "work_bitter_work"  , Link.class);

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

		final MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario( config );
		createNetwork( sc.getNetwork() );
		createPopulation( sc.getPopulation() , sc.getHouseholds() );

		new PopulationWriter( sc.getPopulation() , sc.getNetwork() ).write( outputPopulation );
		new HouseholdsWriterV10( sc.getHouseholds() ).writeFile( outputHouseholds );
		new NetworkWriter( sc.getNetwork() ).write( outputNetwork );
		new ConfigWriter( config ).write( outputConfig );
	}

	private static void createConfig(
			final Config config,
			final String outputNetwork,
			final String outputPopulation,
			final String outputHouseholds) {
		config.subtourModeChoice().setConsiderCarAvailability( true );

		config.network().setInputFile( outputNetwork );
		config.plans().setInputFile( outputPopulation );

		config.scenario().setUseHouseholds( true );
		config.households().setInputFile( outputHouseholds );

		config.qsim().setEndTime( 30 * 3600 );

		config.controler().setLastIteration( 300 );

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
		strategies.setDisableInnovationAfterIteration( 200 );

		/* scope of select exp beta */ {
			final StrategyParameterSet set = new StrategyParameterSet();
			strategies.addStrategyParameterSet( set );

			set.setStrategyName( "SelectExpBeta" );
			set.setWeight( 0.6 );
			set.setIsInnovative( false );
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

		// cleanup: kick out all unused config groups
		final Collection<String> usedModules = new HashSet<String>();
		usedModules.add( SubtourModeChoiceConfigGroup.GROUP_NAME );
		usedModules.add( NetworkConfigGroup.GROUP_NAME );
		usedModules.add( PlansConfigGroup.GROUP_NAME );
		usedModules.add( ScenarioConfigGroup.GROUP_NAME );
		usedModules.add( HouseholdsConfigGroup.GROUP_NAME );
		usedModules.add( QSimConfigGroup.GROUP_NAME );
		usedModules.add( ControlerConfigGroup.GROUP_NAME );
		usedModules.add( PlanCalcScoreConfigGroup.GROUP_NAME );
		usedModules.add( GroupReplanningConfigGroup.GROUP_NAME );

		final Iterator<String> nameIterator = config.getModules().keySet().iterator();
		while ( nameIterator.hasNext() ) {
			final String name = nameIterator.next();
			if ( !usedModules.contains( name ) ) nameIterator.remove();
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
							householdIdFactory.createNextId(Household.class) );
				((HouseholdsImpl) households).addHousehold( household );

				final Id<Link> passengerWorkLinkId = Id.create( P_WORK_PREFIX + workPlaceCount , Link.class);

				final List<Id<Person>> members = new ArrayList<Id<Person>>();
				((HouseholdImpl) household).setMemberIds( members );
				
				for ( int coupleCount = 0; coupleCount < N_COUPLES_PER_HH; coupleCount++ ) {
					/* driver scope */ {
						final Person driver =
							population.getFactory().createPerson(
									personIdFactory.createNextId(Person.class) );
						PersonUtils.setCarAvail(driver, "always");
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
									personIdFactory.createNextId(Person.class) );
						PersonUtils.setCarAvail(passenger, "never");
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
			final Id<Link> workLinkId) {
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
		((NetworkImpl) network).setCapacityPeriod( 1 );

		final UniqueIdFactory nodeIdFactory = new UniqueIdFactory( "node-" );
		final UniqueIdFactory linkIdFactory = new UniqueIdFactory( "link-" );

		final Node homeOriginNode =
			network.getFactory().createNode(
				nodeIdFactory.createNextId(Node.class),
					new Coord(X_HOME, (double) 0));
		network.addNode( homeOriginNode );
		final Node homeDestinationNode =
			network.getFactory().createNode(
				nodeIdFactory.createNextId(Node.class),
					new Coord(X_HOME, (double) 0));
		network.addNode( homeDestinationNode );
		final Node workOriginNode =
			network.getFactory().createNode(
				nodeIdFactory.createNextId(Node.class),
					new Coord(X_WORK, (double) 0));
		network.addNode( workOriginNode );
		final Node workDestinationNode =
			network.getFactory().createNode(
				nodeIdFactory.createNextId(Node.class),
					new Coord(X_WORK, (double) 0));
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
					linkIdFactory.createNextId(Link.class),
					homeDestinationNode,
					workOriginNode ) );

		network.addLink(
				network.getFactory().createLink(
					linkIdFactory.createNextId(Link.class),
					workDestinationNode,
					homeOriginNode ) );

		for ( int i=1; i <= N_WORK; i++ ) {
			final Node node =
				network.getFactory().createNode(
						nodeIdFactory.createNextId(Node.class),
						new Coord(X_WORK, (double) (i * LENGTH_DETOUR)));
			network.addNode( node );

			network.addLink(
					network.getFactory().createLink(
						linkIdFactory.createNextId(Link.class),
						homeDestinationNode,
						node ) );
			network.addLink(
					network.getFactory().createLink(
						Id.create( P_WORK_PREFIX + i , Link.class ),
						node,
						workOriginNode ) );
			network.addLink(
					network.getFactory().createLink(
						linkIdFactory.createNextId(Link.class),
						workDestinationNode,
						node ) );
		}

		for ( Link l : network.getLinks().values() ) {
			l.setCapacity( LINK_CAPACITY );
			l.setLength(
					CoordUtils.calcEuclideanDistance( 
						l.getFromNode().getCoord(),
						l.getToNode().getCoord() ) );
			l.setFreespeed( FREESPEED );
		}
	}
}

