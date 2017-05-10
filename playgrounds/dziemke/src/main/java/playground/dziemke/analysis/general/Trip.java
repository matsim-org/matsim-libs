package playground.dziemke.analysis.general;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 * @author gthunig on 04.04.2017.
 */
public class Trip {

    private Id<Person> personId;
    private Id<Trip> tripId;
    private double weight = 1;
    private String activityTypeBeforeTrip;
    private double departureTime_s;
    private double arrivalTime_s;
    private String legMode;
    private double distanceBeeline_m;
    private double distanceRouted_m;
    private double duration_s;
    private String activityTypeAfterTrip;

    //getters and setters
    public Id<Person> getPersonId() {
        return personId;
    }

    public void setPersonId(Id<Person> personId) {
        this.personId = personId;
    }

    public Id<Trip> getTripId() {
        return tripId;
    }

    public void setTripId(Id<Trip> tripId) {
        this.tripId = tripId;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String getActivityTypeBeforeTrip() {
        return activityTypeBeforeTrip;
    }

    public void setActivityTypeBeforeTrip(String activityTypeBeforeTrip) {
        this.activityTypeBeforeTrip = activityTypeBeforeTrip;
    }

    public double getDepartureTime_s() {
        return departureTime_s;
    }

    public void setDepartureTime_s(double departureTime_s) {
        this.departureTime_s = departureTime_s;
    }

    public double getArrivalTime_s() {
        return arrivalTime_s;
    }

    public void setArrivalTime_s(double arrivalTime_s) {
        this.arrivalTime_s = arrivalTime_s;
    }

    public String getLegMode() {
        return legMode;
    }

    public void setLegMode(String legMode) {
        this.legMode = legMode;
    }

    public double getDistanceBeeline_m() {
        return distanceBeeline_m;
    }

    public void setDistanceBeeline_m(double distanceBeeline_m) {
        this.distanceBeeline_m = distanceBeeline_m;
    }

    public double getDistanceRouted_m() {
        return distanceRouted_m;
    }

    public void setDistanceRouted_m(double distanceRouted_m) {
        this.distanceRouted_m = distanceRouted_m;
    }

    public double getDuration_s() {
        return duration_s;
    }

    public void setDuration_s(double duration_s) {
        this.duration_s = duration_s;
    }

    public String getActivityTypeAfterTrip() {
        return activityTypeAfterTrip;
    }

    public void setActivityTypeAfterTrip(String activityTypeAfterTrip) {
        this.activityTypeAfterTrip = activityTypeAfterTrip;
    }



}
