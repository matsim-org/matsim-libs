package playground.sebhoerl.av.logic.service;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import playground.sebhoerl.av.utils.UncachedId;

public class RequestFactory {
    private long index = 0;
    
    private UncachedId createId(Id<Person> passengerId, Id<Link> pickupLinkId, Id<Link> dropoffLinkId) {
        return new UncachedId(String.format("%s:%s:%s:%d", 
                passengerId.toString(), 
                pickupLinkId.toString(), 
                dropoffLinkId.toString(),
                index++));
    }
    
    public synchronized Request createRequest(Id<Person> passengerId, double pickupTime, Id<Link> pickupLinkId, Id<Link> dropoffLinkId) {
        return new Request(
                createId(passengerId, pickupLinkId, dropoffLinkId), 
                passengerId, pickupTime, 
                pickupLinkId, dropoffLinkId);
    }
}
