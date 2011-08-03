/* *********************************************************************** *
 * project: org.matsim.*
 * BkControlerDistance
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.distance;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.testcases.MatsimTestCase;

import playground.benjamin.scoring.distance.BkControlerDistance;


/**
 * Tests the scoring of BkControlerDistance and Controler
 * @author dgrether
 *
 */
public class BkDistanceScoringTest extends MatsimTestCase {

	/*package*/ final static Id id1 = new IdImpl("1");
	/*package*/ final static Id id2 = new IdImpl("2");

	private EventsToScore planScorer;

	public void testSingleIterationScoring() {
		Config config = this.loadConfig(this.getClassInputDirectory() + "configScoreTest.xml");
		String netFileName = this.getClassInputDirectory() + "network.xml";
		config.network().setInputFile(netFileName);
		config.plans().setInputFile(this.getClassInputDirectory() + "plansScoreTestV4.xml");

		final BkControlerDistance controler = new BkControlerDistance(config);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);

		controler.addControlerListener(new StartupListener() {

			public void notifyStartup(final StartupEvent event) {
//				double agent1LeaveHomeTime = controler.getPopulation().getPerson(id1).getPlans().get(0).getFirstActivity().getEndTime();
//				double agent2LeaveHomeTime = controler.getPopulation().getPerson(id2).getPlans().get(0).getFirstActivity().getEndTime();
//				controler.getEvents().addHandler(new TestSingleIterationEventHandler(agent1LeaveHomeTime, agent2LeaveHomeTime));
				planScorer = new EventsToScore(controler.getPopulation(), controler.getScoringFunctionFactory(), controler.getConfig().planCalcScore().getLearningRate());
				controler.getEvents().addHandler(planScorer);
			}
		});

		controler.run();
		this.planScorer.finish();
		//this score is calculated as follows:
		//U_total_car = 0                          -(0.2*0.12/1000m)*50000m      +2.26*8*LN(1/(EXP((-10*3600s)/(8*3600s))))   +2.26*12*LN(15/(12*EXP((-10*3600s)/(12*3600s))))
		//U_total_pt  = -(0.1/3600s)*(120min*60)   -(0.0535*0.28/1000m)*50000m   +2.26*8*LN(1/(EXP((-10*3600s)/(8*3600s))))   +2.26*12*LN(14/(12*EXP((-10*3600s)/(12*3600s))))
		assertEquals(50.23165311164136, this.planScorer.getAgentScore(id1), EPSILON);
		assertEquals(48.45180305141843, this.planScorer.getAgentScore(id2), EPSILON);
	}

	public void testSingleIterationControlerScoring() {
		Config config = this.loadConfig(this.getClassInputDirectory() + "configControlerScoreTest.xml");
		String netFileName = this.getClassInputDirectory() + "network.xml";
		config.network().setInputFile(netFileName);
		config.plans().setInputFile(this.getClassInputDirectory() + "plansScoreTestV4.xml");

		final Controler controler = new Controler(config);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);

		controler.addControlerListener(new StartupListener() {

			public void notifyStartup(final StartupEvent event) {
//				double agent1LeaveHomeTime = controler.getPopulation().getPerson(id1).getPlans().get(0).getFirstActivity().getEndTime();
//				double agent2LeaveHomeTime = controler.getPopulation().getPerson(id2).getPlans().get(0).getFirstActivity().getEndTime();
//				controler.getEvents().addHandler(new TestSingleIterationEventHandler(agent1LeaveHomeTime, agent2LeaveHomeTime));
				planScorer = new EventsToScore(controler.getPopulation(), controler.getScoringFunctionFactory(), controler.getConfig().planCalcScore().getLearningRate());
				controler.getEvents().addHandler(planScorer);
			}
		});

		controler.run();
		this.planScorer.finish();
		//this score is calculated as follows:
		//U_total_car = 0                          -(0.2*0.12/1000m)*50000m      +2.26*8*LN(1/(EXP((-10*3600s)/(8*3600s))))   +2.26*12*LN(15/(12*EXP((-10*3600s)/(12*3600s))))
		//U_total_pt  = -(0.1/3600s)*(120min*60)   -(0.0535*0.28/1000m)*50000m   +2.26*8*LN(1/(EXP((-10*3600s)/(8*3600s))))   +2.26*12*LN(14/(12*EXP((-10*3600s)/(12*3600s))))
		assertEquals(50.23165311164136, this.planScorer.getAgentScore(id1), EPSILON);
		assertEquals(48.45180305141842, this.planScorer.getAgentScore(id2), EPSILON);
	}
}
