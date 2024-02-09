package org.matsim.contrib.dvrp.fleet;

import java.util.LinkedList;
import java.util.List;

import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSource;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

/**
 * This is a convenience class that simplifies the process of adding VRP agents
 * during the QSim.
 */
public class FleetExtensionHelper implements MobsimEngine {
	private final VrpAgentSource source;
	private final Fleet fleet;

	private InternalInterface internalInterface;

	private final List<DvrpVehicle> vehicles = new LinkedList<>();

	FleetExtensionHelper(VrpAgentSource source, Fleet fleet) {
		this.source = source;
		this.fleet = fleet;
	}

	public void addVehicle(DvrpVehicle vehicle) {
		synchronized (vehicles) {
			vehicles.add(vehicle);
		}
	}

	@Override
	public void doSimStep(double time) {
		synchronized (vehicles) {
			for (DvrpVehicle vehicle : vehicles) {
				DynAgent agent = source.addVehicle(vehicle);
				fleet.addVehicle(vehicle);

				internalInterface.arrangeNextAgentState(agent);
			}

			vehicles.clear();
		}
	}

	@Override
	public void onPrepareSim() {
	}

	@Override
	public void afterSim() {
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	static public class FleetExtensionHelperQSimModule extends AbstractDvrpModeQSimModule {
		public FleetExtensionHelperQSimModule(String mode) {
			super(mode);
		}

		@Override
		protected void configureQSim() {
			bindModal(FleetExtensionHelper.class).toProvider(modalProvider(getter -> {
				return new FleetExtensionHelper(getter.getModal(VrpAgentSource.class), getter.getModal(Fleet.class));
			})).asEagerSingleton();
		}
	}
}
