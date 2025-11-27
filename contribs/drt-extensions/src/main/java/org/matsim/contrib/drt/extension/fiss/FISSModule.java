package org.matsim.contrib.drt.extension.fiss;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetworkModeDepartureHandler;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetworkModeDepartureHandlerDefaultImpl;
import org.matsim.dsim.simulation.net.NetworkTrafficDepartureHandler;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

public class FISSModule extends AbstractModule {
	private static final Logger LOG = LogManager.getLogger(FISSModule.class);

	@Override
	public void install() {

		this.addControllerListenerBinding().toInstance(new StartupListener() {
			@Inject
			private FISSConfigGroup fissConfigGroup;
			@Inject
			private Scenario scenario;

			@Override
			public void notifyStartup(StartupEvent event) {
				Vehicles vehiclesContainer = scenario.getVehicles();

				for (String sampledMode : fissConfigGroup.getSampledModes()) {

					final Id<VehicleType> vehicleTypeId = Id.create(sampledMode, VehicleType.class);
					VehicleType vehicleType = vehiclesContainer.getVehicleTypes().get(vehicleTypeId);
					Gbl.assertNotNull(vehicleType, "you are using mode vehicle types, but have not provided a vehicle type for mode=" + sampledMode);

					final double pcu = vehicleType.getPcuEquivalents() / fissConfigGroup.getSampleFactor();
					LOG.info("Set pcuEquivalent of vehicleType '{}' to {}", vehicleTypeId, pcu);
					vehicleType.setPcuEquivalents(pcu);
				}

			}
		});

		this.installOverridingQSimModule(new AbstractQSimModule() {

			@Inject
			private ControllerConfigGroup controllerConfigGroup;

			@Override
			protected void configureQSim() {

				var delegateDepartureHandlerClass = switch (this.controllerConfigGroup.getMobsim()) {
					case "qsim" -> NetworkModeDepartureHandlerDefaultImpl.class;
					case "dsim" -> NetworkTrafficDepartureHandler.class;
					default ->
						throw new IllegalArgumentException("Unknown mobsim type: " + controllerConfigGroup.getMobsim() + ". FISS only works with ['qsim', or 'dsim'].");
				};

				// bind a base departure handler to which we can delegate from the FISS departure handler.
				bind(NetworkModeDepartureHandler.class)
					.annotatedWith(Names.named("base-network-mode-departure-handler"))
					.to(delegateDepartureHandlerClass)
					.in(Singleton.class);

				// also bind FISS departure handler as default departure handler in the mobsim.
				bind(NetworkModeDepartureHandler.class).to(FISS.class).in(Singleton.class);
			}
		});
	}
}
