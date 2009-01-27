/* *********************************************************************** *
 * project: org.matsim.*
 * BKickControler2Test
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
package playground.benjamin;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.StartupListener;
import org.matsim.scoring.EventsToScore;
import org.matsim.testcases.MatsimTestCase;


/**
 * Tests the scoring of the BKickControler2
 * @author dgrether
 *
 */
public class BKickControler2Test extends MatsimTestCase {

	/*package*/ final static Id id1 = new IdImpl("1");
	/*package*/ final static Id id2 = new IdImpl("2");
	
	private EventsToScore planScorer;

	public void testSingleIterationScoring() {
		Config config = this.loadConfig(this.getClassInputDirectory() + "configScoreTest.xml");
		String netFileName = this.getClassInputDirectory() + "network.xml";
		config.network().setInputFile(netFileName);
		config.plans().setInputFile(this.getClassInputDirectory() + "plansScoreTest.xml");

		final BKickControler2 controler = new BKickControler2(config);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);

		controler.addControlerListener(new StartupListener() {

			public void notifyStartup(final StartupEvent event) {
//				double agent1LeaveHomeTime = controler.getPopulation().getPerson(id1).getPlans().get(0).getFirstActivity().getEndTime();
//				double agent2LeaveHomeTime = controler.getPopulation().getPerson(id2).getPlans().get(0).getFirstActivity().getEndTime();
//				controler.getEvents().addHandler(new TestSingleIterationEventHandler(agent1LeaveHomeTime, agent2LeaveHomeTime));
				planScorer = new EventsToScore(controler.getPopulation(), controler.getScoringFunctionFactory());
				controler.getEvents().addHandler(planScorer);
			}
		});

		controler.run();
		this.planScorer.finish();
		//this score is calculated as follows:
		//
		assertEquals(50.23165311164136, this.planScorer.getAgentScore(id1), EPSILON);
		assertEquals(48.54542805141843, this.planScorer.getAgentScore(id2), EPSILON);
		
	}
	
}
