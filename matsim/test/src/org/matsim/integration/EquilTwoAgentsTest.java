/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.controler.corelisteners.PlansScoring;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.StartupListener;
import org.matsim.events.EventActivityEnd;
import org.matsim.events.EventActivityStart;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.handler.EventHandlerActivityEndI;
import org.matsim.events.handler.EventHandlerActivityStartI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.scoring.EventsToScore;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.misc.Time;


/**
 * @author dgrether
 *
 */
public class EquilTwoAgentsTest extends MatsimTestCase {

	private double agent1LeaveHomeTime, agent2LeaveHomeTime;

	private EventsToScore planScorer;

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
		
		controler.addControlerListener(new StartupListener() {
			
			public void notifyStartup(StartupEvent event) {
				Controler controler = event.getControler();
				controler.getEvents().addHandler(new TestSingleIterationEventHandler());
			  agent1LeaveHomeTime = event.getControler().getPopulation().getPerson(new IdImpl("1")).getPlans().get(0).getFirstActivity().getEndTime();
			  agent2LeaveHomeTime = event.getControler().getPopulation().getPerson(new IdImpl("2")).getPlans().get(0).getFirstActivity().getEndTime();
			
			  planScorer = new EventsToScore(controler.getPopulation(), controler.getScoringFunctionFactory());
				event.getControler().getEvents().addHandler(planScorer);
			}
		});
		
		controler.run();
	}
	
	
	
	private class TestSingleIterationEventHandler implements EventHandlerLinkEnterI, EventHandlerActivityStartI, EventHandlerActivityEndI {

		private double agentOneTime, agentTwoTime;
		
		private double agentOneScore, agentTwoScore;
		
		public void handleEvent(EventLinkEnter e) {
			System.out.println("Enter Link:" + e.linkId + " at Time: " + e.time);
			if (e.link.getId().equals(new IdImpl("6"))) {
				agentOneTime = agent1LeaveHomeTime + 1.0;
				assertEquals(agentOneTime, e.time);
			}
			else if (e.link.getId().equals(new IdImpl("15"))) {
				agentOneTime = agentOneTime + 359.0 + 1.0;
				assertEquals(agentOneTime, e.time);
			}
			else if (e.link.getId().equals(new IdImpl("20"))) {
				agentOneTime = agentOneTime + 179.0 + 1.0;
				assertEquals(agentOneTime, e.time);
			}
			else if (e.link.getId().equals(new IdImpl("21"))) {
				agentOneTime = agentOneTime + 1.0;
				assertEquals(agentOneTime, e.time);
			}
			else if (e.link.getId().equals(new IdImpl("22"))) {
				System.out.println("22");
				agentOneTime = agentOneTime + 359.0 + 1.0;
				assertEquals(agentOneTime, e.time);
			}
			else if (e.link.getId().equals(new IdImpl("23"))) {
				System.out.println("23");
				agentOneTime = agentOneTime + 1259.0 + 1.0;
				assertEquals(agentOneTime, e.time);
			}
			else if (e.link.getId().equals(new IdImpl("1"))) {
				agentOneTime = agentOneTime + 359.0 + 1.0;
				assertEquals(agentOneTime, e.time);
			}
		}

		public void handleEvent(EventActivityStart e) {
			System.out.println("Start Activity " + e.acttype + " : Time: " + Time.writeTime(e.time) + " Agent: " + e.agentId);
			System.out.println("Score: " + planScorer.getAgentScore(e.agent.getId()));
			if (e.agent.getId().equals(new IdImpl("1"))) {
				if (e.acttype.equalsIgnoreCase("w")) {
					//test the time
					//time is time till link 15 + freespeed of link 20 
					agentOneTime = agentOneTime + 359.0;
					System.out.println("Car tt to work is: " + (agentOneTime - agent1LeaveHomeTime));
					assertEquals(agentOneTime, e.time);
					
					//test the score
					//0.25 h = 15 min = 900 s fstt of agent 1 (car)
					agentOneScore = 0.25 * -6.0;
					assertEquals(agentOneScore, planScorer.getAgentScore(e.agent.getId()));
				}
				else { // it is home
					//test the time 
					agentOneTime = agentOneTime + 359.0;
					assertEquals(agentOneTime, e.time);
					
					//test the score
					//must be negative score for traveling to work (i.e. value of agentOneScore)  
					//plus activity score 8 h work typical = 8h, beta_perf = 6
					System.out.println("Scorecalc: " + (6*8*Math.log(8/ (8*Math.exp(-10.0/8.0)))));
					agentOneScore = agentOneScore + (6.0*8.0*Math.log(8.0/ (8.0*Math.exp(-10.0/8.0))));
					//plus negative score for traveling home 39 minutes
					agentOneScore = agentOneScore + (39.0/60.0 * -6.0);
					assertEquals(agentOneScore, planScorer.getAgentScore(e.agent.getId()));
				}
			}
			else if (e.agent.getId().equals(new IdImpl("2"))) {
				if (e.acttype.equalsIgnoreCase("w")) {
					//test the time
					//this is the version used in speech:	agentTwoTime = agentTwoTime + 30.0 * 60.0;
					//this is the version used in code:
					agentTwoTime = agentTwoTime + 1078.0;
					assertEquals(agentTwoTime, e.time);
					
					//test the score
					//1078 s fstt of agent 2 (non-car)
					agentTwoScore = 1078.0/3600.0 * -3.0;
					assertEquals(agentTwoScore, planScorer.getAgentScore(e.agent.getId()));
				}
				else {
					//test the time
//				this is the version used in speech:	agentTwoTime = agentTwoTime + 108.0 * 60.0;
					//this is the version used in code:
					agentTwoTime = agentTwoTime + 3958.0;
					assertEquals(agentTwoTime, e.time);

					//test the score
					//must be negative score for traveling to work (i.e. value of agentTwoScore)  
					//plus activity score 8 h work typical = 8h, beta_perf = 6
					agentTwoScore = agentTwoScore + (6.0*8.0*Math.log(8.0/ (8.0*Math.exp(-10.0/8.0))));
					//plus negative score for traveling home 3958.0 seconds by non-car mode (should be 78 min but is less!)
					agentTwoScore = agentTwoScore + (3958.0/3600.0 * -3.0);
					assertEquals(agentTwoScore, planScorer.getAgentScore(e.agent.getId()));
					
					planScorer.finish();
					System.out.println("Score 1: " + planScorer.getAgentScore(new IdImpl("1")));		
					System.out.println("Score 2: " + planScorer.getAgentScore(e.agent.getId()));		

				}
			}
		}
		
		public void handleEvent(EventActivityEnd e) {
			System.out.println("End Activity " + e.acttype + " : Time: " + Time.writeTime(e.time) + " Agent: " + e.agentId);
			System.out.println("Score: " + planScorer.getAgentScore(e.agent.getId()));
			
			if (e.agent.getId().equals(new IdImpl("1"))) {
				if (e.acttype.equalsIgnoreCase("w")) {
					agentOneTime = agentOneTime + 8.0 * 3600;
					assertEquals(agentOneTime, e.time);
				}
				else {
					assertEquals(agent1LeaveHomeTime, e.time);
				}
			}
			else if (e.agent.getId().equals(new IdImpl("2"))) {
				if (e.acttype.equalsIgnoreCase("w")) {
					agentTwoTime = agentTwoTime + 8.0 * 3600;
					assertEquals(agentTwoTime, e.time);
				}
				else {
					System.out.println("2");
					agentTwoTime = agent2LeaveHomeTime;
					assertEquals(agentTwoTime, e.time);
				}
				
				
			}
		}

		public void reset(int iteration) {
			
		}

		
	};
	
	

}
