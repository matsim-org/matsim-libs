package org.matsim.contrib.carsharing.relocation.utils;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.carsharing.manager.demand.DemandHandler;
import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.CompanyContainer;
import org.matsim.contrib.carsharing.manager.supply.FreeFloatingVehiclesContainer;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

public class FFVehiclesRentalsWriterListener implements IterationEndsListener {
	int frequency = 0;

	@Inject private CarsharingSupplyInterface carsharingSupply;

	@Inject private DemandHandler demandHandler;

	@Inject private OutputDirectoryHierarchy outputDirectoryHierarchy;

	public FFVehiclesRentalsWriterListener(int frequency) {
		this.frequency = frequency;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (event.getIteration() % this.frequency == 0) {
			FFVehiclesRentalsWriter writer = new FFVehiclesRentalsWriter();
			String filename = this.outputDirectoryHierarchy.getIterationFilename(event.getIteration(), "ff-vehicle-rentals.xml");

			for (String companyId : this.carsharingSupply.getCompanyNames()) {
				CompanyContainer companyContainer = this.carsharingSupply.getCompany(companyId);
				FreeFloatingVehiclesContainer freeFloatingContainer = (FreeFloatingVehiclesContainer) companyContainer.getVehicleContainer("freefloating");

				writer.addCompanyId(companyId);

				for (CSVehicle vehicle : freeFloatingContainer.getFfvehicleIdMap().values()) {
					Id<Vehicle> vehicleId = Id.create(vehicle.getVehicleId(), Vehicle.class);
					ArrayList<RentalInfo> rentals = new ArrayList<RentalInfo>();

					if (this.demandHandler.getVehicleRentalsMap().containsKey(vehicleId)) {

						for (RentalInfo info : this.demandHandler.getVehicleRentalsMap().get(vehicleId).getRentals()) {
							rentals.add(info);
						}
					}

					writer.addVehicleRentals(companyId, vehicleId, rentals);
				}
			}

			writer.writeFile(filename);
		}
	}
}
