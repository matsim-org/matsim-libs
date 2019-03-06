package org.matsim.contrib.locationchoice;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
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
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup.Algotype;
import org.matsim.contrib.locationchoice.bestresponse.BestReplyLocationChoicePlanStrategy;
import org.matsim.contrib.locationchoice.bestresponse.DCActivityWOFacilitiesScoringFunction;
import org.matsim.contrib.locationchoice.bestresponse.DCScoringFunctionFactory;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceContext;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;
import static org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup.Algotype.*;
import static org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup.ApproximationLevel.completeRouting;
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

		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		DestinationChoiceConfigGroup dccg = ConfigUtils.addOrGetModule( config, DestinationChoiceConfigGroup.class );;

		dccg.setAlgorithm( bestResponse );
		// yy I don't think that this is honoured anywhere in the way this is plugged together here.  kai, mar'19

		dccg.setEpsilonScaleFactors("100.0");
		dccg.setRandomSeed(4711);

		// SCENARIO:
		final Scenario scenario = ScenarioUtils.createScenario(config );

		final double scale = 1000. ;
		final double speed = 10. ;
		createExampleNetwork(scenario, scale, speed);

		Link ll1 = scenario.getNetwork().getLinks().get( Id.create(1, Link.class ) ) ;
		ActivityFacility ff1 = scenario.getActivityFacilities().getFacilities().get(Id.create(1, ActivityFacility.class ) );
		Person person = localCreatePopWOnePerson(scenario, ll1, ff1, 8.*60*60+5*60 );

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
		assertEquals( Id.create(63, ActivityFacility.class), newWork.getFacilityId() ); // as I have changed the scoring (act is included) I also changed the test here: 27->92
	}

	@Test
	public void testLocationChoiceFeb2013NegativeScores() {
		// config:
		final Config config = localCreateConfig( utils.getPackageInputDirectory() + "config.xml");

		final DestinationChoiceConfigGroup dccg = ConfigUtils.addOrGetModule(config, DestinationChoiceConfigGroup.class ) ;

		dccg.setAlgorithm( bestResponse );
		// yy Don't think has an influence with setup here. kai, mar'19
		// well, it does not work without this ...

		dccg.setEpsilonScaleFactors("100.0" );

		// scenario:
		final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config );

		final double scale = 100000. ;
		final double speed = 1. ;

		createExampleNetwork(scenario, scale, speed);

		Link ll1 = scenario.getNetwork().getLinks().get(Id.create(1, Link.class)) ;
		ActivityFacility ff1 = scenario.getActivityFacilities().getFacilities().get(Id.create(1, ActivityFacility.class)) ;
		Person person = localCreatePopWOnePerson(scenario, ll1, ff1, 8.*60*60+5*60);

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
//		Config config = ConfigUtils.createConfig() ;
		final Config config = localCreateConfig( utils.getPackageInputDirectory() + "config.xml");

		final DestinationChoiceConfigGroup dccg = ConfigUtils.addOrGetModule(config, DestinationChoiceConfigGroup.class ) ;
		dccg.setEpsilonScaleFactors("100.0" );
		dccg.setAlgorithm( bestResponse );
		dccg.setFlexibleTypes( "shop" );
		dccg.setTravelTimeApproximationLevel( completeRouting );
		dccg.setRandomSeed( 1 );
		// yy do we need more settings here?

		// ---

		//		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		// (this will not load anything since there are no files defined)

		Scenario scenario = ScenarioUtils.createScenario( config ) ;
		// don't load anything

		NetworkFactory nf = scenario.getNetwork().getFactory();
		PopulationFactory pf = scenario.getPopulation().getFactory();
		ActivityFacilitiesFactory ff = scenario.getActivityFacilities().getFactory();

		Node prevNode;
		{
			Node node = nf.createNode( Id.createNodeId( 0 ) , new Coord( 0., 0. ) ) ;
			scenario.getNetwork().addNode( node );
			prevNode = node ;
		}
		for ( int ii=1 ; ii<100 ; ii++ ) {
			Node node = nf.createNode( Id.createNodeId( ii ) , new Coord( ii*100, 0.) ) ;
			scenario.getNetwork().addNode( node );
			// ---
			addLinkAndFacility( scenario, nf, ff, prevNode, node );
			addLinkAndFacility( scenario, nf, ff, node, prevNode );
			// ---
			prevNode = node ;
		}
		Person person = pf.createPerson( Id.createPersonId( "abc" ) ) ;
		{
			scenario.getPopulation().addPerson( person );
			Plan plan = pf.createPlan() ;
			person.addPlan( plan ) ;
			// ---
			Activity home = pf.createActivityFromCoord( "home", new Coord( 50., 0. ) );
			home.setEndTime( 7. * 3600. );
			plan.addActivity( home );
			{
				Leg leg = pf.createLeg( "car" );
				leg.setDepartureTime( 7.*3600. );
				leg.setTravelTime( 1800. );
				plan.addLeg( leg );
			}
			{
				Activity shop = pf.createActivityFromCoord( "shop", new Coord( 250., 0. ) );
				// shop.setMaximumDuration( 3600. ); // does not work for locachoice: time computation is not able to deal with it.  yyyy replace by
				// more central code. kai, mar'19
				shop.setEndTime( 8.*3600 );
				plan.addActivity( shop );
			}
			{
				Leg leg = pf.createLeg( "car" );
				leg.setDepartureTime( 8.*3600. );
				leg.setTravelTime( 1800. );
				plan.addLeg( leg );
			}
			{
				Activity home2 = pf.createActivityFromCoord( "home", new Coord( 50., 0. ) );
				PopulationUtils.copyFromTo( home, home2 );
				plan.addActivity( home2 );
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
			}
		});

		controler.run();

		assertEquals("number of plans in person.", 2, person.getPlans().size());
		Plan newPlan = person.getSelectedPlan();
		Level lvl = Level.INFO ;
		log.log( lvl, "" );
		log.log( lvl, " newPlan: " + newPlan ) ;
		for( PlanElement planElement : newPlan.getPlanElements() ){
			log.log( lvl, planElement.toString() ) ;
		}
		log.log(lvl,"") ;
		Activity newWork = (Activity) newPlan.getPlanElements().get(2 );
		if ( config.plansCalcRoute().isInsertingAccessEgressWalk() ) {
			newWork = (Activity) newPlan.getPlanElements().get(6);
		}
		System.err.println( " newWork: " + newWork ) ;
		System.err.println( " facilityId: " + newWork.getFacilityId() ) ;
		assertNotNull( newWork ) ;
		assertTrue( !newWork.getFacilityId().equals(Id.create(1, ActivityFacility.class) ) ) ; // should be different from facility number 1 !!
		assertEquals( Id.create(63, ActivityFacility.class), newWork.getFacilityId() ); // as I have changed the scoring (act is included) I also changed the test here: 27->92

		// This test is technically failing, but it seems to be doing what it should: It selects a facility with a large positive epsilon.  Changing the dccg random seed leads
		// to different frozen epsilons, and thus to a different selected facility.  I have not yet tested if this is stable under repeated calls.  kai, mar'19

	}

	public static void addLinkAndFacility( Scenario scenario, NetworkFactory nf, ActivityFacilitiesFactory ff, Node prevNode, Node node ){
		final String str = prevNode.getId() + "-" + node.getId();
		Link link = nf.createLink( Id.createLinkId( str ), prevNode, node ) ;
		Set<String> set = new HashSet<>() ;
		set.add("car" ) ;
		link.setAllowedModes( set ) ;
		link.setLength( 100. );
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
