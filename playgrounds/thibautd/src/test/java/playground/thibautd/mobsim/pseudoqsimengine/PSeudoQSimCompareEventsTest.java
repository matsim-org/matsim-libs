/* *********************************************************************** *
 * project: org.matsim.*
 * PSeudoQSimCompareEventsTest.java
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
package playground.thibautd.mobsim.pseudoqsimengine;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import playground.thibautd.mobsim.CompareEventsUtils;
import playground.thibautd.pseudoqsim.PseudoSimConfigGroup;
import playground.thibautd.pseudoqsim.pseudoqsimengine.QSimWithPseudoEngineFactory;
import playground.thibautd.scripts.scenariohandling.CreateGridNetworkWithDimensions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author thibautd
 */
//@Ignore( "fails since refactoring in DriverAgent. to fix!!!" )
public class PSeudoQSimCompareEventsTest {
	private static final boolean DUMP_EVENTS = false;

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testEventsSimilarToQsim() {
		testEventsSimilarToQsim( false , 1 );
	}

	@Test
	public void testEventsSimilarToQsimParallel() {
		//Logger.getLogger( "playground.thibautd.mobsim.pseudoqsimengine" ).setLevel( Level.TRACE );
		testEventsSimilarToQsim( false , 15 );
	}

	@Test
	public void testEventsSimilarToQsimPt() {
		testEventsSimilarToQsim( true , 1 );
	}

	@Test
	public void testEventsSimilarToQsimParallelPt() {
		//Logger.getLogger( "playground.thibautd.mobsim.pseudoqsimengine" ).setLevel( Level.TRACE );
		testEventsSimilarToQsim( true , 15 );
	}


	private void testEventsSimilarToQsim(
			final boolean useTransit,
			final int nThreads) {
		utils.getOutputDirectory(); // intended side effect: delete content
		final Scenario scenario = createTestScenario( useTransit );

		final PseudoSimConfigGroup conf = new PseudoSimConfigGroup();
		conf.setNThreads( nThreads );
		scenario.getConfig().addModule( conf );
		final TravelTimeCalculator travelTime =
			new TravelTimeCalculator(
					scenario.getNetwork(),
					scenario.getConfig().travelTimeCalculator());

		CompareEventsUtils.testEventsSimilarToQsim(
				scenario,
				new PlanRouter(
						createTripRouter( travelTime , scenario ) ),
				new QSimFactory(),
				DUMP_EVENTS ? utils.getOutputDirectory()+"/qSimEvent.xml" : null,
				new QSimWithPseudoEngineFactory(
					travelTime.getLinkTravelTimes() ),
				DUMP_EVENTS ? utils.getOutputDirectory()+"/pSimEvent.xml" : null,
				travelTime,
				false );
	}

	private TripRouter createTripRouter(TravelTimeCalculator travelTime, Scenario scenario) {
		final TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults();
		builder.setTravelDisutility(
				new RandomizingTimeDistanceTravelDisutility.Builder( TransportMode.car, scenario.getConfig().planCalcScore() ).createTravelDisutility(
						travelTime.getLinkTravelTimes() ));
		builder.setTravelTime( travelTime.getLinkTravelTimes() );
		return builder.build( scenario ).get();
	}


	private Scenario createTestScenario(final boolean useTransit) {
		final Config config = ConfigUtils.createConfig();
		if ( useTransit ) {
			config.transit().setUseTransit( true );

			config.qsim().setEndTime( 30 * 3600 );
		}
		final Scenario sc = ScenarioUtils.createScenario( config );
		CreateGridNetworkWithDimensions.createNetwork(
				sc.getNetwork(),
				75 / 3.6,
				1000,
				100,
				20,
				20 );
		if ( useTransit ) createSchedule( sc );

		final Random random = new Random( 1234 );
		final List<Id> linkIds = new ArrayList<Id>( sc.getNetwork().getLinks().keySet() );
		for ( String mode : new String[]{ TransportMode.car , TransportMode.pt } ) {
			for ( int i = 0; i < 2000; i++ ) {
				final Person person = sc.getPopulation().getFactory().createPerson( Id.create( mode+"."+i , Person.class ) );
				sc.getPopulation().addPerson( person );

				final Plan plan = sc.getPopulation().getFactory().createPlan();
				person.addPlan( plan );

				for ( int j=0; j<3; j++ ) {
					final Activity firstActivity = 
							sc.getPopulation().getFactory().createActivityFromLinkId(
								"h",
								linkIds.get(
									random.nextInt(
										linkIds.size() ) ) );

					// everybody leaves at the same time, to have some congestion
					firstActivity.setEndTime( j * 10 );
					plan.addActivity( firstActivity );

					plan.addLeg( sc.getPopulation().getFactory().createLeg( mode ) );
				}

				plan.addActivity(
						sc.getPopulation().getFactory().createActivityFromLinkId(
							"h",
							linkIds.get(
								random.nextInt(
									linkIds.size() ) ) ) );

			}

			/* make sure at least one agent has a zero-length trip */ {
				final Person person = sc.getPopulation().getFactory().createPerson( Id.create( mode+".jojo" , Person.class ) );
				sc.getPopulation().addPerson( person );

				final Plan plan = sc.getPopulation().getFactory().createPlan();
				person.addPlan( plan );

				final Id linkId =
							linkIds.get(
								random.nextInt(
									linkIds.size() ) );
				for ( int j=0; j<3; j++ ) {
					final Activity firstActivity = 
							sc.getPopulation().getFactory().createActivityFromLinkId(
								"h",
								linkId );

					// everybody leaves at the same time, to have some congestion
					firstActivity.setEndTime( j * 10 );
					plan.addActivity( firstActivity );

					plan.addLeg( sc.getPopulation().getFactory().createLeg( mode ) );
				}

				plan.addActivity(
						sc.getPopulation().getFactory().createActivityFromLinkId(
							"h",
							linkId ) );
			}
		}

		// fill coords
		for ( Person person : sc.getPopulation().getPersons().values() ) {
			for ( Plan plan : person.getPlans() ) {
				for ( PlanElement pe : plan.getPlanElements() ) {
					if ( pe instanceof Activity ) {
						final ActivityImpl act = (ActivityImpl) pe;
						act.setCoord(
								sc.getNetwork().getLinks().get(
									act.getLinkId() ).getCoord() );
					}
				}
			}
		}
		return sc;
	}

	private void createSchedule(final Scenario sc) {
		final TransitSchedule schedule = ((MutableScenario) sc).getTransitSchedule();
		final TransitScheduleFactory factory = schedule.getFactory();

		final Vehicles vehicles = ((MutableScenario) sc).getTransitVehicles();

		final VehicleType vehicleType =
			vehicles.getFactory().createVehicleType(
					Id.create( "vehicle" , VehicleType.class ) );
		{
			final VehicleCapacity cap = vehicles.getFactory().createVehicleCapacity();
			cap.setSeats( 100 );
			cap.setStandingRoom( 50 );
			vehicleType.setCapacity( cap );
		}

		vehicles.addVehicleType(
				vehicleType );

		final FreespeedTravelTimeAndDisutility tt = new FreespeedTravelTimeAndDisutility( sc.getConfig().planCalcScore() );
		final Dijkstra dijkstra = new Dijkstra( sc.getNetwork() , tt , tt );

		final List<Id> linkIds = new ArrayList<Id>( sc.getNetwork().getLinks().keySet() );
		final Random random = new Random( 987 );
		for ( int i=0; i < 100; i++ ) {
			final Id originLinkId = linkIds.get( random.nextInt( linkIds.size() ) );
			final Id destinationLinkId = linkIds.get( random.nextInt( linkIds.size() ) );

			final Path path = dijkstra.calcLeastCostPath(
					sc.getNetwork().getLinks().get( originLinkId ).getToNode(),
					sc.getNetwork().getLinks().get( destinationLinkId ).getFromNode(),
					0,
					null,
					null );

			final List<Id<Link>> ids = new ArrayList<Id<Link>>();
			for ( Link l : path.links ) ids.add( l.getId() );
			final NetworkRoute route =
					new LinkNetworkRouteImpl(
							originLinkId,
							ids,
							destinationLinkId );

			final TransitLine line = factory.createTransitLine( Id.create( "line-"+i , TransitLine.class ) );
			final List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
			stops.add( factory.createTransitRouteStop(
						createStop(
							factory,
							Id.create( "line-"+i+"-o" , TransitStopFacility.class ),
							sc.getNetwork().getLinks().get( originLinkId ) ),
						0,
						0));
			double time = tt.getLinkTravelTime(
					sc.getNetwork().getLinks().get( originLinkId ),
					0,
					null,
					null);
			for ( Link l : path.links ) {
				stops.add( factory.createTransitRouteStop(
							createStop(
								factory,
								Id.create( "line-"+i+"-"+l.getId() , TransitStopFacility.class ),
								l),
							time,
							0));
				time += tt.getLinkTravelTime(
						l,
						time,
						null,
						null);
			}
			stops.add( factory.createTransitRouteStop(
						createStop(
							factory,
							Id.create( "line-"+i+"-d" , TransitStopFacility.class ),
							sc.getNetwork().getLinks().get( destinationLinkId ) ),
						time,
						0));

			for ( TransitRouteStop stop : stops ) {
				schedule.addStopFacility( stop.getStopFacility() );
			}

			final TransitRoute transitRoute =
					factory.createTransitRoute(
						Id.create( "line-"+i+"-route" , TransitRoute.class ),
						route,
						stops,
						"pt" );

			line.addRoute( transitRoute );

			for ( double depTime = 0 ; depTime <= 12 * 3600; depTime += 20 * 60 ) {
				final Departure dep =
						factory.createDeparture(
							Id.create( "line-"+i+"-"+depTime , Departure.class ),
							depTime );
				dep.setVehicleId( Id.create( "veh-"+dep.getId() , Vehicle.class ) );
				vehicles.addVehicle(
						vehicles.getFactory().createVehicle(
							dep.getVehicleId(),
							vehicleType ) );
				transitRoute.addDeparture( dep );
			}

			schedule.addTransitLine( line );
		}
	}

	private TransitStopFacility createStop(
			final TransitScheduleFactory factory,
			final Id<TransitStopFacility> id,
			final Link link) {
		final TransitStopFacility f =
						factory.createTransitStopFacility(
								id,
								link.getCoord(),
								false );
		f.setLinkId( link.getId() );
		return f;
	}

}

