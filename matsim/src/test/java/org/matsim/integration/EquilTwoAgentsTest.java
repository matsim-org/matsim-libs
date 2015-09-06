/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.integration;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;

/**
 * This test uses the org.matsim.examples equil scenario with two agents
 * to check the calculation of scores and traveltimes in the framework.
 * The scores and traveltimes calculated by MATSim are compared
 * with values analytically computed by hand.
 * 
 * Note: This used to be a "white box" test, where intermediate results were pulled
 * from the scoring function. However, this is an undefined state of the scoring
 * function -- the contract says you have to call finish on the scoring function
 * before it delivers a meaningful result. -- michaz 2012
 * 
 * @author dgrether
 */
public class EquilTwoAgentsTest extends MatsimTestCase {

	/*package*/ final static Logger log = Logger.getLogger(EquilTwoAgentsTest.class);

	private EventsToScore planScorer = null;

	/*package*/ final static Id<Person> personId1 = Id.create("1", Person.class);
	/*package*/ final static Id<Person> personId2 = Id.create("2", Person.class);
	/*package*/ final static Id<Link> id1 = Id.create("1", Link.class);
	/*package*/ final static Id<Link> id2 = Id.create("2", Link.class);
	/*package*/ final static Id<Link> id6 = Id.create("6", Link.class);
	/*package*/ final static Id<Link> id15 = Id.create("15", Link.class);
	/*package*/ final static Id<Link> id20 = Id.create("20", Link.class);
	/*package*/ final static Id<Link> id21 = Id.create("21", Link.class);
	/*package*/ final static Id<Link> id22 = Id.create("22", Link.class);
	/*package*/ final static Id<Link> id23 = Id.create("23", Link.class);

	private TestSingleIterationEventHandler handler;

	@Override
	protected void tearDown() throws Exception {
		this.planScorer = null;
		super.tearDown();
	}

	public void testSingleIterationPlansV4() {
		final Config config = this.loadConfig(this.getClassInputDirectory() + "config.xml");
		String netFileName = "test/scenarios/equil/network.xml";
		config.network().setInputFile(netFileName);
		config.plans().setInputFile(this.getClassInputDirectory() + "plans2.xml");

		PlanCalcScoreConfigGroup pcsConfig = config.planCalcScore() ;
		ActivityParams params = new ActivityParams("h") ;
        params.setTypicalDuration(123456789.0) ; // probably dummy
//		params.setOpeningTime(0.) ;
//		params.setClosingTime(0.) ; // cannot access "setScoreAtAll" at this level.
		params.setScoringThisActivityAtAll(false);
		pcsConfig.addActivityParams(params) ;
		
		final Controler controler = new Controler(config);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.getConfig().controler().setCreateGraphs(false);
        controler.getConfig().controler().setWriteEventsInterval(0);
		controler.addControlerListener(new StartupListener() {
			@Override
			public void notifyStartup(final StartupEvent event) {
                double agent1LeaveHomeTime = ((PlanImpl) controler.getScenario().getPopulation().getPersons().get(personId1).getPlans().get(0)).getFirstActivity().getEndTime();
                double agent2LeaveHomeTime = ((PlanImpl) controler.getScenario().getPopulation().getPersons().get(personId2).getPlans().get(0)).getFirstActivity().getEndTime();
				handler = new TestSingleIterationEventHandler(agent1LeaveHomeTime, agent2LeaveHomeTime);
				controler.getEvents().addHandler(handler);
				
				
				// Construct a scoring function which does not score the home activity. Because the analytical calculations against which 
				// we are testing here are based on that.
//				CharyparNagelScoringParameters params = new CharyparNagelScoringParameters(config.planCalcScore());
//				ActivityUtilityParameters activityUtilityParameters = new ActivityUtilityParameters("h", 1.0, 123456789.0);
//				activityUtilityParameters.setScoreAtAll(false);

//				ActivityUtilityParameters.Factory factory = new ActivityUtilityParameters.Factory() ;
//				factory.setScoreAtAll(false) ;
//				factory.setType("h") ;
//				factory.setTypicalDuration_s(123456789.0) ;
//				ActivityUtilityParameters activityUtilityParameters = factory.create() ;
//
//				params.utilParams.put("h", activityUtilityParameters);
				EquilTwoAgentsTest.this.planScorer = new EventsToScore(controler.getScenario(), 
//						new CharyparNagelScoringFunctionFactory(params, controler.getScenario().getNetwork()));
				new CharyparNagelScoringFunctionFactory(config.planCalcScore(), config.scenario(), controler.getScenario().getNetwork()));
				
				controler.getEvents().addHandler(EquilTwoAgentsTest.this.planScorer);
			}
		});

		controler.run();
		this.planScorer.finish();

		assertEquals(handler.agentOneScore, EquilTwoAgentsTest.this.planScorer.getAgentScore(personId1), EPSILON);
		assertEquals(handler.agentTwoScore, EquilTwoAgentsTest.this.planScorer.getAgentScore(personId2), EPSILON);
	}


	private static class TestSingleIterationEventHandler implements LinkEnterEventHandler, ActivityStartEventHandler, ActivityEndEventHandler {

		private final double agent1LeaveHomeTime, agent2LeaveHomeTime;

		private double agentOneTime, agentTwoTime;

		private double agentOneScore, agentTwoScore;

		/*package*/ TestSingleIterationEventHandler(final double agent1LeaveHomeTime, final double agent2LeaveHomeTime) {
			this.agent1LeaveHomeTime = agent1LeaveHomeTime;
			this.agent2LeaveHomeTime = agent2LeaveHomeTime;
		}

		@Override
		public void handleEvent(final LinkEnterEvent e) {
			log.debug("Enter Link:" + e.getLinkId().toString() + " at Time: " + e.getTime());
			if (e.getLinkId().equals(id6)) {
				this.agentOneTime = this.agent1LeaveHomeTime + 1.0;
				assertEquals(this.agentOneTime, e.getTime(), EPSILON);
			}
			else if (e.getLinkId().equals(id15)) {
				this.agentOneTime = this.agentOneTime + 359.0 + 1.0;
				assertEquals(this.agentOneTime, e.getTime(), EPSILON);
			}
			else if (e.getLinkId().equals(id20)) {
				this.agentOneTime = this.agentOneTime + 179.0 + 1.0;
				assertEquals(this.agentOneTime, e.getTime(), EPSILON);
			}
			else if (e.getLinkId().equals(id21)) {
				this.agentOneTime = this.agentOneTime + 1.0;
				assertEquals(this.agentOneTime, e.getTime(), EPSILON);
			}
			else if (e.getLinkId().equals(id22)) {
				this.agentOneTime = this.agentOneTime + 359.0 + 1.0;
				assertEquals(this.agentOneTime, e.getTime(), EPSILON);
			}
			else if (e.getLinkId().equals(id23)) {
				this.agentOneTime = this.agentOneTime + 1259.0 + 1.0;
				assertEquals(this.agentOneTime, e.getTime(), EPSILON);
			}
			else if (e.getLinkId().equals(id1)) {
				this.agentOneTime = this.agentOneTime + 359.0 + 1.0;
				assertEquals(this.agentOneTime, e.getTime(), EPSILON);
			}
		}

		@Override
		public void handleEvent(final ActivityStartEvent e) {
			log.debug("Start Activity " + e.getActType() + " : Time: " + Time.writeTime(e.getTime()) + " Agent: " + e.getPersonId().toString());
			if (e.getPersonId().equals(personId1)) {
				if (e.getActType().equalsIgnoreCase("w")) {
					//test the time
					//time is time till link 15 + freespeed of link 20
					this.agentOneTime = this.agentOneTime + 359.0;
					log.debug("Car tt to work is: " + (this.agentOneTime - this.agent1LeaveHomeTime));
					assertEquals(this.agentOneTime, e.getTime(), EPSILON);

					//0.25 h = 15 min = 900 s fstt of agent 1 (car)
					this.agentOneScore = 0.25 * -6.0;
				}
				else { // it is home
					//test the time
					this.agentOneTime = this.agentOneTime + 359.0;
					assertEquals(this.agentOneTime, e.getTime(), EPSILON);

					//must be negative score for traveling to work (i.e. value of agentOneScore)
					//plus activity score 8 h work typical = 8h, beta_perf = 6
					double deltaScore = (6.0*8.0*Math.log(8.0 / (8.0*Math.exp(-10.0/8.0))));
					log.debug("Scorecalc: " + deltaScore);
					this.agentOneScore = this.agentOneScore + deltaScore;
					//plus negative score for traveling home 39 minutes
					this.agentOneScore = this.agentOneScore + (39.0/60.0 * -6.0);
				}
			}
			else if (e.getPersonId().equals(personId2)) {
				if (e.getActType().equalsIgnoreCase("w")) {
					//test the time
					//this is the version used in speech:	agentTwoTime = agentTwoTime + 30.0 * 60.0;
					//this is the version used in code:
					this.agentTwoTime = this.agentTwoTime + 1797.0; // 30.0 * 60.0 - 3.0
					assertEquals(this.agentTwoTime, e.getTime(), EPSILON);

					//1097s free speed travel time of agent 2 (non-car)
					this.agentTwoScore = 1797.0/3600.0 * -3.0;
				}
				else {
					//test the time
					//				this is the version used in speech:	agentTwoTime = agentTwoTime + 78.0 * 60.0;
					//this is the version used in code:
					this.agentTwoTime = this.agentTwoTime + 4677.0; // 78.0 * 60.0 - 3.0
					assertEquals(this.agentTwoTime, e.getTime(), EPSILON);

					//must be negative score for traveling to work (i.e. value of agentTwoScore)
					//plus activity score 8 h work typical = 8h, beta_perf = 6
					this.agentTwoScore = this.agentTwoScore + (6.0*8.0*Math.log(8.0 / (8.0*Math.exp(-10.0/8.0))));
					// plus negative score for being late at work in the morning
					this.agentTwoScore = this.agentTwoScore + (this.agent2LeaveHomeTime + 1797.0 - 7.0*3600) * (-18.0 / 3600.0);
					//plus negative score for traveling home 4677.0 seconds by non-car mode (should be 78 min but is less!)
					this.agentTwoScore = this.agentTwoScore + (4677.0/3600.0 * -3.0);

				}
			}
		}

		@Override
		public void handleEvent(final ActivityEndEvent e) {
			log.debug("End Activity " + e.getActType() + " : Time: " + Time.writeTime(e.getTime()) + " Agent: " + e.getPersonId().toString());

			if (e.getPersonId().equals(personId1)) {
				if (e.getActType().equalsIgnoreCase("w")) {
					this.agentOneTime = this.agentOneTime + 8.0 * 3600;
					assertEquals(this.agentOneTime, e.getTime(), EPSILON);
				}
				else {
					assertEquals(this.agent1LeaveHomeTime, e.getTime(), EPSILON);
				}
			}
			else if (e.getPersonId().equals(personId2)) {
				if (e.getActType().equalsIgnoreCase("w")) {
					this.agentTwoTime = this.agentTwoTime + 8.0 * 3600;
					assertEquals(this.agentTwoTime, e.getTime(), EPSILON);
				}
				else {
					this.agentTwoTime = this.agent2LeaveHomeTime;
					assertEquals(this.agentTwoTime, e.getTime(), EPSILON);
				}
			}
		}

		@Override
		public void reset(final int iteration) {
		}
	}

}
