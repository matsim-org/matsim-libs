package org.matsim.dsim.simulation.net;

import com.google.inject.Singleton;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetworkModeDepartureHandler;
import org.matsim.dsim.DSimConfigGroup;

public class NetworkTrafficModule extends AbstractQSimModule {

	public final static String COMPONENT_NAME = "NetsimEngine";

	@Override
	protected void configureQSim() {

		bind(NetworkTrafficEngine.class).in(Singleton.class);
		bind(SimNetwork.class).in(Singleton.class);
		bind(ActiveLinks.class).in(Singleton.class);
		bind(ActiveNodes.class).in(Singleton.class);
		bind(NetworkModeDepartureHandler.class).to(NetworkTrafficDepartureHandler.class).in(Singleton.class);

		// register netsim engine and departure handler as Qsim components. This is the mechanism to add
		// things to the SimProcess provider.
		addQSimComponentBinding(COMPONENT_NAME).to(NetworkTrafficEngine.class);
		addQSimComponentBinding(COMPONENT_NAME).to(NetworkModeDepartureHandler.class);

		var dsimConfig = ConfigUtils.addOrGetModule(getConfig(), DSimConfigGroup.class);

		var parkedClass = switch (dsimConfig.getVehicleBehavior()) {
			case teleport -> TeleportedParking.class;
			case wait ->
				throw new IllegalArgumentException("Dsim only supports config:qsim.vehicleBehavior='teleport' and 'exception'. " + getConfig().qsim().getVehicleBehavior() + " is not supported.");
			case exception -> MassConservingParking.class;
		};
		bind(ParkedVehicles.class).to(parkedClass).in(Singleton.class);
	}
}
