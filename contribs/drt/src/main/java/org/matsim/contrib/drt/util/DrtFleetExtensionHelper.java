package org.matsim.contrib.drt.util;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetExtensionHelper;
import org.matsim.contrib.dvrp.fleet.FleetExtensionHelper.FleetExtensionHelperQSimModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;

/**
 * This is a convenience class that simplifies the process of adding VRP agents
 * to a DRT fleet during the QSim. On top of DVRP's FleetExtensionHelper it also
 * makes sure to initialize the first schedule task.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class DrtFleetExtensionHelper {
	private final FleetExtensionHelper delegate;
	private final DrtTaskFactory taskFactory;
	private final Network network;

	DrtFleetExtensionHelper(FleetExtensionHelper delegate, DrtTaskFactory taskFactory, Network network) {
		this.delegate = delegate;
		this.taskFactory = taskFactory;
		this.network = network;
	}

	public void addVehicle(DvrpVehicleSpecification specification) {
		addVehicle(new DvrpVehicleImpl(specification, network.getLinks().get(specification.getStartLinkId())));
	}

	public void addVehicle(DvrpVehicle vehicle) {
		vehicle.getSchedule().addTask(taskFactory.createInitialTask(vehicle, vehicle.getServiceBeginTime(),
				vehicle.getServiceEndTime(), vehicle.getStartLink()));

		delegate.addVehicle(vehicle);
	}

	static public class DrtFleetExtensionHelperQSimModule extends AbstractDvrpModeQSimModule {
		private final boolean installFleetExtensionHelper;

		public DrtFleetExtensionHelperQSimModule(String mode) {
			this(mode, true);
		}

		public DrtFleetExtensionHelperQSimModule(String mode, boolean installFleetExtensionHelper) {
			super(mode);
			this.installFleetExtensionHelper = installFleetExtensionHelper;
		}

		@Override
		protected void configureQSim() {
			if (installFleetExtensionHelper) {
				install(new FleetExtensionHelperQSimModule(getMode()));
			}

			bindModal(DrtFleetExtensionHelper.class).toProvider(modalProvider(getter -> {
				return new DrtFleetExtensionHelper(getter.getModal(FleetExtensionHelper.class),
						getter.getModal(DrtTaskFactory.class), getter.getModal(Network.class));
			})).asEagerSingleton();

			addModalQSimComponentBinding().to(modalKey(FleetExtensionHelper.class));
		}
	}
}
