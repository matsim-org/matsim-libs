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

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractController;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser / Senozon AG
 */
public class PtTutorialTest {

	private static final class EnterVehicleEventCounter implements BasicEventHandler {
		private long cnt = 0 ;
		@Override public void reset(int iteration) {
			cnt = 0 ;
		}
		@Override public void handleEvent( Event event ) {
			if ( event instanceof PersonEntersVehicleEvent ) {
				cnt++ ;
//				System.err.println( event ) ;
			}
		}
		public final long getCnt() {
			return this.cnt;
		}
	}

	private final static Logger log = Logger.getLogger(PtTutorialTest.class);
	
	public @Rule MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void ensure_tutorial_runs() {
		Config config = this.utils.loadConfig("examples/pt-tutorial/0.config.xml");
		config.controler().setLastIteration(1);


		try {
			Controler controler = new Controler(config);
			final EnterVehicleEventCounter enterVehicleEventCounter = new EnterVehicleEventCounter();
			controler.addOverridingModule( new AbstractModule(){
				@Override public void install() {
					this.addEventHandlerBinding().toInstance( enterVehicleEventCounter);
				}
			});
			controler.run();
			Assert.assertEquals( 1867, enterVehicleEventCounter.getCnt() );
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.fail("There shouldn't be any exception, but there was ... :-(");
		}
		final String it1Plans = "ITERS/it.1/1.plans.xml.gz";
		Assert.assertTrue(new File(config.controler().getOutputDirectory(), it1Plans).exists());
		Assert.assertTrue(new File(config.controler().getOutputDirectory(), "output_config.xml.gz").exists());
		
		log.info( AbstractController.DIVIDER ) ;
		log.info( AbstractController.DIVIDER ) ;
		// try to restart from output:
		log.info( AbstractController.DIVIDER ) ;
		log.info( AbstractController.DIVIDER ) ;

		config.plans().setInputFile( config.controler().getOutputDirectory() + "/" + it1Plans ); 

		config.controler().setOverwriteFileSetting( OverwriteFileSetting.overwriteExistingFiles );
		// note: cannot delete directory since some of the input resides there. kai, sep'15
		
		config.controler().setFirstIteration(10);
		config.controler().setLastIteration(10);

		try {
			Controler controler = new Controler(config);
			final EnterVehicleEventCounter enterVehicleEventCounter = new EnterVehicleEventCounter();
			controler.addOverridingModule( new AbstractModule(){
				@Override public void install() {
					this.addEventHandlerBinding().toInstance( enterVehicleEventCounter);
				}
			});
			controler.run();
			System.err.println( " cnt=" +  enterVehicleEventCounter.getCnt() ) ;
			Assert.assertEquals( 1867, enterVehicleEventCounter.getCnt() );
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.fail("There shouldn't be any exception, but there was ... :-(");
		}
		
	}
	
}
