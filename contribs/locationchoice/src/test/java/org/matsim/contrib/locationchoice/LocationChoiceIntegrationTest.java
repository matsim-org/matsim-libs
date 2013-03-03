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

import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.MaxDCScoreWrapper;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.ReadOrComputeMaxDCScore;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DCActivityWOFacilitiesScoringFunction;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DCScoringFunctionFactory;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalties;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.LocationChoiceConfigGroup.Algotype;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

public class LocationChoiceIntegrationTest extends MatsimTestCase {
	
	public void testLocationChoiceJan2013() {
		//	CONFIG:
		final Config config = localCreateConfig();

		config.locationchoice().setAlgorithm(Algotype.bestResponse) ;
		config.locationchoice().setEpsilonScaleFactors("100.0") ;
		config.locationchoice().setRandomSeed("4711") ;

		// SCENARIO:
		final ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);

		final double scale = 1000. ;
		final double speed = 10. ;
		createExampleNetwork(scenario, scale, speed);

		Link ll1 = scenario.getNetwork().getLinks().get(new IdImpl(1)) ;
		ActivityFacility ff1 = scenario.getActivityFacilities().getFacilities().get(new IdImpl(1)) ;
		Person person = localCreatePopWOnePerson(scenario, ll1, ff1, 8.*60*60+5*60);

		// joint context (based on scenario):
		final DestinationChoiceBestResponseContext lcContext = new DestinationChoiceBestResponseContext(scenario) ;
		lcContext.init();
				
		// CONTROL(L)ER:
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true) ;
		
		ReadOrComputeMaxDCScore computer = new ReadOrComputeMaxDCScore(lcContext);
  		computer.readOrCreateMaxDCScore(controler, lcContext.kValsAreRead());
  		final ObjectAttributes personsMaxDCScoreUnscaled = computer.getPersonsMaxEpsUnscaled();

		// set scoring function factory:
		controler.setScoringFunctionFactory( new ScoringFunctionFactory(){
			@Override
			public ScoringFunction createNewScoringFunction(Plan plan) {		
				ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
				scoringFunctionAccumulator.addScoringFunction(new DCActivityWOFacilitiesScoringFunction(plan, lcContext ) );
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(lcContext.getParams(), scenario.getNetwork()));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(lcContext.getParams()));
				return scoringFunctionAccumulator;
			}
		}) ;
		
		MaxDCScoreWrapper dcScore = new MaxDCScoreWrapper();
		dcScore.setPersonsMaxDCScoreUnscaled(personsMaxDCScoreUnscaled);
		controler.getScenario().addScenarioElement(lcContext);
		controler.getScenario().addScenarioElement(dcScore);

		// add locachoice strategy factory:
		controler.addPlanStrategyFactory("MyLocationChoice", new PlanStrategyFactory(){
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario2, EventsManager eventsManager) {
				return new BestReplyLocationChoicePlanStrategy(scenario) ;
				// yy MZ argues that this is not so great since the factory now has context that goes beyond Scenario and EventsManager.
				// As an alternative, one could add the context to Scenario as scenario element.  I find the present version easier to read, and
				// as long as the factories remain anonymous classes, this is most probably ok. kai, feb'13
			}
		});
		
		// this is here only to switch on otfvis if needed:
//		controler.setMobsimFactory(new FactoryForMobsimWithOTFVis() ) ;

		// run:
		controler.run();

		assertEquals("number of plans in person.", 2, person.getPlans().size());
		Plan newPlan = person.getSelectedPlan();
		System.err.println( " newPlan: " + newPlan ) ;
		ActivityImpl newWork = (ActivityImpl) newPlan.getPlanElements().get(2);
		System.err.println( " newWork: " + newWork ) ;
		System.err.println( " facilityId: " + newWork.getFacilityId() ) ;
		assertTrue( !newWork.getFacilityId().equals(new IdImpl(1) ) ) ; // should be different from facility number 1 !!
		assertEquals( new IdImpl(92), newWork.getFacilityId() ); // as I have changed the scoring (act is included) I also changed the test here: 27->92
	}

	public void testLocationChoiceFeb2013NegativeScores() {
		// config:
		final Config config = localCreateConfig();

		config.locationchoice().setAlgorithm(Algotype.bestResponse) ;
		config.locationchoice().setEpsilonScaleFactors("100.0") ;

		// scenario:
		final ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);

		final double scale = 100000. ;
		final double speed = 1. ;

		createExampleNetwork(scenario, scale, speed);

		Link ll1 = scenario.getNetwork().getLinks().get(new IdImpl(1)) ;
		ActivityFacility ff1 = scenario.getActivityFacilities().getFacilities().get(new IdImpl(1)) ;
		Person person = localCreatePopWOnePerson(scenario, ll1, ff1, 8.*60*60+5*60);

		FacilityPenalties facPenalties = scenario.getScenarioElement(FacilityPenalties.class);
		if (facPenalties == null) {
			facPenalties = new FacilityPenalties();
			scenario.addScenarioElement( facPenalties );
		}

		final DestinationChoiceBestResponseContext lcContext = new DestinationChoiceBestResponseContext(scenario) ;
		lcContext.init();
		
		// CONTROL(L)ER:
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true) ;
		
		ReadOrComputeMaxDCScore computer = new ReadOrComputeMaxDCScore(lcContext);
  		computer.readOrCreateMaxDCScore(controler, lcContext.kValsAreRead());
  		final ObjectAttributes personsMaxDCScoreUnscaled = computer.getPersonsMaxEpsUnscaled();

		// set scoring function
		DCScoringFunctionFactory scoringFunctionFactory = new DCScoringFunctionFactory(config, controler, lcContext);
		scoringFunctionFactory.setUsingFacilityOpeningTimes(false) ;

		controler.setScoringFunctionFactory(scoringFunctionFactory);
		
		MaxDCScoreWrapper dcScore = new MaxDCScoreWrapper();
		dcScore.setPersonsMaxDCScoreUnscaled(personsMaxDCScoreUnscaled);
		controler.getScenario().addScenarioElement(lcContext);
		controler.getScenario().addScenarioElement(dcScore);

		// set locachoice strategy:
		controler.addPlanStrategyFactory("MyLocationChoice", new PlanStrategyFactory(){
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario2, EventsManager eventsManager) {
				return new BestReplyLocationChoicePlanStrategy(scenario) ;
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
		//		assertTrue( !newWork.getFacilityId().equals(new IdImpl(1) ) ) ; // should be different from facility number 1 !!
		//		assertEquals( new IdImpl(55), newWork.getFacilityId() );
		System.err.println("shouldn't this change anyways??") ;
	}

	private void createExampleNetwork(final ScenarioImpl scenario, final double scale, final double speed) {
		Network network = scenario.getNetwork() ;

		Node node0 = network.getFactory().createNode(new IdImpl(0), new CoordImpl(-scale,0) ) ;
		network.addNode(node0) ;

		Node node1 = network.getFactory().createNode(new IdImpl(1), new CoordImpl(10,0) ) ;
		network.addNode(node1) ;

		Link link1 = network.getFactory().createLink(new IdImpl(1), node0, node1 );
		network.addLink(link1) ;
		Link link1b = network.getFactory().createLink(new IdImpl("1b"), node1, node0 ) ;
		network.addLink(link1b) ;

		final int nNodes = 100 ;
		Random random = new Random(4711) ;
		for ( int ii=2 ; ii<nNodes+2 ; ii++ ) {
			double tmp = Math.PI*(ii-1)/nNodes ;
			Coord coord = new CoordImpl( scale*Math.sin(tmp),scale*Math.cos(tmp) ) ;

			Node node = network.getFactory().createNode(new IdImpl(ii), coord ) ;
			network.addNode(node) ;

			double rnd = random.nextDouble() ;
			{
				Link link = network.getFactory().createLink(new IdImpl(ii), node1, node) ;
				link.setLength(rnd*scale) ;
				link.setFreespeed(speed) ;
				link.setCapacity(1.) ;
				network.addLink(link) ;
			}
			{
				Link link = network.getFactory().createLink(new IdImpl(ii+"b"), node, node1) ;
				link.setLength(rnd*scale) ;
				link.setFreespeed(speed) ;
				link.setCapacity(1.) ;
				network.addLink(link) ;
			}

			ActivityFacility facility = scenario.getActivityFacilities().createAndAddFacility(new IdImpl(ii), coord ) ;
			facility.getActivityOptions().put("work", new ActivityOptionImpl("work", facility)) ;
		}

		// create one additional facility for the initial activity:
		ActivityFacilityImpl facility1 = scenario.getActivityFacilities().createAndAddFacility(new IdImpl(1), new CoordImpl(scale,0) );
		facility1.getActivityOptions().put("work", new ActivityOptionImpl("work", facility1)) ;
		// (as soon as you set a scoring function that looks if activity types match opportunities at facilities, you can only use
		// an activity type that indeed is at the facility)
	}
	public void testLocationChoice() {
		final Config config = localCreateConfig();

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);

		// setup network
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		Node node1 = network.createAndAddNode(new IdImpl(1), new CoordImpl(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl(2), new CoordImpl(1000, 0));
		Link link = network.createAndAddLink(new IdImpl(1), node1, node2, 1000, 10, 3600, 1);
		ActivityFacilityImpl facility1 = scenario.getActivityFacilities().createAndAddFacility(new IdImpl(1), new CoordImpl(0, 500));
		facility1.getActivityOptions().put("initial-work", new ActivityOptionImpl("initial-work", facility1));
		ActivityFacilityImpl facility2 = scenario.getActivityFacilities().createAndAddFacility(new IdImpl(2), new CoordImpl(0, 400));
		facility2.getActivityOptions().put("work", new ActivityOptionImpl("work", facility2));
		ActivityFacilityImpl facility3 = scenario.getActivityFacilities().createAndAddFacility(new IdImpl(3), new CoordImpl(0, 300));
		facility3.getActivityOptions().put("work", new ActivityOptionImpl("work", facility3));

		Person person = localCreatePopWOnePerson(scenario, link, facility1, 17.*60.*60.);

		Controler controler = new Controler(scenario);

		// set locachoice strategy:
		controler.addPlanStrategyFactory("MyLocationChoice", new PlanStrategyFactory(){
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario2, EventsManager eventsManager) {
				return new LocationChoicePlanStrategy(scenario2) ;
			}
		});
		// (this is now only necessary since the config for all three tests sets MyLocationChoice instead of LocationChoice. Probably
		// should pull the best response test away from the other (old) test.  kai, feb'13
		
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
		ActivityImpl newWork = (ActivityImpl) newPlan.getPlanElements().get(2);
		assertTrue(newWork.getFacilityId().equals(new IdImpl(2)) || newWork.getFacilityId().equals(new IdImpl(3)));
	}

	/** 
	 * setup population with one person
	 * @param workActEndTime TODO
	 */
	private static Person localCreatePopWOnePerson(ScenarioImpl scenario, Link link, ActivityFacility facility1, double workActEndTime) {

		Population population = scenario.getPopulation();

		Person person = population.getFactory().createPerson(new IdImpl(1));
		population.addPerson(person);

		PlanImpl plan = (PlanImpl) population.getFactory().createPlan() ;
		person.addPlan(plan) ;

		{
			Activity act = population.getFactory().createActivityFromCoord("home", new CoordImpl(0,0)) ;
			//		act.setLinkId(link.getId());
			act.setEndTime(8.0 * 3600);
			plan.addActivity(act) ;
		}
		plan.addLeg(population.getFactory().createLeg(TransportMode.car)) ;
		{
			Activity act = population.getFactory().createActivityFromCoord("work", scenario.getActivityFacilities().getLocation(facility1.getId()).getCoord() ) ;
			act.setEndTime(workActEndTime);
			((ActivityImpl)act).setFacilityId(facility1.getId());
			plan.addActivity(act) ;
		}
		plan.createAndAddLeg(TransportMode.car);
		{
			//		act = plan.createAndAddActivity("home", new CoordImpl(0, 0));
			//		act.setLinkId(link.getId());
			Activity act = population.getFactory().createActivityFromCoord("home", new CoordImpl(0,0)) ;
			plan.addActivity(act) ;
		}

		return person;
	}

	private Config localCreateConfig() {
		// setup config
		final Config config = loadConfig(null);

		config.global().setNumberOfThreads(0);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(1);
		config.controler().setMobsim("qsim");

		config.addQSimConfigGroup(new QSimConfigGroup()) ;
		config.getQSimConfigGroup().setSnapshotStyle(QSimConfigGroup.SNAPSHOT_AS_QUEUE) ;

		config.locationchoice().setAlgorithm(Algotype.random);
		config.locationchoice().setFlexibleTypes("work");

		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(12*60*60);
		config.planCalcScore().addActivityParams(home);
		ActivityParams work = new ActivityParams("work");
		work.setTypicalDuration(12*60*60);
		config.planCalcScore().addActivityParams(work);

		final StrategySettings strategySettings = new StrategySettings(new IdImpl("1"));
		strategySettings.setModuleName("MyLocationChoice");
		strategySettings.setProbability(1.0);
		config.strategy().addStrategySettings(strategySettings);
		
		config.otfVis().setEffectiveLaneWidth(1.) ;
		config.otfVis().setLinkWidth((float)1.) ;
		config.otfVis().setShowTeleportedAgents(true) ;
		config.otfVis().setDrawNonMovingItems(true) ;

		return config;
	}
	
	class FactoryForMobsimWithOTFVis implements MobsimFactory {
		@Override
		public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
			QSim qSim = (QSim) new QSimFactory().createMobsim(sc, eventsManager) ;
			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, eventsManager, qSim);
			OTFClientLive.run(sc.getConfig(), server);
			return qSim ;
		} 
	} 

}
