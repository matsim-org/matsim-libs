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

package org.matsim.contrib.dvrp.examples.onetaxi;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.dynagent.run.DynActivityEngine;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class RunOneTaxiWithPrebookingExampleIT {
	private static final Logger log = Logger.getLogger(RunOneTaxiWithPrebookingExampleIT.class);

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRun() {
		// load config
		Config config = ConfigUtils.loadConfig(RunOneTaxiExample.CONFIG_FILE, new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		config.controler().setLastIteration(0);

		config.controler().setOutputDirectory(utils.getOutputDirectory());

		DvrpConfigGroup dvrpConfig = ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);

		QSimComponentsConfigGroup componentsConfig = ConfigUtils.addOrGetModule(config, QSimComponentsConfigGroup.class);
		for (String component : componentsConfig.getActiveComponents()) {
			log.info("mobsimComponent=" + component);
		}

		config.addConfigConsistencyChecker(new DvrpConfigConsistencyChecker());
		config.checkConsistency();

		// load scenario
		Scenario scenario = ScenarioUtils.loadScenario(config);

		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			plan.getAttributes().putAttribute( ActivityEngineWithWakeup.PREBOOKING_OFFSET_ATTRIBUTE_NAME, 900. );
			//			for( PlanElement pe : plan.getPlanElements() ){
			//				if ( pe instanceof Leg ) {
			//					if ( ((Leg) pe).getMode().equals( TransportMode.drt ) || ((Leg) pe).getMode().equals( TransportMode.taxi ) ) {
			//						log.warn("adding attribute ...") ;
			//						pe.getAttributes().putAttribute( ActivityEngineWithWakeup.PREBOOKING_OFFSET_ATTRIBUTE_NAME, 900. ) ;
			//					}
			//				}
			//			}
		}

//		scenario.getPopulation().getPersons().values().removeIf( result -> !result.getId().toString().equals( "passenger_0" ) );

		PopulationUtils.writePopulation(scenario.getPopulation(), utils.getOutputDirectory() + "/../pop.xml");

		// setup controler
		Controler controler = new Controler(scenario);

		controler.configureQSimComponents( new QSimComponentsConfigurator(){
			@Override
			public void configure( QSimComponentsConfig components ){
				// this method, other than the methods in addOverriding..., is _not_ additive.  It always starts afresh, from the default configuration.
				components.removeNamedComponent( ActivityEngineModule.COMPONENT_NAME );
				components.addNamedComponent( "abc" );
				components.addNamedComponent( "def" );
				components.addNamedComponent( PassengerModule.BookingEngineQSimModule.COMPONENT_NAME );
				for( String m : new String[]{TransportMode.taxi} ){
					components.addComponent( DvrpModes.mode( m ) );
				}
			}
		} );
		// yyyy in the somewhat longer run, would rather not have the components configuration in user code.  kai, mar'19

		controler.addOverridingModule(new DvrpModule());

		controler.addOverridingModule(new OneTaxiModule(RunOneTaxiExample.TAXIS_FILE));
		// yyyy I find it unexpected to have an example as "module".  kai, mar'19

		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override protected void configureQSim() {
				this.addQSimComponentBinding( "def" ).to( ActivityEngineWithWakeup.class ) ;
				this.addQSimComponentBinding( "abc" ).to( DynActivityEngine.class ) ;
			}
		});

//		controler.addOverridingModule(new OTFVisLiveModule() ); // OTFVis visualisation

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.bind( Checker.class ).in( Singleton.class ) ;
				this.addEventHandlerBinding().to( Checker.class ) ;
				this.addControlerListenerBinding().to( Checker.class ) ;
			}
		});

		// run simulation
		controler.run();
	}

	private static class Checker implements BasicEventHandler, ShutdownListener {
		private int cnt = 0;
		private int cnt2 = 0;

		@Override
		public synchronized void handleEvent( Event event ) {
			if (event instanceof ActivityEngineWithWakeup.AgentWakeupEvent) {
				final ActivityEngineWithWakeup.AgentWakeupEvent ev = (ActivityEngineWithWakeup.AgentWakeupEvent)event;
//				System.out.println() ;
				System.err.println(event) ;
//				System.out.println("") ;
				switch (cnt) {
					case 0:
					case 1:
					case 2:
					case 3:
						Assert.assertEquals(0., event.getTime(), Double.MIN_VALUE );
						final List<String> personIds = Arrays.asList("passenger_0", "passenger_1",
								"passenger_2", "passenger_3" );
						Assert.assertTrue(personIds.contains(ev.getPersonId().toString()));
						break;
					case 4:
						Assert.assertEquals(300., event.getTime(), Double.MIN_VALUE);
						Assert.assertEquals("passenger_4", ev.getPersonId().toString());
						break;
					case 5:
						Assert.assertEquals(600., event.getTime(), Double.MIN_VALUE);
						Assert.assertEquals("passenger_5", ev.getPersonId().toString());
						break;
					case 6:
						Assert.assertEquals(900., event.getTime(), Double.MIN_VALUE);
						Assert.assertEquals("passenger_6", ev.getPersonId().toString());
						break;
					case 7:
						Assert.assertEquals(1200., event.getTime(), Double.MIN_VALUE);
						Assert.assertEquals("passenger_7", ev.getPersonId().toString());
						break;
					case 8:
						Assert.assertEquals(1500., event.getTime(), Double.MIN_VALUE);
						Assert.assertEquals("passenger_8", ev.getPersonId().toString());
						break;
					case 9:
						Assert.assertEquals(1800., event.getTime(), Double.MIN_VALUE);
						Assert.assertEquals("passenger_9", ev.getPersonId().toString());
						break;
				}
				cnt++;
			} else if (event instanceof PassengerRequestScheduledEvent ) {
				PassengerRequestScheduledEvent ev = (PassengerRequestScheduledEvent)event;
//				System.out.println("") ;
				System.err.println( event) ;
//				System.out.println(""); ;
				Assert.assertEquals("taxi_one", ev.getVehicleId().toString());
				switch (cnt2) {
					case 0:
						Assert.assertEquals(61.66, ev.getPickupTime(), 0.1 );
						Assert.assertEquals("taxi_0", ev.getRequestId().toString());
						break;
					case 1:
						Assert.assertEquals(567.0, ev.getPickupTime(), 0.1 );
						Assert.assertEquals("taxi_1", ev.getRequestId().toString());
						break;
					case 2:
						Assert.assertEquals(954.33, ev.getPickupTime(), 0.1 );
						Assert.assertEquals("taxi_2", ev.getRequestId().toString());
						break;
					case 3:
						Assert.assertEquals(1401.66, ev.getPickupTime(), 0.1 );
						Assert.assertEquals("taxi_3", ev.getRequestId().toString());
						break;
					case 4:
						Assert.assertEquals(1977.8, ev.getPickupTime(), 0.1);
						Assert.assertEquals("taxi_4", ev.getRequestId().toString());
						break;
					case 5:
						Assert.assertEquals(2503.46, ev.getPickupTime(), 0.1);
						Assert.assertEquals("taxi_5", ev.getRequestId().toString());
						break;
					case 6:
						Assert.assertEquals(2932.46, ev.getPickupTime(), 0.1);
						Assert.assertEquals("taxi_6", ev.getRequestId().toString());
						break;
					case 7:
						Assert.assertEquals(3317.46, ev.getPickupTime(), 0.1);
						Assert.assertEquals("taxi_7", ev.getRequestId().toString());
						break;
					case 8:
						Assert.assertEquals(3944.86, ev.getPickupTime(), 0.1);
						Assert.assertEquals("taxi_8", ev.getRequestId().toString());
						break;
					case 9:
						Assert.assertEquals(4333.53, ev.getPickupTime(), 0.1);
						Assert.assertEquals("taxi_9", ev.getRequestId().toString());
						break;
				}
				cnt2++;
			} else if ( event instanceof HasPersonId ) {
//				if ( ((HasPersonId) event).getPersonId().toString().equals( "passenger_0" )){
					System.err.println( event );
//				}
			}
		}

		@Override
		public void notifyShutdown( ShutdownEvent event ){
			log.info("cnt=" + cnt ) ;
			log.info("cnt2=" + cnt2 ) ;
//			Assert.assertEquals( 10, cnt );
//			Assert.assertEquals( 10, cnt2 );
		}
	}
}
