package org.matsim.contrib.drt.extension.operations.shifts.run;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.DefaultShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.EvShiftDvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.Fleets;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.core.modal.ModalProviders;
import org.matsim.vehicles.Vehicle;

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

			@Inject
			private ElectricFleet evFleet;

			@Override
			public Fleet get() {
				FleetSpecification fleetSpecification = getModalInstance(FleetSpecification.class);
				Network network = getModalInstance(Network.class);

				return Fleets.createCustomFleet(fleetSpecification,
                        s -> {
                            DefaultShiftDvrpVehicle shiftDvrpVehicle =
                                    new DefaultShiftDvrpVehicle(new DvrpVehicleImpl(s, network.getLinks().get(s.getStartLinkId())));

                            if (evFleet != null) {
                                Id<Vehicle> id = Id.create(s.getId(), Vehicle.class);
                                if (evFleet.getElectricVehicles().containsKey(id)) {
                                    return new EvShiftDvrpVehicle(shiftDvrpVehicle, evFleet.getElectricVehicles().get(id));
                                }
                            }
                            return shiftDvrpVehicle;
                        });
			}
		}).asEagerSingleton();
	}
}
