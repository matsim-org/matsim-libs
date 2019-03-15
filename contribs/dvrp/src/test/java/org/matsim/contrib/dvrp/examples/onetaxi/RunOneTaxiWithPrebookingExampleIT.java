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

import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.passenger.ActivityEngineWithWakeup;
import org.matsim.contrib.dvrp.passenger.BookingEngine;
import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.dynagent.run.DynActivityEngineModule;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.ActivityEngineModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import javax.inject.Singleton;

public class RunOneTaxiWithPrebookingExampleIT{
	private static final Logger log = Logger.getLogger(RunOneTaxiWithPrebookingExampleIT.class);

	@Test
	public void testRun() {
		// load config
		Config config = ConfigUtils.loadConfig( RunOneTaxiExample.CONFIG_FILE, new DvrpConfigGroup(), new OTFVisConfigGroup() );
		config.controler().setLastIteration( 0 );

		DvrpConfigGroup dvrpConfig = ConfigUtils.addOrGetModule( config, DvrpConfigGroup.class );

		QSimComponentsConfigGroup componentsConfig = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );
		for( String component : componentsConfig.getActiveComponents() ){
			log.info( "mobsimComponent=" + component ) ;
		}

		config.addConfigConsistencyChecker(new DvrpConfigConsistencyChecker() );
		config.checkConsistency();

		// load scenario
		Scenario scenario = ScenarioUtils.loadScenario(config );

		for( Person person : scenario.getPopulation().getPersons().values() ){
			
		}

		// setup controler
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new DvrpModule() );
		controler.addOverridingModule(new OneTaxiModule( RunOneTaxiExample.TAXIS_FILE) );
		controler.configureQSimComponents( components -> {
			log.info("=== before ...") ;
			for( Object component : components.getActiveComponents() ){
				log.info( component.toString() ) ;
			}
			components.removeNamedComponent( ActivityEngineModule.COMPONENT_NAME );
			components.addNamedComponent( DynActivityEngineModule.COMPONENT_NAME );
//			components.addNamedComponent( "abc" );
			components.addNamedComponent( "BookingEngine" );
			for( String m : new String[]{TransportMode.taxi} ){
				components.addComponent( DvrpModes.mode( m ) );
				// note that this is not an "addNAMEDComponent"!
			}
			log.info("=== after ...") ;
			for( Object component : components.getActiveComponents() ){
				log.info( component.toString() ) ;
			}
		} );
		controler.addOverridingQSimModule( new AbstractQSimModule(){
			@Override protected void configureQSim(){
				this.bind( ActivityEngineWithWakeup.class ).in( Singleton.class ) ;
//				this.addQSimComponentBinding( "abc" ).to( ActivityEngineWithWakeup.class ) ;

				this.bind( BookingEngine.class ).in( Singleton.class ) ;
				this.addQSimComponentBinding( "BookingEngine" ).to( BookingEngine.class ) ;

//				MapBinder<String, TripInfo.Provider> mapBinder = MapBinder.newMapBinder( this.binder(), String.class, TripInfo.Provider.class );
//				mapBinder.addBinding("abc" ).toProvider( new Provider<TripInfo.Provider>() {
//					@Override public TripInfo.Provider get(){
//						return new PassengerEngine( mode, eventsManager, requestCreator, optimizer, network, requestValidator ) ;
//					}
//				} );

//				this.binder().bind( TripInfo.Provider.class ).annotatedWith( Names.named( TransportMode.taxi ) ).to( PassengerEngine.class ) ;
				// does not work since PassengerEngine does not have a constructor annotated with @Inject.  kai, mar'19
				// In general I am not sure if this is the right syntax, or if one should rather use a Multibinder or a MultiSet.  kai, mar'19
			}
		} ) ;

		if ( true ) {
			controler.addOverridingModule(new OTFVisLiveModule() ); // OTFVis visualisation
		}

		// run simulation
		controler.run();
	}
}
