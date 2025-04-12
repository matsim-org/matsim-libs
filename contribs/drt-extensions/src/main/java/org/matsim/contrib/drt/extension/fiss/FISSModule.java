package org.matsim.contrib.drt.extension.fiss;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetworkModeDepartureHandler;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetworkModeDepartureHandlerDefaultImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class FISSModule extends AbstractModule{
	private static final Logger LOG = LogManager.getLogger( FISSModule.class );

	@Override public void install(){

		this.addControlerListenerBinding().toInstance( new StartupListener(){
			@Inject private FISSConfigGroup fissConfigGroup;
			@Inject private Scenario scenario;

			@Override public void notifyStartup( StartupEvent event ){
				Vehicles vehiclesContainer = scenario.getVehicles();

				for( String sampledMode : fissConfigGroup.sampledModes ){

					final Id<VehicleType> vehicleTypeId = Id.create( sampledMode, VehicleType.class );
					VehicleType vehicleType = vehiclesContainer.getVehicleTypes().get( vehicleTypeId );
					Gbl.assertNotNull( vehicleType, "you are using mode vehicle types, but have not provided a vehicle type for mode=" + sampledMode );

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
