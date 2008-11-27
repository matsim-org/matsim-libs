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
 * @author dgrether
 */
public class EquilTwoAgentsTest extends MatsimTestCase {

	private double agent1LeaveHomeTime, agent2LeaveHomeTime;

	/*package*/ EventsToScore planScorer;

	/**
	 * @see org.matsim.testcases.MatsimTestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();

	}


	public void testSingleIteration() {
		Config config = this.loadConfig(this.getClassInputDirectory() + "config.xml");
		String netFileName = "test/scenarios/equil/network.xml";
		config.network().setInputFile(netFileName);
		config.plans().setInputFile(this.getClassInputDirectory() + "plans2.xml");

		Controler controler = new Controler(config);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);

		controler.addControlerListener(new StartupListener() {

			public void notifyStartup(final StartupEvent event) {
				Controler controler = event.getControler();
				controler.getEvents().addHandler(new TestSingleIterationEventHandler());
			  EquilTwoAgentsTest.this.agent1LeaveHomeTime = event.getControler().getPopulation().getPerson(new IdImpl("1")).getPlans().get(0).getFirstActivity().getEndTime();
			  EquilTwoAgentsTest.this.agent2LeaveHomeTime = event.getControler().getPopulation().getPerson(new IdImpl("2")).getPlans().get(0).getFirstActivity().getEndTime();

			  EquilTwoAgentsTest.this.planScorer = new EventsToScore(controler.getPopulation(), controler.getScoringFunctionFactory());
				event.getControler().getEvents().addHandler(EquilTwoAgentsTest.this.planScorer);
			}
		});

		controler.run();
	}



	private class TestSingleIterationEventHandler implements LinkEnterEventHandler, ActStartEventHandler, ActEndEventHandler {

		private double agentOneTime, agentTwoTime;

		private double agentOneScore, agentTwoScore;

		public void handleEvent(final LinkEnterEvent e) {
			// TODO [DG,MR,performance] do not create new IdImpl for every event, but do it once as static members.
			System.out.println("Enter Link:" + e.linkId + " at Time: " + e.time);
			if (e.link.getId().equals(new IdImpl("6"))) {
				this.agentOneTime = EquilTwoAgentsTest.this.agent1LeaveHomeTime + 1.0;
				assertEquals(this.agentOneTime, e.time, EPSILON);
			}
			else if (e.link.getId().equals(new IdImpl("15"))) {
				this.agentOneTime = this.agentOneTime + 359.0 + 1.0;
				assertEquals(this.agentOneTime, e.time, EPSILON);
			}
			else if (e.link.getId().equals(new IdImpl("20"))) {
				this.agentOneTime = this.agentOneTime + 179.0 + 1.0;
				assertEquals(this.agentOneTime, e.time, EPSILON);
			}
			else if (e.link.getId().equals(new IdImpl("21"))) {
				this.agentOneTime = this.agentOneTime + 1.0;
				assertEquals(this.agentOneTime, e.time, EPSILON);
			}
			else if (e.link.getId().equals(new IdImpl("22"))) {
				System.out.println("22");
				this.agentOneTime = this.agentOneTime + 359.0 + 1.0;
				assertEquals(this.agentOneTime, e.time, EPSILON);
			}
			else if (e.link.getId().equals(new IdImpl("23"))) {
				System.out.println("23");
				this.agentOneTime = this.agentOneTime + 1259.0 + 1.0;
				assertEquals(this.agentOneTime, e.time, EPSILON);
			}
			else if (e.link.getId().equals(new IdImpl("1"))) {
				this.agentOneTime = this.agentOneTime + 359.0 + 1.0;
				assertEquals(this.agentOneTime, e.time, EPSILON);
			}
		}

		public void handleEvent(final ActStartEvent e) {
			System.out.println("Start Activity " + e.acttype + " : Time: " + Time.writeTime(e.time) + " Agent: " + e.agentId);
			System.out.println("Score: " + EquilTwoAgentsTest.this.planScorer.getAgentScore(e.agent.getId()));
			if (e.agent.getId().equals(new IdImpl("1"))) {
				if (e.acttype.equalsIgnoreCase("w")) {
					//test the time
					//time is time till link 15 + freespeed of link 20
					this.agentOneTime = this.agentOneTime + 359.0;
					System.out.println("Car tt to work is: " + (this.agentOneTime - EquilTwoAgentsTest.this.agent1LeaveHomeTime));
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
					System.out.println("Scorecalc: " + (6*8*Math.log(8/ (8*Math.exp(-10.0/8.0)))));
					this.agentOneScore = this.agentOneScore + (6.0*8.0*Math.log(8.0/ (8.0*Math.exp(-10.0/8.0))));
					//plus negative score for traveling home 39 minutes
					this.agentOneScore = this.agentOneScore + (39.0/60.0 * -6.0);
					assertEquals(this.agentOneScore, EquilTwoAgentsTest.this.planScorer.getAgentScore(e.agent.getId()), EPSILON);
				}
			}
			else if (e.agent.getId().equals(new IdImpl("2"))) {
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
					this.agentTwoScore = this.agentTwoScore + (6.0*8.0*Math.log(8.0/ (8.0*Math.exp(-10.0/8.0))));
					//plus negative score for traveling home 4677.0 seconds by non-car mode (should be 78 min but is less!)
					this.agentTwoScore = this.agentTwoScore + (4677.0/3600.0 * -3.0);
					assertEquals(this.agentTwoScore, EquilTwoAgentsTest.this.planScorer.getAgentScore(e.agent.getId()), EPSILON);

					EquilTwoAgentsTest.this.planScorer.finish();
					System.out.println("Score 1: " + EquilTwoAgentsTest.this.planScorer.getAgentScore(new IdImpl("1")));
					System.out.println("Score 2: " + EquilTwoAgentsTest.this.planScorer.getAgentScore(e.agent.getId()));

				}
			}
		}

		public void handleEvent(final ActEndEvent e) {
			System.out.println("End Activity " + e.acttype + " : Time: " + Time.writeTime(e.time) + " Agent: " + e.agentId);
			System.out.println("Score: " + EquilTwoAgentsTest.this.planScorer.getAgentScore(e.agent.getId()));

			if (e.agent.getId().equals(new IdImpl("1"))) {
				if (e.acttype.equalsIgnoreCase("w")) {
					this.agentOneTime = this.agentOneTime + 8.0 * 3600;
					assertEquals(this.agentOneTime, e.time, EPSILON);
				}
				else {
					assertEquals(EquilTwoAgentsTest.this.agent1LeaveHomeTime, e.time, EPSILON);
				}
			}
			else if (e.agent.getId().equals(new IdImpl("2"))) {
				if (e.acttype.equalsIgnoreCase("w")) {
					this.agentTwoTime = this.agentTwoTime + 8.0 * 3600;
					assertEquals(this.agentTwoTime, e.time, EPSILON);
				}
				else {
					System.out.println("2");
					this.agentTwoTime = EquilTwoAgentsTest.this.agent2LeaveHomeTime;
					assertEquals(this.agentTwoTime, e.time, EPSILON);
				}
			}
		}

		public void reset(final int iteration) {

		}

	}

}
