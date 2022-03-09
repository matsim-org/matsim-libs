package org.matsim.contrib.shared_mobility.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.shared_mobility.service.SharingService;
import org.matsim.vehicles.Vehicle;

/**
 * @author steffenaxer
 */
public class SharingLeg {
    Id<Person> personId;
    double departureTime;
    double arrivalTime;
    Id<SharingService> sharingServiceId;
    double distance;
    Id<Vehicle> vehicleId;
    Coord fromCoord;
    Coord toCoord;

    public SharingLeg(Id<Person> personId, double departureTime, double arrivalTime,
                      Id<SharingService> sharingServiceId, double distance, Id<Vehicle> vehicleId, Coord fromCoord, Coord toCoord) {

        this.personId = personId;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.sharingServiceId = sharingServiceId;
        this.distance = distance;
        this.vehicleId = vehicleId;
        this.fromCoord = fromCoord;
        this.toCoord = toCoord;
    }

    public Id<Vehicle> getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Id<Vehicle> vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Id<Person> getPersonId() {
        return personId;
    }

    public void setPersonId(Id<Person> personId) {
        this.personId = personId;
    }

    public double getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(double departureTime) {
        this.departureTime = departureTime;
    }

    public double getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(double arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public Id<SharingService> getSharingServiceId() {
        return sharingServiceId;
    }

    public void setSharingServiceId(Id<SharingService> sharingServiceId) {
        this.sharingServiceId = sharingServiceId;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public Coord getFromCoord() { return fromCoord; }

    public void setFromCoord(Coord fromCoord) { this.fromCoord = fromCoord; }

    public Coord getToCoord() { return toCoord; }

    public void setToCoord(Coord toCoord) { this.toCoord = toCoord; }
}
