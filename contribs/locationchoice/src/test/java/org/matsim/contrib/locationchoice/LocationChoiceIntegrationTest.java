/* *********************************************************************** *
 * project: org.matsim.*
 * LocationChoice.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.contrib.locationchoice;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup.Algotype;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.MaxDCScoreWrapper;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.ReadOrComputeMaxDCScore;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DCActivityWOFacilitiesScoringFunction;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DCScoringFunctionFactory;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;

import javax.inject.Provider;
import java.util.Random;

public class LocationChoiceIntegrationTest extends MatsimTestCase {

	public void testLocationChoiceJan2013() {
		//	CONFIG:
		final Config config = localCreateConfig();

		((DestinationChoiceConfigGroup)config.getModule("locationchoice")).setAlgorithm(Algotype.bestResponse);
		((DestinationChoiceConfigGroup)config.getModule("locationchoice")).setEpsilonScaleFactors("100.0");
		((DestinationChoiceConfigGroup)config.getModule("locationchoice")).setRandomSeed(4711);

		// SCENARIO:
		final Scenario scenario = ScenarioUtils.createScenario(config);

		final double scale = 1000. ;
		final double speed = 10. ;
		createExampleNetwork(scenario, scale, speed);

		Link ll1 = scenario.getNetwork().getLinks().get(Id.create(1, Link.class)) ;
		ActivityFacility ff1 = scenario.getActivityFacilities().getFacilities().get(Id.create(1, ActivityFacility.class));
		Person person = localCreatePopWOnePerson(scenario, ll1, ff1, 8.*60*60+5*60);

		// joint context (based on scenario):
		final DestinationChoiceBestResponseContext lcContext = new DestinationChoiceBestResponseContext(scenario) ;
		lcContext.init();

		// CONTROL(L)ER:
		Controler controler = new Controler(scenario);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		ReadOrComputeMaxDCScore computer = new ReadOrComputeMaxDCScore(lcContext);
        computer.readOrCreateMaxDCScore(controler.getConfig(), lcContext.kValsAreRead());
        final ObjectAttributes personsMaxDCScoreUnscaled = computer.getPersonsMaxEpsUnscaled();

		// set scoring function factory:
		controler.setScoringFunctionFactory( new ScoringFunctionFactory(){
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				SumScoringFunction sum = new SumScoringFunction() ;
				sum.addScoringFunction(new CharyparNagelActivityScoring(lcContext.getParams()));
				sum.addScoringFunction(new CharyparNagelLegScoring(lcContext.getParams(), scenario.getNetwork() ) ) ;
				sum.addScoringFunction( new CharyparNagelAgentStuckScoring(lcContext.getParams() ) );
				sum.addScoringFunction( new DCActivityWOFacilitiesScoringFunction(person, lcContext) ) ;
				return sum ;
			}
		}) ;

		MaxDCScoreWrapper dcScore = new MaxDCScoreWrapper();
		dcScore.setPersonsMaxDCScoreUnscaled(personsMaxDCScoreUnscaled);
		controler.getScenario().addScenarioElement(DestinationChoiceBestResponseContext.ELEMENT_NAME , lcContext);
		controler.getScenario().addScenarioElement(MaxDCScoreWrapper.ELEMENT_NAME , dcScore);

		// add locachoice strategy factory:
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("MyLocationChoice").to(BestReplyLocationChoicePlanStrategy.class);
			}
		});

		// this is here only to switch on otfvis if needed:
//		controler.setMobsimFactory(new FactoryForMobsimWithOTFVis() ) ;

		// run:
		controler.run();

		assertEquals("number of plans in person.", 2, person.getPlans().size());
		Plan newPlan = person.getSelectedPlan();
		System.err.println( " newPlan: " + newPlan ) ;
		Activity newWork = (Activity) newPlan.getPlanElements().get(2);
		if ( config.plansCalcRoute().isInsertingAccessEgressWalk() ) {
			newWork = (Activity) newPlan.getPlanElements().get(6);
		}
		System.err.println( " newWork: " + newWork ) ;
		System.err.println( " facilityId: " + newWork.getFacilityId() ) ;
		assertNotNull( newWork ) ;
		assertTrue( !newWork.getFacilityId().equals(Id.create(1, ActivityFacility.class) ) ) ; // should be different from facility number 1 !!
		assertEquals( Id.create(63, ActivityFacility.class), newWork.getFacilityId() ); // as I have changed the scoring (act is included) I also changed the test here: 27->92
	}

	public void testLocationChoiceFeb2013NegativeScores() {
		// config:
		final Config config = localCreateConfig();
		
		((DestinationChoiceConfigGroup)config.getModule("locationchoice")).setAlgorithm(Algotype.bestResponse);
		((DestinationChoiceConfigGroup)config.getModule("locationchoice")).setEpsilonScaleFactors("100.0");


		// scenario:
		final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		final double scale = 100000. ;
		final double speed = 1. ;

		createExampleNetwork(scenario, scale, speed);

		Link ll1 = scenario.getNetwork().getLinks().get(Id.create(1, Link.class)) ;
		ActivityFacility ff1 = scenario.getActivityFacilities().getFacilities().get(Id.create(1, ActivityFacility.class)) ;
		Person person = localCreatePopWOnePerson(scenario, ll1, ff1, 8.*60*60+5*60);

		final DestinationChoiceBestResponseContext lcContext = new DestinationChoiceBestResponseContext(scenario) ;
		lcContext.init();

		// CONTROL(L)ER:
		Controler controler = new Controler(scenario);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		ReadOrComputeMaxDCScore computer = new ReadOrComputeMaxDCScore(lcContext);
        computer.readOrCreateMaxDCScore(controler.getConfig(), lcContext.kValsAreRead());
        final ObjectAttributes personsMaxDCScoreUnscaled = computer.getPersonsMaxEpsUnscaled();

		// set scoring function
		DCScoringFunctionFactory scoringFunctionFactory = new DCScoringFunctionFactory(controler.getScenario(), lcContext);
		scoringFunctionFactory.setUsingConfigParamsForScoring(true) ;

		controler.setScoringFunctionFactory(scoringFunctionFactory);

		MaxDCScoreWrapper dcScore = new MaxDCScoreWrapper();
		dcScore.setPersonsMaxDCScoreUnscaled(personsMaxDCScoreUnscaled);
		controler.getScenario().addScenarioElement(DestinationChoiceBestResponseContext.ELEMENT_NAME, lcContext);
		controler.getScenario().addScenarioElement(MaxDCScoreWrapper.ELEMENT_NAME , dcScore);

		// set locachoice strategy:
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("MyLocationChoice").to(BestReplyLocationChoicePlanStrategy.class);
			}
		});

		// this is here only to switch on otfvis if needed:
//		controler.setMobsimFactory(new FactoryForMobsimWithOTFVis() ) ;

		controler.run();

		assertEquals("number of plans in person.", 2, person.getPlans().size());
		Plan newPlan = person.getSelectedPlan();
		System.err.println( " newPlan: " + newPlan ) ;
		ActivityImpl newWork = (ActivityImpl) newPlan.getPlanElements().get(2);
		System.err.println( " newWork: " + newWork ) ;
		System.err.println( " facilityId: " + newWork.getFacilityId() ) ;
		//		assertTrue( !newWork.getFacilityId().equals(Id.create(1) ) ) ; // should be different from facility number 1 !!
		//		assertEquals( Id.create(55), newWork.getFacilityId() );
		System.err.println("shouldn't this change anyways??") ;
	}

	private void createExampleNetwork(final Scenario scenario, final double scale, final double speed) {
		Network network = scenario.getNetwork() ;

		final double x = -scale;
		Node node0 = network.getFactory().createNode(Id.create(0, Node.class), new Coord(x, (double) 0)) ;
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
			facility.addActivityOption(new ActivityOptionImpl("work"));
		}

		// create one additional facility for the initial activity:
		ActivityFacility facility1 = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create(1, ActivityFacility.class), new Coord(scale, (double) 0));
		scenario.getActivityFacilities().addActivityFacility(facility1);
		facility1.addActivityOption(new ActivityOptionImpl("work"));
		// (as soon as you set a scoring function that looks if activity types match opportunities at facilities, you can only use
		// an activity type that indeed is at the facility)
	}
	public void testLocationChoice() {
		final Config config = localCreateConfig();

		final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		// setup network
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		Node node1 = network.createAndAddNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = network.createAndAddNode(Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		Link link = network.createAndAddLink(Id.create(1, Link.class), node1, node2, 1000, 10, 3600, 1);
		ActivityFacilityImpl facility1 = ((ActivityFacilitiesImpl) scenario.getActivityFacilities()).createAndAddFacility(Id.create(1, ActivityFacility.class), new Coord((double) 0, (double) 500));
		facility1.addActivityOption(new ActivityOptionImpl("initial-work"));
		ActivityFacilityImpl facility2 = ((ActivityFacilitiesImpl) scenario.getActivityFacilities()).createAndAddFacility(Id.create(2, ActivityFacility.class), new Coord((double) 0, (double) 400));
		facility2.addActivityOption(new ActivityOptionImpl("work"));
		ActivityFacilityImpl facility3 = ((ActivityFacilitiesImpl) scenario.getActivityFacilities()).createAndAddFacility(Id.create(3, ActivityFacility.class), new Coord((double) 0, (double) 300));
		facility3.addActivityOption(new ActivityOptionImpl("work"));

		Person person = localCreatePopWOnePerson(scenario, link, facility1, 17.*60.*60.);

		Controler controler = new Controler(scenario);

		// set locachoice strategy:
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
				addPlanStrategyBinding("MyLocationChoice").toProvider(new javax.inject.Provider<PlanStrategy>() {
					@Override
					public PlanStrategy get() {
						return new LocationChoicePlanStrategy(scenario, tripRouterProvider);
					}
				});
			}
		});
		// (this is now only necessary since the config for all three tests sets MyLocationChoice instead of LocationChoice. Probably
		// should pull the best response test away from the other (old) test.  kai, feb'13

		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();

		// test that everything worked as expected
		// The initial facility is *not* part of the choice set (its supported activity type is called "initial-work" for that reason)
		// so that the test can notice that there is a difference. In my earlier attempt, the random facility chosen would always be the one
		// on which I already am, so the test was no good.
		// Secondly, I need to give it two facilities to choose from, because a choice set of size 1 is treated specially
		// (it is assumed that the one element is the one I'm already on, so nothing is done).
		// I tricked it. :-)   michaz
		assertEquals("number of plans in person.", 2, person.getPlans().size());
		Plan newPlan = person.getSelectedPlan();
		Activity newWork = (Activity) newPlan.getPlanElements().get(2);
		if ( config.plansCalcRoute().isInsertingAccessEgressWalk() ) {
			newWork = (Activity) newPlan.getPlanElements().get(6);
		}
		assertNotNull( newWork ) ;
		assertNotNull( newWork.getFacilityId() ) ;
		assertTrue(newWork.getFacilityId().equals(Id.create(2, ActivityFacility.class)) || newWork.getFacilityId().equals(Id.create(3, ActivityFacility.class)));
	}

	/**
	 * setup population with one person
	 * @param workActEndTime TODO
	 */
	private static Person localCreatePopWOnePerson(Scenario scenario, Link link, ActivityFacility facility1, double workActEndTime) {

		Population population = scenario.getPopulation();

		Person person = population.getFactory().createPerson(Id.create(1, Person.class));
		population.addPerson(person);

		PlanImpl plan = (PlanImpl) population.getFactory().createPlan() ;
		person.addPlan(plan) ;

		{
			Activity act = population.getFactory().createActivityFromCoord("home", new Coord((double) 0, (double) 0)) ;
			//		act.setLinkId(link.getId());
			act.setEndTime(8.0 * 3600);
			plan.addActivity(act) ;
		}
		plan.addLeg(population.getFactory().createLeg(TransportMode.car)) ;
		{
			Activity act = population.getFactory().createActivityFromCoord("work", scenario.getActivityFacilities().getFacilities().get(facility1.getId()).getCoord() ) ;
			act.setEndTime(workActEndTime);
			((ActivityImpl)act).setFacilityId(facility1.getId());
			plan.addActivity(act) ;
		}
		plan.createAndAddLeg(TransportMode.car);
		{
			//		act = plan.createAndAddActivity("home", new CoordImpl(0, 0));
			//		act.setLinkId(link.getId());
			Activity act = population.getFactory().createActivityFromCoord("home", new Coord((double) 0, (double) 0)) ;
			plan.addActivity(act) ;
		}

		return person;
	}

	private Config localCreateConfig() {
		// setup config
		String args [] = {this.getPackageInputDirectory() + "config.xml"};
		Config config = ConfigUtils.loadConfig(args[0], new DestinationChoiceConfigGroup() ) ;

		config.global().setNumberOfThreads(0);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(1);
		config.controler().setMobsim("qsim");
		config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue) ;

		((DestinationChoiceConfigGroup)config.getModule("locationchoice")).setAlgorithm(Algotype.random);
		((DestinationChoiceConfigGroup)config.getModule("locationchoice")).setFlexibleTypes("work");

		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(12*60*60);
		config.planCalcScore().addActivityParams(home);
		ActivityParams work = new ActivityParams("work");
		work.setTypicalDuration(12*60*60);
		config.planCalcScore().addActivityParams(work);

		final StrategySettings strategySettings = new StrategySettings(Id.create("1", StrategySettings.class));
		strategySettings.setStrategyName("MyLocationChoice");
		strategySettings.setWeight(1.0);
		config.strategy().addStrategySettings(strategySettings);

		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setEffectiveLaneWidth(1.) ;
		config.qsim().setLinkWidthForVis((float)1.) ;
		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setShowTeleportedAgents(true) ;
		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setDrawNonMovingItems(true) ;

		return config;
	}

	class FactoryForMobsimWithOTFVis implements MobsimFactory {
		@Override
		public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
			QSim qSim = (QSim) QSimUtils.createDefaultQSim(sc, eventsManager);
			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, eventsManager, qSim);
			OTFClientLive.run(sc.getConfig(), server);
			return qSim ;
		}
	}

}
