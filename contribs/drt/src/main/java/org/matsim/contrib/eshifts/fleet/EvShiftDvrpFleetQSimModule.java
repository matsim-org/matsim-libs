package org.matsim.contrib.eshifts.fleet;

import com.google.inject.Inject;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.Fleets;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.contrib.eshifts.fleet.EvShiftDvrpVehicle;
import org.matsim.contrib.ev.fleet.ElectricFleet;

public class EvShiftDvrpFleetQSimModule extends AbstractDvrpModeQSimModule {

	public EvShiftDvrpFleetQSimModule(String mode) {
		super(mode);
	}

	@Override
	public void configureQSim() {
		bindModal(Fleet.class).toProvider(new ModalProviders.AbstractProvider<>(getMode()) {
			@Inject
			private ElectricFleet evFleet;

			@Override
			public Fleet get() {
				FleetSpecification fleetSpecification = getModalInstance(FleetSpecification.class);
				Network network = getModalInstance(Network.class);
				return Fleets.createCustomFleet(fleetSpecification,
						s -> EvShiftDvrpVehicle.create(new DvrpVehicleImpl(s, network.getLinks().get(s.getStartLinkId())),
								evFleet));

			}
		}).asEagerSingleton();
	}
}
