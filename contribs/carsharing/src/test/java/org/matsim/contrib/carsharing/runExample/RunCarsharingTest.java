/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package org.matsim.contrib.carsharing.runExample;

import static org.junit.Assert.fail;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.contrib.carsharing.config.OneWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import com.google.inject.Inject;

/**
 * @author nagel
 *
 */
public class RunCarsharingTest {
	private final static Logger log = Logger.getLogger( RunCarsharingTest.class ) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public final void test() {
		try {
			log.info( "class input dir: " + utils.getClassInputDirectory() ) ;

			Config config = ConfigUtils.loadConfig( utils.getClassInputDirectory() + "/config.xml",  
					new FreeFloatingConfigGroup(), 
					new OneWayCarsharingConfigGroup(), 
					new TwoWayCarsharingConfigGroup() ) ;

			config.controler().setOutputDirectory( utils.getOutputDirectory() );

			config.network().setInputFile( utils.getClassInputDirectory()+"/network.xml" );

			config.plans().setInputFile( utils.getClassInputDirectory()+"/10persons.xml");
			config.plans().setInputPersonAttributeFile( utils.getClassInputDirectory()+"/1000desiresAttributes.xml");

			config.facilities().setInputFile( utils.getClassInputDirectory()+"/facilities.xml" );

			FreeFloatingConfigGroup ffConfig = (FreeFloatingConfigGroup) config.getModule( FreeFloatingConfigGroup.GROUP_NAME ) ;
			ffConfig.setvehiclelocations( utils.getClassInputDirectory()+"/Stations.txt");

			OneWayCarsharingConfigGroup oneWayConfig = (OneWayCarsharingConfigGroup) config.getModule( OneWayCarsharingConfigGroup.GROUP_NAME ) ;
			oneWayConfig.setvehiclelocations( utils.getClassInputDirectory()+"/Stations.txt");

			TwoWayCarsharingConfigGroup twoWayConfig = (TwoWayCarsharingConfigGroup) config.getModule( TwoWayCarsharingConfigGroup.GROUP_NAME ) ;
			twoWayConfig.setvehiclelocations( utils.getClassInputDirectory()+"/Stations.txt");

			// ---

			Scenario scenario = ScenarioUtils.loadScenario( config ) ;

			// ---

			final Controler controler = new Controler( scenario );
			RunCarsharing.installCarSharing(controler);

			// ---

			final MyAnalysis myAnalysis = new MyAnalysis();
			controler.addOverridingModule( new AbstractModule(){
				@Override
				public void install() {
					this.bind(MyAnalysis.class).toInstance( myAnalysis ) ;
				}

			});


			// ---

			controler.run();
			
			myAnalysis.testOutput();

		} catch (Exception ee ) {
			fail("something went wrong") ;
		}

		
		log.info("done");
	}

	static class MyAnalysis {
		@Inject private LegHistogram histogram ;
		
		void testOutput() {
			int nofLegs = 0;
			for (int nofDepartures : this.histogram.getDepartures()) {
				nofLegs += nofDepartures;
			}
			log.info("number of legs:\t"  + nofLegs + "\t100%");
			for (String legMode : this.histogram.getLegModes()) {
				int nofModeLegs = 0;
				for (int nofDepartures : this.histogram.getDepartures(legMode)) {
					nofModeLegs += nofDepartures;
				}
//				if (nofModeLegs != 0) {
//					log.warn("number of " + legMode + " legs:\t"  + nofModeLegs + "\t" + (nofModeLegs * 100.0 / nofLegs) + "%");
//					if ( TransportMode.car.equals(legMode) ) {
//						log.info("(car legs include legs by pt vehicles)") ;
//					}
//				}
				if ( TransportMode.car.equals(legMode) ) {
					Assert.assertEquals(4, nofModeLegs );
				} else if ( "freefloating".equals(legMode) ) {
					Assert.assertEquals( 1, nofModeLegs ) ;
				} else if ( "onewaycarsharing".equals(legMode) ) {
					Assert.assertEquals( 1, nofModeLegs ) ;
				} else if ( "twpwaycarsharing".equals(legMode) ) {
					Assert.assertEquals( 6, nofModeLegs ) ;
				}
			}
			
		}
		
	}
}

