package playground.sebhoerl.av.logic.service;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import playground.sebhoerl.av.utils.UncachedId;

public class Request {
    final private UncachedId id;
    
    private double pickupTime;
    private double submissionTime;
    
    private Id<Link> pickupLinkId;
    private Id<Link> dropoffLinkId;
    
    private Id<Person> passengerId;
    
    /**
     * Creates a AV request instance. Should always be done through AVRequestFactory in order to 
     * have a unique id.
     */
    public Request(UncachedId id, Id<Person> passengerId, double pickupTime, Id<Link> pickupLinkId, Id<Link> dropoffLinkId) {
        this.id = id;
        this.pickupTime = pickupTime;
        this.pickupLinkId = pickupLinkId;
        this.dropoffLinkId = dropoffLinkId;
        this.passengerId = passengerId;
    }

    public double getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(double pickupTime) {
        this.pickupTime = pickupTime;
    }

    public Id<Link> getPickupLinkId() {
        return pickupLinkId;
    }

    public void setPickupLinkId(Id<Link> pickupLinkId) {
        this.pickupLinkId = pickupLinkId;
    }

    public Id<Link> getDropoffLinkId() {
        return dropoffLinkId;
    }

    public void setDropoffLinkId(Id<Link> dropoffLinkId) {
        this.dropoffLinkId = dropoffLinkId;
    }

    public Id<Person> getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(Id<Person> passengerId) {
        this.passengerId = passengerId;
    }

    public UncachedId getId() {
        return id;
    }
    
    public boolean equals(Object object) {
        if (object instanceof Request) {
            return ((Request) object).getId().equals(id);
        }
        
        return false;
    }

    public double getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime(double submissionTime) {
        this.submissionTime = submissionTime;
    }
}
