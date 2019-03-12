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

import com.google.inject.Provider;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.passenger.ActivityEngineWithWakeup;
import org.matsim.contrib.dvrp.passenger.BookingEngine;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.run.DvrpConfigConsistencyChecker;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.ActivityEngineModule;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.core.mobsim.qsim.interfaces.TripInfo;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import javax.inject.Singleton;

public class RunOneTaxiWithPrebookingExampleIT{
	@Test
	public void testRun() {
		// load config
		Config config = ConfigUtils.loadConfig( RunOneTaxiExample.CONFIG_FILE, new DvrpConfigGroup(), new OTFVisConfigGroup() );
		config.controler().setLastIteration( 0 );
		config.addConfigConsistencyChecker(new DvrpConfigConsistencyChecker() );
		config.checkConsistency();

		// load scenario
		Scenario scenario = ScenarioUtils.loadScenario(config );

		// setup controler
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new DvrpModule() );
		controler.addOverridingModule(new OneTaxiModule( RunOneTaxiExample.TAXIS_FILE) );
		controler.configureQSimComponents( DvrpQSimComponents.activateModes( TransportMode.taxi ) );
		controler.addOverridingQSimModule( new AbstractQSimModule(){
			@Override protected void configureQSim(){
				this.bind( ActivityEngineWithWakeup.class ).in( Singleton.class ) ;
				this.addQSimComponentBinding( ActivityEngineModule.COMPONENT_NAME ).to( ActivityEngineWithWakeup.class ) ;

				this.bind( BookingEngine.class ) ;
				this.addQSimComponentBinding( "BookingEngine" ).to( BookingEngine.class ) ;

				MapBinder<String, TripInfo.Provider> mapBinder = MapBinder.newMapBinder( this.binder(), String.class, TripInfo.Provider.class );
				mapBinder.addBinding("abc" ).toProvider( new Provider<TripInfo.Provider>() {
					@Override public TripInfo.Provider get(){
						return new PassengerEngine( mode, eventsManager, requestCreator, optimizer, network, requestValidator ) ;
					}
				} );

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
