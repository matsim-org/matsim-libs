package org.matsim.contrib.locationchoice.frozenepsilons;

import static org.junit.jupiter.api.Assertions.*;
import static org.matsim.contrib.locationchoice.LocationChoiceIT.localCreatePopWOnePerson;
import static org.matsim.contrib.locationchoice.frozenepsilons.FrozenTastesConfigGroup.Algotype;
import static org.matsim.contrib.locationchoice.frozenepsilons.FrozenTastesConfigGroup.Algotype.bestResponse;
import static org.matsim.contrib.locationchoice.frozenepsilons.FrozenTastesConfigGroup.ApproximationLevel;
import static org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
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
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import com.google.inject.Inject;
import com.google.inject.Singleton;

public class FrozenEpsilonLocaChoiceIT{
	private static final Logger log = LogManager.getLogger( FrozenEpsilonLocaChoiceIT.class ) ;

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils() ;

	/**
	 * This one <em>is</em>, I think, testing the frozen epsilon location choice. kai, mar'19
	 */
	/*
	 * This test might fail if it is run as part of all LocationChoice-Tests in IntelliJ or Eclipse,
	 * but it runs correctly when being run from Maven or individually.
	 * This is likely due relying somewhere on an internal iteration order (likely in IdMap), which
	 * may be different if other tests have run before in the same JVM and thus Id-indices are different
	 * than when running this test alone.
	 *
	 * For Maven, the surefire-plugin can be configured to run each test individually in a separate JVM which
	 * solves this problem, but I don't know how to solve this in IntelliJ or Eclipse.
	 * -mrieser/2019Sept26
	 *
	 * Confirmed: This tests fails when called AFTER BestReplyIT, michalm/mar'20
	 */
	@Test
	void testLocationChoiceJan2013() {
		//	CONFIG:
		final Config config = localCreateConfig( this.utils.getPackageInputDirectory() + "../config2.xml");

		config.controller().setOutputDirectory( utils.getOutputDirectory() );
		config.controller().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
                {
                        FrozenTastesConfigGroup dccg = ConfigUtils.addOrGetModule( config, FrozenTastesConfigGroup.class );

                        dccg.setAlgorithm( bestResponse );
                        // yy I don't think that this is honoured anywhere in the way this is plugged together here.  kai, mar'19

                        dccg.setEpsilonScaleFactors( "100.0" );
                        dccg.setRandomSeed( 4711 );
                        dccg.setTravelTimeApproximationLevel( ApproximationLevel.localRouting );
                }
		config.routing().setRoutingRandomness(0.);

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

		assertEquals(2, person.getPlans().size(), "number of plans in person.");
		Plan newPlan = person.getSelectedPlan();
		System.err.println( " newPlan: " + newPlan ) ;
		Activity newWork = (Activity) newPlan.getPlanElements().get(2 );
		if ( !config.routing().getAccessEgressType().equals(RoutingConfigGroup.AccessEgressType.none) ) {
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
	void testLocationChoiceFeb2013NegativeScores() {
		// config:
		final Config config = localCreateConfig( utils.getPackageInputDirectory() + "../config2.xml");

		config.controller().setOutputDirectory( utils.getOutputDirectory() );

		final FrozenTastesConfigGroup dccg = ConfigUtils.addOrGetModule(config, FrozenTastesConfigGroup.class ) ;

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
		controler.getConfig().controller().setOverwriteFileSetting( OverwriteFileSetting.overwriteExistingFiles );

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

		assertEquals(2, person.getPlans().size(), "number of plans in person.");
		Plan newPlan = person.getSelectedPlan();
		System.err.println( " newPlan: " + newPlan ) ;
		Activity newWork = (Activity) newPlan.getPlanElements().get(2);
		System.err.println( " newWork: " + newWork ) ;
		System.err.println( " facilityId: " + newWork.getFacilityId() ) ;
		//		assertTrue( !newWork.getFacilityId().equals(Id.create(1) ) ) ; // should be different from facility number 1 !!
		//		assertEquals( Id.create(55), newWork.getFacilityId() );
		System.err.println("shouldn't this change anyways??") ;
	}

	enum RunType { shortRun, medRun, longRun }

	@Test
	void testFacilitiesAlongALine() {
		RunType runType = RunType.shortRun ;
		Config config = ConfigUtils.createConfig() ;
		switch( runType ) {
			case shortRun:
				config.controller().setLastIteration( 2 );
				break;
			case medRun:
				config.controller().setLastIteration( 100 );
				break;
			case longRun:
				config.controller().setLastIteration( 1000 );
				break;
			default:
				throw new RuntimeException( Gbl.NOT_IMPLEMENTED) ;
		}
		config.controller().setOutputDirectory( utils.getOutputDirectory() );

		config.scoring().addActivityParams( new ActivityParams( "home" ).setTypicalDuration( 12.*3600. ) );
		config.scoring().addActivityParams( new ActivityParams( "shop" ).setTypicalDuration( 2.*3600. ) );

		config.replanning().addStrategySettings( new StrategySettings( ).setStrategyName( FrozenTastes.LOCATION_CHOICE_PLAN_STRATEGY ).setWeight( 1.0 ).setDisableAfter( 10 ) );

		switch ( runType ){
			case shortRun:
				break;
			case medRun:
			case longRun:
				config.replanning().addStrategySettings( new StrategySettings().setStrategyName( FrozenTastes.LOCATION_CHOICE_PLAN_STRATEGY ).setWeight( 0.1 ) );
				config.replanning().addStrategySettings( new StrategySettings().setStrategyName( DefaultSelector.ChangeExpBeta ).setWeight( 1.0 ) );
				config.replanning().setFractionOfIterationsToDisableInnovation( 0.8 );
				config.scoring().setFractionOfIterationsToStartScoreMSA( 0.8 );
			break ;
			default:
				throw new RuntimeException( Gbl.NOT_IMPLEMENTED ) ;
		}

		final FrozenTastesConfigGroup dccg = ConfigUtils.addOrGetModule(config, FrozenTastesConfigGroup.class ) ;
		switch( runType ) {
			case shortRun:
				dccg.setEpsilonScaleFactors("10.0" );
				break;
			case longRun:
			case medRun:
				dccg.setEpsilonScaleFactors("10.0" );
				break;
			default:
				throw new RuntimeException( Gbl.NOT_IMPLEMENTED ) ;
		}
		dccg.setAlgorithm( bestResponse );
		dccg.setFlexibleTypes( "shop" );
		dccg.setTravelTimeApproximationLevel( ApproximationLevel.localRouting );
		dccg.setRandomSeed( 2 );
		dccg.setDestinationSamplePercent( 5. );
//		dccg.setInternalPlanDataStructure( DestinationChoiceConfigGroup.InternalPlanDataStructure.lcPlan );
		// using LCPlans does not, or no longer, work (throws a null pointer exception).  kai, mar'19

		// ---

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

		// CONTROL(L)ER:
		Controler controler = new Controler(scenario);
		controler.getConfig().controller().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		FrozenTastes.configure( controler );

		// bind locachoice strategy (selected in localCreateConfig):
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
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
						return (int) (val/10000.) ;
//						if ( val < 1. ) {
//							return 0 ;
//						}
//						return (int) ( Math.log(val)/Math.log(2) ) ;
					}
					@Override public void notifyShutdown( ShutdownEvent event ){
						switch( runType ) {
							case longRun:
							case medRun:
								return;
							case shortRun:
								break;
							default:
								throw new RuntimeException( Gbl.NOT_IMPLEMENTED ) ;
						}
						if ( event.isUnexpected() ) {
							return ;
						}
						double[] cnt = new double[1000] ;
						for( Person person : population.getPersons().values() ){
							List<Trip> trips = TripStructureUtils.getTrips( person.getSelectedPlan() );
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
						// Note that the following "check" method is deliberately a bit imprecise (see implementation), since we are only interested in the
						// (approximate) distribution.  kai, mar'19
						check( 1104, cnt[0] );
						check( 474, cnt[1] );
						check( 264, cnt[2] );
						check( 96, cnt[3] );
						check( 34, cnt[4] );
						check( 22, cnt[5] );
						check( 4, cnt[6] );
						check( 0, cnt[7] );
						check( 0, cnt[8] );
						check( 2, cnt[9] );
					}

					void check( double val, double actual ){
						Assertions.assertEquals( val, actual, 2.*Math.max( 5, Math.sqrt( val ) ) );
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

	static Config localCreateConfig( String configFileName ) {
		// setup config
		Config config = ConfigUtils.loadConfig(configFileName, new FrozenTastesConfigGroup() ) ;

		config.global().setNumberOfThreads(0);
		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(1);
		config.controller().setMobsim("qsim");
		config.qsim().setSnapshotStyle( QSimConfigGroup.SnapshotStyle.queue ) ;

		final FrozenTastesConfigGroup dccg = ConfigUtils.addOrGetModule(config, FrozenTastesConfigGroup.class ) ;
		dccg.setAlgorithm( Algotype.random );
		dccg.setFlexibleTypes("work" );

		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(12*60*60);
		config.scoring().addActivityParams(home);
		ActivityParams work = new ActivityParams("work");
		work.setTypicalDuration(12*60*60);
		config.scoring().addActivityParams(work);
		ActivityParams shop = new ActivityParams("shop");
		shop.setTypicalDuration(1.*60*60);
		config.scoring().addActivityParams(shop);

		final StrategySettings strategySettings = new StrategySettings(Id.create("1", StrategySettings.class ));
		strategySettings.setStrategyName("MyLocationChoice");
		strategySettings.setWeight(1.0);
		config.replanning().addStrategySettings(strategySettings);

		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class ).setEffectiveLaneWidth(1. ) ;
		config.qsim().setLinkWidthForVis((float)1.) ;
		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setShowTeleportedAgents(true) ;
		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setDrawNonMovingItems(true) ;

		return config;
	}


}
