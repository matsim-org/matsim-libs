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

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.contrib.carsharing.config.OneWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
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
		Config config = ConfigUtils.loadConfig( utils.getClassInputDirectory() + "/config.xml",  
				new FreeFloatingConfigGroup(), 
				new OneWayCarsharingConfigGroup(), 
				new TwoWayCarsharingConfigGroup(),
				new CarsharingConfigGroup() ) ;

		config.controler().setOutputDirectory( utils.getOutputDirectory() );
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.overwriteExistingFiles );

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
//		controler.setDirtyShutdown(true);
		
		RunCarsharing.installCarSharing(controler);

		final MyAnalysis myAnalysis = new MyAnalysis();
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() {
				this.bind(MyAnalysis.class).toInstance( myAnalysis ) ;
				this.addControlerListenerBinding().toInstance( myAnalysis ) ;
			}
		});


		// ---

		controler.run();

		log.info("done");
	}

	static class MyAnalysis implements AfterMobsimListener {
		@Inject private LegHistogram histogram ;

		void testOutput(int iteration) {
			int nofLegs = 0;
			for (int nofDepartures : this.histogram.getDepartures()) {
				nofLegs += nofDepartures;
			}
			log.info("number of legs:\t"  + nofLegs + "\t100%");
			for (String legMode : this.histogram.getLegModes()) {
				int nOfModeLegs = 0;
				for (int nofDepartures : this.histogram.getDepartures(legMode)) {
					nOfModeLegs += nofDepartures;
				}
				//				if (nofModeLegs != 0) {
				//					log.warn("number of " + legMode + " legs:\t"  + nofModeLegs + "\t" + (nofModeLegs * 100.0 / nofLegs) + "%");
				//					if ( TransportMode.car.equals(legMode) ) {
				//						log.info("(car legs include legs by pt vehicles)") ;
				//					}
				//				}
				if ( iteration==1 ) {
					if ( TransportMode.car.equals(legMode) ) {
						Assert.assertEquals(10, nOfModeLegs );
					} else if ( "freefloating".equals(legMode) ) {
						Assert.assertEquals( 0, nOfModeLegs ) ;
					} else if ( "onewaycarsharing".equals(legMode) ) {
						Assert.assertEquals( 0, nOfModeLegs ) ;
					} else if ( "twowaycarsharing".equals(legMode) ) {
						Assert.assertEquals( 2, nOfModeLegs ) ;
					}
					else if ( "walk_rb".equals(legMode) ) {
						Assert.assertEquals( 2, nOfModeLegs ) ;
					}
				} else if ( iteration==2 ) {
					if ( TransportMode.car.equals(legMode) ) {
						Assert.assertEquals(6, nOfModeLegs );
					} else if ( "freefloating".equals(legMode) ) {
						Assert.assertEquals( 2, nOfModeLegs ) ;
					} else if ( "onewaycarsharing".equals(legMode) ) {
						Assert.assertEquals( 1, nOfModeLegs ) ;
					} else if ( "twowaycarsharing".equals(legMode) ) {
						Assert.assertEquals( 4, nOfModeLegs ) ;
					}
					else if ( "walk_rb".equals(legMode) ) {
						Assert.assertEquals( 4, nOfModeLegs ) ; 
					}
				} else if ( iteration==3 ) {
					if ( TransportMode.car.equals(legMode) ) {
						Assert.assertEquals(6, nOfModeLegs );
					} else if ( "freefloating".equals(legMode) ) {
						Assert.assertEquals( 2, nOfModeLegs ) ;
					} else if ( "onewaycarsharing".equals(legMode) ) {
						Assert.assertEquals( 2, nOfModeLegs ) ;
					} else if ( "twowaycarsharing".equals(legMode) ) {
						Assert.assertEquals( 6, nOfModeLegs ) ;
					}
					else if ( "walk_rb".equals(legMode) ) {
						Assert.assertEquals( 6, nOfModeLegs ) ; 
					}
				} else if ( iteration==4 ) {
					if ( TransportMode.car.equals(legMode) ) {
						Assert.assertEquals(6, nOfModeLegs );
					} 
					else if ( "freefloating".equals(legMode) ) {
						Assert.assertEquals(4, nOfModeLegs ) ;
					} 
					else if ( "onewaycarsharing".equals(legMode) ) {
						Assert.assertEquals( 2, nOfModeLegs ) ;
					} 
					else if ( "twowaycarsharing".equals(legMode) ) {
						Assert.assertEquals( 4, nOfModeLegs ) ;
					} 
					else if ( "walk_ff".equals(legMode) ) {
						Assert.assertEquals( 4, nOfModeLegs ) ; // presumably: walking to the car, but parking the car at the destination => ff x 1
					} 
					else if ( "walk_ow_sb".equals(legMode) ) {
						Assert.assertEquals( 4, nOfModeLegs ) ; // presumably: walk to car, walk from car => 1-way x 2
					} 
					else if ( "walk_rb".equals(legMode) ) {
						Assert.assertEquals( 4, nOfModeLegs ) ; 
					}
				} else if ( iteration==10 ) {
					if ( TransportMode.car.equals(legMode) ) {
						Assert.assertEquals(6, nOfModeLegs );
					} else if ( "freefloating".equals(legMode) ) {
						Assert.assertEquals(2, nOfModeLegs ) ;
					} else if ( "onewaycarsharing".equals(legMode) ) {
						Assert.assertEquals( 1, nOfModeLegs ) ;
					} else if ( "twowaycarsharing".equals(legMode) ) {
						Assert.assertEquals( 4, nOfModeLegs ) ;
					}
					else if ( "walk_rb".equals(legMode) ) {
						Assert.assertEquals( 4, nOfModeLegs ) ;
					}
				}
			}

		}

		@Override
		public void notifyAfterMobsim(AfterMobsimEvent event) {
			testOutput( event.getIteration() ) ;
		}

	}
}

