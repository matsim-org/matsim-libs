package org.matsim.dsim.simulation.net;

import com.google.inject.Singleton;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

public class NetworkTrafficModule extends AbstractQSimModule {

	public final static String COMPONENT_NAME = "NetsimEngine";

	@Override
	protected void configureQSim() {

		bind(NetworkTrafficEngine.class).in(Singleton.class);
		bind(SimNetwork.class).in(Singleton.class);
		bind(ActiveLinks.class).in(Singleton.class);
		bind(ActiveNodes.class).in(Singleton.class);

		addQSimComponentBinding(COMPONENT_NAME).to(NetworkTrafficEngine.class);

		var parkedClass = switch (getConfig().qsim().getVehicleBehavior()) {
			case teleport -> TeleportedParking.class;
			case wait ->
				throw new IllegalArgumentException("Dsim only supports config:qsim.vehicleBehavior='teleport' and 'exception'. " + getConfig().qsim().getVehicleBehavior() + " is not supported.");
			case exception -> MassConservingParking.class;
		};
		bind(ParkedVehicles.class).to(parkedClass);
	}
}
