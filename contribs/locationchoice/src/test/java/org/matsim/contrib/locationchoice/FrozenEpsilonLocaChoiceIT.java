package org.matsim.contrib.locationchoice;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup.ApproximationLevel;
import org.matsim.contrib.locationchoice.bestresponse.BestReplyLocationChoicePlanStrategy;
import org.matsim.contrib.locationchoice.bestresponse.DCActivityWOFacilitiesScoringFunction;
import org.matsim.contrib.locationchoice.bestresponse.DCScoringFunctionFactory;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceContext;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.*;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;
import static org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup.Algotype.*;
import static org.matsim.contrib.locationchoice.LocationChoiceIT.localCreateConfig;
import static org.matsim.contrib.locationchoice.LocationChoiceIT.localCreatePopWOnePerson;

public class FrozenEpsilonLocaChoiceIT{
	private static final Logger log = Logger.getLogger( FrozenEpsilonLocaChoiceIT.class ) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	/**
	 * This one <em>is</em>, I think, testing the frozen epsilon location choice. kai, mar'19
	 */
	@Test
	public void testLocationChoiceJan2013() {
		//	CONFIG:
		final Config config = localCreateConfig( this.utils.getPackageInputDirectory() + "config.xml");

		config.controler().setOutputDirectory( utils.getOutputDirectory() );
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		DestinationChoiceConfigGroup dccg = ConfigUtils.addOrGetModule( config, DestinationChoiceConfigGroup.class );;

		dccg.setAlgorithm( bestResponse );
		// yy I don't think that this is honoured anywhere in the way this is plugged together here.  kai, mar'19

		dccg.setEpsilonScaleFactors("100.0");
		dccg.setRandomSeed(4711);
		dccg.setTravelTimeApproximationLevel( ApproximationLevel.localRouting );


		// SCENARIO:
		final Scenario scenario = ScenarioUtils.createScenario(config );

		final double scale = 1000. ;
		final double speed = 10. ;
		createExampleNetwork(scenario, scale, speed);

		Link ll1 = scenario.getNetwork().getLinks().get( Id.create(1, Link.class ) ) ;
		ActivityFacility ff1 = scenario.getActivityFacilities().getFacilities().get(Id.create(1, ActivityFacility.class ) );
		Person person = localCreatePopWOnePerson(scenario, ff1, 8.*60*60+5*60 );

		// joint context (based on scenario):
		final DestinationChoiceContext lcContext = new DestinationChoiceContext(scenario) ;
		scenario.addScenarioElement(DestinationChoiceContext.ELEMENT_NAME , lcContext);
		// makes all kinds of stuff available.  also reads or creates the k values both for the persons and the facilities.


		// CONTROL(L)ER:
		Controler controler = new Controler(scenario);

		// set scoring function factory:
		controler.setScoringFunctionFactory( new ScoringFunctionFactory(){
			@Override
			public ScoringFunction createNewScoringFunction( Person person ) {
				SumScoringFunction sum = new SumScoringFunction() ;
				sum.addScoringFunction(new CharyparNagelActivityScoring(lcContext.getParams()) );
				sum.addScoringFunction(new CharyparNagelLegScoring(lcContext.getParams(), scenario.getNetwork(), config.transit().getTransitModes() ) ) ;
				sum.addScoringFunction( new CharyparNagelAgentStuckScoring(lcContext.getParams() ) );
				sum.addScoringFunction( new DCActivityWOFacilitiesScoringFunction(person, lcContext) ) ;
				return sum ;
			}
		}) ;

		// add locachoice strategy factory (used because of defined in config.xml):
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("MyLocationChoice").to(BestReplyLocationChoicePlanStrategy.class);
			}
		});

		// run:
		controler.run();

		assertEquals("number of plans in person.", 2, person.getPlans().size());
		Plan newPlan = person.getSelectedPlan();
		System.err.println( " newPlan: " + newPlan ) ;
		Activity newWork = (Activity) newPlan.getPlanElements().get(2 );
		if ( config.plansCalcRoute().isInsertingAccessEgressWalk() ) {
			newWork = (Activity) newPlan.getPlanElements().get(6);
		}
		System.err.println( " newWork: " + newWork ) ;
		System.err.println( " facilityId: " + newWork.getFacilityId() ) ;
		assertNotNull( newWork ) ;
		assertTrue( !newWork.getFacilityId().equals(Id.create(1, ActivityFacility.class) ) ) ; // should be different from facility number 1 !!
//		assertEquals( Id.create(63, ActivityFacility.class), newWork.getFacilityId() ); // as I have changed the scoring (act is included) I also changed the test here: 27->92
		assertEquals( Id.create(64, ActivityFacility.class), newWork.getFacilityId() ); // as I have changed the scoring (act is included) I also changed the test here: 27->92
	}

	@Test
	public void testLocationChoiceFeb2013NegativeScores() {
		// config:
		final Config config = localCreateConfig( utils.getPackageInputDirectory() + "config.xml");

		config.controler().setOutputDirectory( utils.getOutputDirectory() );

		final DestinationChoiceConfigGroup dccg = ConfigUtils.addOrGetModule(config, DestinationChoiceConfigGroup.class ) ;

		dccg.setAlgorithm( bestResponse );
		// yy Don't think has an influence with setup here. kai, mar'19
		// well, it does not work without this ...

		dccg.setEpsilonScaleFactors("100.0" );
		dccg.setTravelTimeApproximationLevel( ApproximationLevel.localRouting );

		// scenario:
		final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config );

		final double scale = 100000. ;
		final double speed = 1. ;

		createExampleNetwork(scenario, scale, speed);

		Link ll1 = scenario.getNetwork().getLinks().get(Id.create(1, Link.class)) ;
		ActivityFacility ff1 = scenario.getActivityFacilities().getFacilities().get(Id.create(1, ActivityFacility.class)) ;
		Person person = localCreatePopWOnePerson(scenario, ff1, 8.*60*60+5*60 );

		final DestinationChoiceContext lcContext = new DestinationChoiceContext(scenario) ;
		scenario.addScenarioElement(DestinationChoiceContext.ELEMENT_NAME, lcContext);

		// CONTROL(L)ER:
		Controler controler = new Controler(scenario);
		controler.getConfig().controler().setOverwriteFileSetting( OverwriteFileSetting.overwriteExistingFiles );

		// set scoring function
		DCScoringFunctionFactory scoringFunctionFactory = new DCScoringFunctionFactory(controler.getScenario(), lcContext);
		scoringFunctionFactory.setUsingConfigParamsForScoring(true) ;
		controler.setScoringFunctionFactory(scoringFunctionFactory);

		// bind locachoice strategy (selected in localCreateConfig):
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("MyLocationChoice").to( BestReplyLocationChoicePlanStrategy.class );
			}
		});

		controler.run();

		assertEquals("number of plans in person.", 2, person.getPlans().size());
		Plan newPlan = person.getSelectedPlan();
		System.err.println( " newPlan: " + newPlan ) ;
		Activity newWork = (Activity) newPlan.getPlanElements().get(2);
		System.err.println( " newWork: " + newWork ) ;
		System.err.println( " facilityId: " + newWork.getFacilityId() ) ;
		//		assertTrue( !newWork.getFacilityId().equals(Id.create(1) ) ) ; // should be different from facility number 1 !!
		//		assertEquals( Id.create(55), newWork.getFacilityId() );
		System.err.println("shouldn't this change anyways??") ;
	}

	@Test public void testFacilitiesAlongALine() {
		Config config = ConfigUtils.createConfig() ;
		//		final Config config = localCreateConfig( utils.getPackageInputDirectory() + "config.xml");
		config.controler().setLastIteration( 2 );
		config.controler().setOutputDirectory( utils.getOutputDirectory() );
		{
			PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams( "home" ) ;
			params.setTypicalDuration( 12.*3600. );
			config.planCalcScore().addActivityParams( params );
		}
		{
			PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams( "shop" ) ;
			params.setTypicalDuration( 2.*3600. );
			config.planCalcScore().addActivityParams( params );
		}
		{
			StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings( ) ;
			stratSets.setStrategyName( "MyLocationChoice" );
			stratSets.setWeight( 1.0 );
			config.strategy().addStrategySettings( stratSets );
		}

		final DestinationChoiceConfigGroup dccg = ConfigUtils.addOrGetModule(config, DestinationChoiceConfigGroup.class ) ;
		dccg.setEpsilonScaleFactors("10.0" );
		dccg.setAlgorithm( bestResponse );
		dccg.setFlexibleTypes( "shop" );
		dccg.setTravelTimeApproximationLevel( ApproximationLevel.localRouting );
		dccg.setRandomSeed( 2 );
		dccg.setDestinationSamplePercent( 5. );

		// ---

		//		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		// (this will not load anything since there are no files defined)

		Scenario scenario = ScenarioUtils.createScenario( config ) ;
		// don't load anything

		NetworkFactory nf = scenario.getNetwork().getFactory();
		PopulationFactory pf = scenario.getPopulation().getFactory();
		ActivityFacilitiesFactory ff = scenario.getActivityFacilities().getFactory();

		// Construct a network and facilities along a line:
		// 0 --(0-1)-- 1 --(2-1)-- 2 -- ...
		// with a facility of same ID attached to each link.  Have home towards the left, and then select a shop facility, with frozen epsilons.

		Node prevNode;
		{
			Node node = nf.createNode( Id.createNodeId( 0 ) , new Coord( 0., 0. ) ) ;
			scenario.getNetwork().addNode( node );
			prevNode = node ;
		}
		for ( int ii=1 ; ii<1000 ; ii++ ) {
			Node node = nf.createNode( Id.createNodeId( ii ) , new Coord( ii*100, 0.) ) ;
			scenario.getNetwork().addNode( node );
			// ---
			addLinkAndFacility( scenario, nf, ff, prevNode, node );
			addLinkAndFacility( scenario, nf, ff, node, prevNode );
			// ---
			prevNode = node ;
		}
		// ===
		final Id<ActivityFacility> homeFacilityId = Id.create( "0-1", ActivityFacility.class ) ;
		final Id<ActivityFacility> initialShopFacilityId = Id.create( "1-2", ActivityFacility.class );
		for ( int jj=0 ; jj<1000 ; jj++ ){
			Person person = pf.createPerson( Id.createPersonId( jj ) );
			{
				scenario.getPopulation().addPerson( person );
				Plan plan = pf.createPlan();
				person.addPlan( plan );
				// ---
				Activity home = pf.createActivityFromActivityFacilityId( "home", homeFacilityId );
				home.setEndTime( 7. * 3600. );
				plan.addActivity( home );
				{
					Leg leg = pf.createLeg( "car" );
					leg.setDepartureTime( 7. * 3600. );
					leg.setTravelTime( 1800. );
					plan.addLeg( leg );
				}
				{
					Activity shop = pf.createActivityFromActivityFacilityId( "shop", initialShopFacilityId );
					// shop.setMaximumDuration( 3600. ); // does not work for locachoice: time computation is not able to deal with it.  yyyy replace by
					// more central code. kai, mar'19
					shop.setEndTime( 10. * 3600 );
					plan.addActivity( shop );
				}
				{
					Leg leg = pf.createLeg( "car" );
					leg.setDepartureTime( 10. * 3600. );
					leg.setTravelTime( 1800. );
					plan.addLeg( leg );
				}
				{
					Activity home2 = pf.createActivityFromActivityFacilityId( "home", homeFacilityId );
					PopulationUtils.copyFromTo( home, home2 );
					plan.addActivity( home2 );
				}
			}
		}

		final DestinationChoiceContext lcContext = new DestinationChoiceContext(scenario) ;
		scenario.addScenarioElement(DestinationChoiceContext.ELEMENT_NAME, lcContext);

		// CONTROL(L)ER:
		Controler controler = new Controler(scenario);
		controler.getConfig().controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		// set scoring function
		DCScoringFunctionFactory scoringFunctionFactory = new DCScoringFunctionFactory(controler.getScenario(), lcContext);
		scoringFunctionFactory.setUsingConfigParamsForScoring(true) ;
		controler.setScoringFunctionFactory(scoringFunctionFactory);

		// bind locachoice strategy (selected in localCreateConfig):
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("MyLocationChoice").to( BestReplyLocationChoicePlanStrategy.class );
				addControlerListenerBinding().to( KaiAnalysisListener.class ).in( Singleton.class ) ;
			}
		});

		controler.addOverridingModule( new AbstractModule(){
			@Override
			public void install(){
				this.addControlerListenerBinding().toInstance( new ShutdownListener(){
					@Inject Population population ;
					@Inject ActivityFacilities facilities ;
					@Inject TripRouter tripRouter ;
					int binFromVal( double val ) {
						if ( val < 1. ) {
							return 0 ;
						}
						return (int) ( Math.log(val)/Math.log(2) ) ;
					}
					@Override public void notifyShutdown( ShutdownEvent event ){
						if ( event.isUnexpected() ) {
							return ;
						}
						double[] cnt = new double[1000] ;
						for( Person person : population.getPersons().values() ){
							List<Trip> trips = TripStructureUtils.getTrips( person.getSelectedPlan(), tripRouter.getStageActivityTypes() );
							for( Trip trip : trips ){
								Facility facFrom = FacilitiesUtils.toFacility( trip.getOriginActivity(), facilities );
								Facility facTo = FacilitiesUtils.toFacility( trip.getDestinationActivity(), facilities );
								double tripBeelineDistance = CoordUtils.calcEuclideanDistance( facFrom.getCoord(), facTo.getCoord() );
								int bin = binFromVal( tripBeelineDistance ) ;
								cnt[bin] ++ ;
							}
						}
						for ( int ii=0 ; ii<cnt.length ; ii++ ){
							if( cnt[ii] > 0 ){
								log.info( "bin=" + ii + "; cnt=" + cnt[ii] );
							}
						}
						check( 2, cnt[0] );
						check( 24, cnt[8] ) ;
						check( 52, cnt[9] ) ;
						check( 68, cnt[10] ) ;
						check( 130, cnt[11] ) ;
						check( 304, cnt[12] ) ;
						check( 326, cnt[13] ) ;
						check( 654, cnt[14] ) ;
						check( 416, cnt[15] ) ;
						check( 24, cnt[16] ) ;
						check( 0, cnt[17] ) ;

						// The bins are logarithmic, so I would have expected the counts to be approximately constant.  That clearly is not the case.  I don't know why.

					}

					void check( double val, double actual ){
						Assert.assertEquals( val, actual, Math.max( 10, Math.sqrt( val ) ) );
					}

				} );
			}
		} ) ;

		controler.run();

		// yyyy todo make test such that far-away activities have strongly lower proba
		// yyyy todo then make other test with other epsilon to show that average distance depends on this

	}

	public static void addLinkAndFacility( Scenario scenario, NetworkFactory nf, ActivityFacilitiesFactory ff, Node prevNode, Node node ){
		final String str = prevNode.getId() + "-" + node.getId();
		Link link = nf.createLink( Id.createLinkId( str ), prevNode, node ) ;
		Set<String> set = new HashSet<>() ;
		set.add("car" ) ;
		link.setAllowedModes( set ) ;
		link.setLength( CoordUtils.calcEuclideanDistance( prevNode.getCoord(), node.getCoord() ) );
		link.setCapacity( 3600. );
		link.setFreespeed( 50./3.6 );
		scenario.getNetwork().addLink( link );
		// ---
		ActivityFacility af = ff.createActivityFacility( Id.create( str, ActivityFacility.class ), link.getCoord(), link.getId() ) ;
		ActivityOption option = ff.createActivityOption( "shop" ) ;
		af.addActivityOption( option );
		scenario.getActivityFacilities().addActivityFacility( af );
	}


	private static void createExampleNetwork( final Scenario scenario, final double scale, final double speed ) {
		Network network = scenario.getNetwork() ;

		final double x = -scale;
		Node node0 = network.getFactory().createNode(Id.create(0, Node.class ), new Coord(x, (double) 0) ) ;
		network.addNode(node0) ;

		Node node1 = network.getFactory().createNode(Id.create(1, Node.class), new Coord((double) 10, (double) 0)) ;
		network.addNode(node1) ;

		Link link1 = network.getFactory().createLink(Id.create(1, Link.class), node0, node1 );
		network.addLink(link1) ;
		Link link1b = network.getFactory().createLink(Id.create("1b", Link.class), node1, node0 ) ;
		network.addLink(link1b) ;

		final int nNodes = 100 ;
		Random random = new Random(4711) ;
		for ( int ii=2 ; ii<nNodes+2 ; ii++ ) {
			double tmp = Math.PI*(ii-1)/nNodes ;
			Coord coord = new Coord(scale * Math.sin(tmp), scale * Math.cos(tmp));

			Node node = network.getFactory().createNode(Id.create(ii, Node.class), coord ) ;
			network.addNode(node) ;

			double rnd = random.nextDouble() ;
			{
				Link link = network.getFactory().createLink(Id.create(ii, Link.class), node1, node) ;
				link.setLength(rnd*scale) ;
				link.setFreespeed(speed) ;
				link.setCapacity(1.) ;
				network.addLink(link) ;
			}
			{
				Link link = network.getFactory().createLink(Id.create(ii+"b", Link.class), node, node1) ;
				link.setLength(rnd*scale) ;
				link.setFreespeed(speed) ;
				link.setCapacity(1.) ;
				network.addLink(link) ;
			}

			ActivityFacility facility = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create(ii, ActivityFacility.class), coord);
			scenario.getActivityFacilities().addActivityFacility(facility);
			facility.addActivityOption(new ActivityOptionImpl("work") );
		}

		// create one additional facility for the initial activity:
		ActivityFacility facility1 = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create(1, ActivityFacility.class), new Coord(scale, (double) 0));
		scenario.getActivityFacilities().addActivityFacility(facility1);
		facility1.addActivityOption(new ActivityOptionImpl("work"));
		// (as soon as you set a scoring function that looks if activity types match opportunities at facilities, you can only use
		// an activity type that indeed is at the facility)
	}


}
