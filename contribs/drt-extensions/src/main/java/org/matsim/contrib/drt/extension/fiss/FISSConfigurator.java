/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.fiss;

import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetworkModeDepartureHandler;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetworkModeDepartureHandlerDefaultImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

/**
 * @author nkuehnel / MOIA, hrewald
 */
public class FISSConfigurator{

	private static final Logger LOG = LogManager.getLogger( FISSConfigurator.class );

	private FISSConfigurator(){
		throw new IllegalStateException( "Utility class" );
	}
	private static class FISSModule extends AbstractModule{
		private final Scenario scenario;
		private final FISSConfigGroup fissConfigGroup;
		public FISSModule( Scenario scenario, FISSConfigGroup fissConfigGroup ){
			this.scenario = scenario;
			this.fissConfigGroup = fissConfigGroup;
		}
		@Override public void install(){

			this.addControlerListenerBinding().toInstance( new StartupListener(){
				@Override public void notifyStartup( StartupEvent event ){
					Vehicles vehiclesContainer = scenario.getVehicles();

					for( String sampledMode : fissConfigGroup.sampledModes ){

						final Id<VehicleType> vehicleTypeId = Id.create( sampledMode, VehicleType.class );
						VehicleType vehicleType = vehiclesContainer.getVehicleTypes().get( vehicleTypeId );

						if( vehicleType == null ){
							vehicleType = VehicleUtils.createVehicleType( vehicleTypeId );
							vehiclesContainer.addVehicleType( vehicleType );
							LOG.info( "Created explicit default vehicle type for mode '{}'", sampledMode );
//				throw new RuntimeException( "I do not understand how this can happen if we are enforcing mode vehicle types from vehicles data.  kai, jan'25" );
						}

						final double pcu = vehicleType.getPcuEquivalents() / fissConfigGroup.sampleFactor;
						LOG.info( "Set pcuEquivalent of vehicleType '{}' to {}", vehicleTypeId, pcu );
						vehicleType.setPcuEquivalents( pcu );
					}

				}
			} );

			this.installOverridingQSimModule( new AbstractQSimModule(){
				@Override protected void configureQSim(){
					bind( NetworkModeDepartureHandler.class ).to( FISS.class ).in( Singleton.class );
					bind( NetworkModeDepartureHandlerDefaultImpl.class ).in( Singleton.class );
					// (dependency of FISS.java)
				}
			} );
		}
	}

	// Should be called late in the setup process (at least after DRT/DVRP modules)
	public static void configure( Controler controler ){

		Scenario scenario = controler.getScenario();
		Config config = controler.getConfig();

		FISSConfigGroup fissConfigGroup = ConfigUtils.addOrGetModule( config, FISSConfigGroup.class );

		// a challenge with the above is that it needs the scenario, but should run before controler start or at least before the first mobsim run.
		// (Maybe it would make sense to put it as a StartupListener, since this is really not a scenario property but a property that belongs
		// to the iterations.  As we can see by the fact that with some of the execution paths the pces are set back to their original values.

		// yyyyyy Question: Are we writing the changed pces into output_(all)Vehicles if they are not set back to their original values in the
		// final iteration?

		controler.addOverridingModule( new FISSModule( scenario, fissConfigGroup ) );

	}

}
