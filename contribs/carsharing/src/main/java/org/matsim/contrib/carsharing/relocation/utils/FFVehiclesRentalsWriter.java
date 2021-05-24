package org.matsim.contrib.carsharing.relocation.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

public class FFVehiclesRentalsWriter extends MatsimXmlWriter {
	protected Map<String, Map<Id<Vehicle>, ArrayList<RentalInfo>>> rentals;

	public FFVehiclesRentalsWriter() {
		this.rentals = new HashMap<String, Map<Id<Vehicle>, ArrayList<RentalInfo>>>();
	}

	public void addCompanyId(String companyId) {
		if (!this.rentals.keySet().contains(companyId)) {
			this.rentals.put(companyId, new TreeMap<Id<Vehicle>, ArrayList<RentalInfo>>());
		}
	}

	public void addVehicleRentals(String companyId, Id<Vehicle> vehicleId, ArrayList<RentalInfo> rentals) {
		this.addCompanyId(companyId);

		this.rentals.get(companyId).put(vehicleId, rentals);
	}

	public void writeFile(final String filename) {
		this.openFile(filename);
		this.writeStartTag("vehicles", Collections.<Tuple<String, String>>emptyList());

		for (Entry<String, Map<Id<Vehicle>, ArrayList<RentalInfo>>> companyVehicleRentals : this.rentals.entrySet()) {
			this.writeStartTag("company", Arrays.asList(createTuple("id", companyVehicleRentals.getKey())));

			for (Entry<Id<Vehicle>, ArrayList<RentalInfo>> vehicleRentals : companyVehicleRentals.getValue().entrySet()) {
				this.writeStartTag("vehicle", Arrays.asList(createTuple("id", vehicleRentals.getKey().toString())));

				for (RentalInfo info : vehicleRentals.getValue()) {
					this.writeStartTag("rental", Arrays.asList(
							createTuple("start_time", Time.writeTime(info.getStartTime())),
							createTuple("end_time", Time.writeTime(info.getEndTime())),
							createTuple("start_link", info.getPickupLinkId().toString()),
							createTuple("end_link", info.getEndLinkId().toString())
					), true);
				}

				this.writeEndTag("vehicle");
			}

			this.writeEndTag("company");
		}

		this.writeEndTag("vehicles");
		this.close();
	}
}
