package org.matsim.contrib.drt.extension.shifts.run;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.Fleets;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.drt.extension.shifts.fleet.DefaultShiftDvrpVehicle;
import org.matsim.core.modal.ModalProviders;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public class ShiftDvrpFleetQsimModule extends AbstractDvrpModeQSimModule {

	public ShiftDvrpFleetQsimModule(String mode) {
		super(mode);
	}

	@Override
	public void configureQSim() {
		bindModal(Fleet.class).toProvider(new ModalProviders.AbstractProvider<>(getMode(), DvrpModes::mode) {

			@Override
			public Fleet get() {
				FleetSpecification fleetSpecification = getModalInstance(FleetSpecification.class);
				Network network = getModalInstance(Network.class);
				return Fleets.createCustomFleet(fleetSpecification,
						s -> new DefaultShiftDvrpVehicle(new DvrpVehicleImpl(s, network.getLinks().get(s.getStartLinkId()))));

			}
		}).asEagerSingleton();
	}
}
