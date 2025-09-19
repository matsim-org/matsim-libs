package org.matsim.drtExperiments.basicStructures;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class GeneralRequest {
    private final Id<Person> passengerId;
    private final Id<Link> fromLinkId;
    private final Id<Link> toLinkId;
    private final double earliestDepartureTime;

    // latest departure time (flexibility is needed to account for traffic uncertainty)
    private double latestDepartureTime;
    // latest arrival time (flexibility is needed to account for traffic uncertainty)
    private double latestArrivalTime;

    public GeneralRequest(Id<Person> passengerId, Id<Link> fromLinkId, Id<Link> toLinkId, double earliestDepartureTime,
                          double latestStartTime, double latestArrivalTime) {
        this.passengerId = passengerId;
        this.fromLinkId = fromLinkId;
        this.toLinkId = toLinkId;
        this.earliestDepartureTime = earliestDepartureTime;
        this.latestDepartureTime = latestStartTime;
        this.latestArrivalTime = latestArrivalTime;
    }

    public Id<Person> getPassengerId() {
        return passengerId;
    }

    public double getEarliestDepartureTime() {
        return earliestDepartureTime;
    }

    public double getLatestArrivalTime() {
        return latestArrivalTime;
    }

    public double getLatestDepartureTime() {
        return latestDepartureTime;
    }

    public Id<Link> getFromLinkId() {
        return fromLinkId;
    }

    public Id<Link> getToLinkId() {
        return toLinkId;
    }

    public void setLatestArrivalTime(double latestArrivalTime) {
        this.latestArrivalTime = latestArrivalTime;
    }

    public void setLatestDepartureTime(double latestDepartureTime) {
        this.latestDepartureTime = latestDepartureTime;
    }
}

