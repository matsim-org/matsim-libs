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
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.StartupListener;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.events.handler.ActStartEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.scoring.EventsToScore;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.misc.Time;

/**
 * This test uses the org.matsim.examples equil scenario with two agents
 * to check the calculation of scores and traveltimes in the framework.
 * The scores and traveltimes calculated by MATSim are compared
 * with values analytically computed by hand.
 * @author dgrether
 */
public class EquilTwoAgentsTest extends MatsimTestCase {

	/*package*/ final static Logger log = Logger.getLogger(EquilTwoAgentsTest.class);

	/*package*/ EventsToScore planScorer = null;

	/*package*/ final static Id id1 = new IdImpl("1");
	/*package*/ final static Id id2 = new IdImpl("2");
	/*package*/ final static Id id6 = new IdImpl("6");
	/*package*/ final static Id id15 = new IdImpl("15");
	/*package*/ final static Id id20 = new IdImpl("20");
	/*package*/ final static Id id21 = new IdImpl("21");
	/*package*/ final static Id id22 = new IdImpl("22");
	/*package*/ final static Id id23 = new IdImpl("23");

	public void testSingleIteration() {
		Config config = this.loadConfig(this.getClassInputDirectory() + "config.xml");
		String netFileName = "test/scenarios/equil/network.xml";
		config.network().setInputFile(netFileName);
		config.plans().setInputFile(this.getClassInputDirectory() + "plans2.xml");

		final Controler controler = new Controler(config);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);

		controler.addControlerListener(new StartupListener() {

			public void notifyStartup(final StartupEvent event) {
				double agent1LeaveHomeTime = controler.getPopulation().getPerson(id1).getPlans().get(0).getFirstActivity().getEndTime();
				double agent2LeaveHomeTime = controler.getPopulation().getPerson(id2).getPlans().get(0).getFirstActivity().getEndTime();
				controler.getEvents().addHandler(new TestSingleIterationEventHandler(agent1LeaveHomeTime, agent2LeaveHomeTime));

				EquilTwoAgentsTest.this.planScorer = new EventsToScore(controler.getPopulation(), controler.getScoringFunctionFactory());
				controler.getEvents().addHandler(EquilTwoAgentsTest.this.planScorer);
			}
		});

		controler.run();
	}

	private class TestSingleIterationEventHandler implements LinkEnterEventHandler, ActStartEventHandler, ActEndEventHandler {

		private final double agent1LeaveHomeTime, agent2LeaveHomeTime;

		private double agentOneTime, agentTwoTime;

		private double agentOneScore, agentTwoScore;

		/*package*/ TestSingleIterationEventHandler(final double agent1LeaveHomeTime, final double agent2LeaveHomeTime) {
			this.agent1LeaveHomeTime = agent1LeaveHomeTime;
			this.agent2LeaveHomeTime = agent2LeaveHomeTime;
		}

		public void handleEvent(final LinkEnterEvent e) {
			log.debug("Enter Link:" + e.linkId + " at Time: " + e.time);
			if (e.link.getId().equals(id6)) {
				this.agentOneTime = this.agent1LeaveHomeTime + 1.0;
				assertEquals(this.agentOneTime, e.time, EPSILON);
			}
			else if (e.link.getId().equals(id15)) {
				this.agentOneTime = this.agentOneTime + 359.0 + 1.0;
				assertEquals(this.agentOneTime, e.time, EPSILON);
			}
			else if (e.link.getId().equals(id20)) {
				this.agentOneTime = this.agentOneTime + 179.0 + 1.0;
				assertEquals(this.agentOneTime, e.time, EPSILON);
			}
			else if (e.link.getId().equals(id21)) {
				this.agentOneTime = this.agentOneTime + 1.0;
				assertEquals(this.agentOneTime, e.time, EPSILON);
			}
			else if (e.link.getId().equals(id22)) {
				this.agentOneTime = this.agentOneTime + 359.0 + 1.0;
				assertEquals(this.agentOneTime, e.time, EPSILON);
			}
			else if (e.link.getId().equals(id23)) {
				this.agentOneTime = this.agentOneTime + 1259.0 + 1.0;
				assertEquals(this.agentOneTime, e.time, EPSILON);
			}
			else if (e.link.getId().equals(id1)) {
				this.agentOneTime = this.agentOneTime + 359.0 + 1.0;
				assertEquals(this.agentOneTime, e.time, EPSILON);
			}
		}

		public void handleEvent(final ActStartEvent e) {
			log.debug("Start Activity " + e.acttype + " : Time: " + Time.writeTime(e.time) + " Agent: " + e.agentId);
			log.debug("Score: " + EquilTwoAgentsTest.this.planScorer.getAgentScore(e.agent.getId()));
			if (e.agent.getId().equals(id1)) {
				if (e.acttype.equalsIgnoreCase("w")) {
					//test the time
					//time is time till link 15 + freespeed of link 20
					this.agentOneTime = this.agentOneTime + 359.0;
					log.debug("Car tt to work is: " + (this.agentOneTime - this.agent1LeaveHomeTime));
					assertEquals(this.agentOneTime, e.time, EPSILON);

					//test the score
					//0.25 h = 15 min = 900 s fstt of agent 1 (car)
					this.agentOneScore = 0.25 * -6.0;
					assertEquals(this.agentOneScore, EquilTwoAgentsTest.this.planScorer.getAgentScore(e.agent.getId()), EPSILON);
				}
				else { // it is home
					//test the time
					this.agentOneTime = this.agentOneTime + 359.0;
					assertEquals(this.agentOneTime, e.time, EPSILON);

					//test the score
					//must be negative score for traveling to work (i.e. value of agentOneScore)
					//plus activity score 8 h work typical = 8h, beta_perf = 6
					double deltaScore = (6.0*8.0*Math.log(8.0 / (8.0*Math.exp(-10.0/8.0))));
					log.debug("Scorecalc: " + deltaScore);
					this.agentOneScore = this.agentOneScore + deltaScore;
					//plus negative score for traveling home 39 minutes
					this.agentOneScore = this.agentOneScore + (39.0/60.0 * -6.0);
					assertEquals(this.agentOneScore, EquilTwoAgentsTest.this.planScorer.getAgentScore(e.agent.getId()), EPSILON);
				}
			}
			else if (e.agent.getId().equals(id2)) {
				if (e.acttype.equalsIgnoreCase("w")) {
					//test the time
					//this is the version used in speech:	agentTwoTime = agentTwoTime + 30.0 * 60.0;
					//this is the version used in code:
					this.agentTwoTime = this.agentTwoTime + 1797.0; // 30.0 * 60.0 - 3.0
					assertEquals(this.agentTwoTime, e.time, EPSILON);

					//test the score
					//1097s free speed travel time of agent 2 (non-car)
					this.agentTwoScore = 1797.0/3600.0 * -3.0;
					assertEquals(this.agentTwoScore, EquilTwoAgentsTest.this.planScorer.getAgentScore(e.agent.getId()), EPSILON);
				}
				else {
					//test the time
					//				this is the version used in speech:	agentTwoTime = agentTwoTime + 78.0 * 60.0;
					//this is the version used in code:
					this.agentTwoTime = this.agentTwoTime + 4677.0; // 78.0 * 60.0 - 3.0
					assertEquals(this.agentTwoTime, e.time, EPSILON);

					//test the score
					//must be negative score for traveling to work (i.e. value of agentTwoScore)
					//plus activity score 8 h work typical = 8h, beta_perf = 6
					this.agentTwoScore = this.agentTwoScore + (6.0*8.0*Math.log(8.0 / (8.0*Math.exp(-10.0/8.0))));
					// plus negative score for being late at work in the morning
					this.agentTwoScore = this.agentTwoScore + (this.agent2LeaveHomeTime + 1797.0 - 7.0*3600) * (-18.0 / 3600.0);
					//plus negative score for traveling home 4677.0 seconds by non-car mode (should be 78 min but is less!)
					this.agentTwoScore = this.agentTwoScore + (4677.0/3600.0 * -3.0);
					assertEquals(this.agentTwoScore, EquilTwoAgentsTest.this.planScorer.getAgentScore(e.agent.getId()), EPSILON);

					EquilTwoAgentsTest.this.planScorer.finish();
					log.debug("Score 1: " + EquilTwoAgentsTest.this.planScorer.getAgentScore(id1));
					log.debug("Score 2: " + EquilTwoAgentsTest.this.planScorer.getAgentScore(e.agent.getId()));
				}
			}
		}

		public void handleEvent(final ActEndEvent e) {
			log.debug("End Activity " + e.acttype + " : Time: " + Time.writeTime(e.time) + " Agent: " + e.agentId);
			log.debug("Score: " + EquilTwoAgentsTest.this.planScorer.getAgentScore(e.agent.getId()));

			if (e.agent.getId().equals(id1)) {
				if (e.acttype.equalsIgnoreCase("w")) {
					this.agentOneTime = this.agentOneTime + 8.0 * 3600;
					assertEquals(this.agentOneTime, e.time, EPSILON);
				}
				else {
					assertEquals(this.agent1LeaveHomeTime, e.time, EPSILON);
				}
			}
			else if (e.agent.getId().equals(id2)) {
				if (e.acttype.equalsIgnoreCase("w")) {
					this.agentTwoTime = this.agentTwoTime + 8.0 * 3600;
					assertEquals(this.agentTwoTime, e.time, EPSILON);
				}
				else {
					this.agentTwoTime = this.agent2LeaveHomeTime;
					assertEquals(this.agentTwoTime, e.time, EPSILON);
				}
			}
		}

		public void reset(final int iteration) {
		}
	}

}
