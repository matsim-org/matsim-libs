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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.matsim.testcases.MatsimTestUtils.EPSILON;

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
public class EquilTwoAgentsTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	/*package*/ final static Logger log = LogManager.getLogger(EquilTwoAgentsTest.class);

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

	@Test
	void testSingleIterationPlansV4() {
		final Config config = utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		ConfigUtils.loadConfig(config, IOUtils.extendUrl(utils.classInputResourcePath(), "config.xml"));
		config.plans().setInputFile(IOUtils.extendUrl(utils.classInputResourcePath(), "plans2.xml").toString());

		ScoringConfigGroup pcsConfig = config.scoring() ;
		ActivityParams params = new ActivityParams("h") ;
        params.setTypicalDuration(123456789.0) ; // probably dummy
		params.setScoringThisActivityAtAll(false);
		pcsConfig.addActivityParams(params) ;

		final Controler controler = new Controler(config);
		controler.getConfig().controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.getConfig().controller().setCreateGraphs(false);
        controler.getConfig().controller().setWriteEventsInterval(0);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addControlerListenerBinding().toInstance(new StartupListener() {
					@Inject
					EventsManager eventsManager;

					@Inject
					Population population;

					@Override
					public void notifyStartup(final StartupEvent event) {
						double agent1LeaveHomeTime = ((Activity)population.getPersons()
								.get(personId1)
								.getPlans()
								.get(0)
								.getPlanElements()
								.get(0)).getEndTime().seconds();
						double agent2LeaveHomeTime = ((Activity)population.getPersons()
								.get(personId2)
								.getPlans()
								.get(0)
								.getPlanElements()
								.get(0)).getEndTime().seconds();
						handler = new TestSingleIterationEventHandler(agent1LeaveHomeTime, agent2LeaveHomeTime);
						eventsManager.addHandler(handler);
					}
				});
			}
		});
		controler.run();

		assertEquals(handler.agentOneScore, controler.getScenario().getPopulation().getPersons().get(personId1).getSelectedPlan().getScore(), EPSILON);
		assertEquals(handler.agentTwoScore, controler.getScenario().getPopulation().getPersons().get(personId2).getSelectedPlan().getScore(), EPSILON);
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
