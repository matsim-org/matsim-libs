package org.matsim.contrib.shared_mobility.analysis;

import java.util.stream.Stream;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.shared_mobility.service.SharingService;
import org.matsim.vehicles.Vehicle;

/**
 * @author steffenaxer
 */
public class SharingLegBuilder {
	Id<Person> personId;
	double departureTime;
	double arrivalTime;
	Id<SharingService> sharingServiceId;
	double distance = 0;
	Id<Vehicle> vehicleId;
	Coord fromCoord;
	Coord toCoord;


	public SharingLegBuilder setFromCoord(Coord fromCoord) {
		this.fromCoord = fromCoord;
		return this;
	}

	public SharingLegBuilder setToCoord(Coord toCoord) {
		this.toCoord = toCoord;
		return this;
	}

	public void setPersonId(Id<Person> personId) {
		this.personId = personId;
	}

	public SharingLegBuilder setDepartureTime(double departureTime) {
		this.departureTime = departureTime;
		return this;
	}

	public SharingLegBuilder setVehicleId(Id<Vehicle> vehicleId) {
		this.vehicleId = vehicleId;
		return this;
	}

	public SharingLegBuilder setArrivalTime(double arrivalTime) {
		this.arrivalTime = arrivalTime;
		return this;
	}

	public SharingLegBuilder setSharingServiceId(Id<SharingService> sharingServiceId) {
		this.sharingServiceId = sharingServiceId;
		return this;
	}

	public SharingLegBuilder addDistance(double addedDistance) {
		this.distance += addedDistance;
		return this;
	}

	public SharingLeg build() {
		boolean valid = isValid();

		if (valid) {
			return new SharingLeg(personId, departureTime, arrivalTime, sharingServiceId, distance, vehicleId, fromCoord, toCoord);
		}
		return null;
	}

	private boolean isValid() {
		boolean valid = false;

		// Those objects are not allowed to be null
		if (Stream.of(personId, departureTime, arrivalTime, sharingServiceId, distance, vehicleId, fromCoord, toCoord)
				.allMatch(x -> x != null)) {
			valid = true;
		}

		if (arrivalTime < departureTime) {
				throw new IllegalStateException("arrivalTime < departureTime");
		}

		return valid;
	}

}
