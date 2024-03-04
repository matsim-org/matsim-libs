/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.examples;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser / Senozon AG
 */
public class PtTutorialIT {

	private final static Logger log = LogManager.getLogger(PtTutorialIT.class);

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void ensure_tutorial_runs() throws MalformedURLException {
		Config config = this.utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("pt-tutorial"), "0.config.xml"));
		config.controller().setLastIteration(1);

		try {
			Controler controler = new Controler(config);
			final EnterVehicleEventCounter enterVehicleEventCounter = new EnterVehicleEventCounter();
			final StageActivityDurationChecker stageActivityDurationChecker = new StageActivityDurationChecker();
			controler.addOverridingModule( new AbstractModule(){
				@Override public void install() {
					this.addEventHandlerBinding().toInstance( enterVehicleEventCounter );
					this.addEventHandlerBinding().toInstance( stageActivityDurationChecker );
				}
			});
			controler.run();
			Assertions.assertEquals( 1867, enterVehicleEventCounter.getCnt() );
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assertions.fail("There shouldn't be any exception, but there was ... :-(");
		}
		final String it1Plans = "ITERS/it.1/1.plans.xml.gz";
		Assertions.assertTrue(new File(config.controller().getOutputDirectory(), it1Plans).exists());
		Assertions.assertTrue(new File(config.controller().getOutputDirectory(), "output_config.xml").exists());

		log.info( Controler.DIVIDER ) ;
		log.info( Controler.DIVIDER ) ;
		// try to restart from output:
		log.info( Controler.DIVIDER ) ;
		log.info( Controler.DIVIDER ) ;

		config.plans().setInputFile(new File(utils.getOutputDirectory() + "/" + it1Plans).toURI().toURL().toString());

		config.controller().setOverwriteFileSetting( OverwriteFileSetting.overwriteExistingFiles );
		// note: cannot delete directory since some of the input resides there. kai, sep'15

		config.controller().setFirstIteration(10);
		config.controller().setLastIteration(10);

		try {
			Controler controler = new Controler(config);
			final EnterVehicleEventCounter enterVehicleEventCounter = new EnterVehicleEventCounter();
			final StageActivityDurationChecker stageActivityDurationChecker = new StageActivityDurationChecker();
			controler.addOverridingModule( new AbstractModule(){
				@Override public void install() {
					this.addEventHandlerBinding().toInstance( enterVehicleEventCounter );
					this.addEventHandlerBinding().toInstance( stageActivityDurationChecker );
				}
			});
			controler.run();
			System.err.println( " cnt=" +  enterVehicleEventCounter.getCnt() ) ;
			Assertions.assertEquals( 1867, enterVehicleEventCounter.getCnt() );
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assertions.fail("There shouldn't be any exception, but there was ... :-(");
		}

	}

	private static final class EnterVehicleEventCounter implements PersonEntersVehicleEventHandler {
		private long cnt = 0 ;
		@Override public void reset(int iteration) {
			cnt = 0 ;
		}
		@Override public void handleEvent( PersonEntersVehicleEvent event ) {
			cnt++ ;
		}
		public final long getCnt() {
			return this.cnt;
		}
	}

	/**
	 * tests matsim-org/matsim-libs/issues/917
	 */
	private static final class StageActivityDurationChecker implements ActivityStartEventHandler, ActivityEndEventHandler {
		private Map<Id<Person>, ActivityStartEvent> personId2ActivityStartEvent = new HashMap<>();

		@Override public void reset(int iteration) {
			personId2ActivityStartEvent.clear();
		}
		@Override public void handleEvent( ActivityStartEvent event ) {
			if (StageActivityTypeIdentifier.isStageActivity(event.getActType()) ) {
				personId2ActivityStartEvent.put(event.getPersonId(), event);
			}
		}
		@Override public void handleEvent( ActivityEndEvent endEvent ) {
			if (StageActivityTypeIdentifier.isStageActivity(endEvent.getActType()) ) {
				ActivityStartEvent startEvent = personId2ActivityStartEvent.get(endEvent.getPersonId());
				Assertions.assertEquals(startEvent.getActType(), endEvent.getActType(), "Stage activity should have same type in current ActivityEndEvent and in last ActivityStartEvent, but did not. PersonId " +
								endEvent.getPersonId() +  ", ActivityStartEvent type: " + startEvent.getActType() +  ", ActivityEndEvent type: " + endEvent.getActType() +
								", start time: " + startEvent.getTime() + ", end time: " + endEvent.getTime());
				Assertions.assertEquals(0.0, startEvent.getTime() - endEvent.getTime(), MatsimTestUtils.EPSILON, "Stage activity should have a duration of 0 seconds, but did not. PersonId " +
								endEvent.getPersonId() +  ", start time: " + startEvent.getTime() + ", end time: " + endEvent.getTime());
				personId2ActivityStartEvent.remove(endEvent.getPersonId());
			}
		}
	}

}
