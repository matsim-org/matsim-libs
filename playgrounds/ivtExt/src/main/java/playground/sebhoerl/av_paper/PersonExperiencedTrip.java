package playground.sebhoerl.av_paper;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;

public class PersonExperiencedTrip {
    private String mode;
    private Double distance;
    private Double startTime;
    private Double endTime;
    private Id<Link> startLink;
    private Id<Link> endLink;
    private Id<Person> person;
    
    private Double walkTime = 0.0;
    private Double walkDistance = 0.0;
    
    public PersonExperiencedTrip() {}
    
    static public PersonExperiencedTrip create(Id<Person> agentId, LinkedList<Leg> legs) {
    	PersonExperiencedTrip trip = new PersonExperiencedTrip();
        
        Leg first = legs.getFirst();
        Leg last = legs.getLast();
        
        if (first.getMode().equals("transit_walk")) {
            trip.setMode("pt");
        } else {
            trip.setMode(first.getMode());
        }
        
        double distance = 0.0;
        
        for (Leg leg : legs) {
            distance += leg.getRoute().getDistance();
            
            if (leg.getMode().equals("transit_walk")) {
                trip.setWalkDistance(trip.getWalkDistance() + leg.getRoute().getDistance());
                trip.setWalkTime(trip.getWalkTime() + leg.getTravelTime());
            }
        }
        
        trip.setDistance(distance);
        
        trip.setStartTime(first.getDepartureTime());
        trip.setEndTime(last.getDepartureTime() + last.getTravelTime());
        
        trip.setPerson(agentId);
        
        trip.setStartLink(first.getRoute().getStartLinkId());
        trip.setEndLink(last.getRoute().getEndLinkId());
        
        return trip;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public Double getStartTime() {
        return startTime;
    }

    public void setStartTime(Double startTime) {
        this.startTime = startTime;
    }

    public Double getEndTime() {
        return endTime;
    }

    public void setEndTime(Double endTime) {
        this.endTime = endTime;
    }

    public Id<Link> getStartLink() {
        return startLink;
    }

    public void setStartLink(Id<Link> startLink) {
        this.startLink = startLink;
    }

    public Id<Link> getEndLink() {
        return endLink;
    }

    public void setEndLink(Id<Link> endLink) {
        this.endLink = endLink;
    }

    public Id<Person> getPerson() {
        return person;
    }

    public void setPerson(Id<Person> person) {
        this.person = person;
    }

    public Double getWalkDistance() {
        return walkDistance;
    }

    public void setWalkDistance(Double walkDistance) {
        this.walkDistance = walkDistance;
    }

    public Double getWalkTime() {
        return walkTime;
    }

    public void setWalkTime(Double walkTime) {
        this.walkTime = walkTime;
    }
}
