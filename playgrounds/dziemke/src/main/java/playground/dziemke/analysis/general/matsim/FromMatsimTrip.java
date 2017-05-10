package playground.dziemke.analysis.general.matsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import playground.dziemke.analysis.general.Trip;

import java.util.LinkedList;
import java.util.List;

/**
 * @author gthunig on 18.04.2017.
 */
public class FromMatsimTrip extends Trip {

    private Id<Link> departureLinkId;
    private List<Id<Link>> links = new LinkedList<>();
    private Id<Link> arrivalLinkId;

    //getters and setters
    Id<Link> getDepartureLinkId() {
        return departureLinkId;
    }

    void setDepartureLinkId(Id<Link> departureLinkId) {
        this.departureLinkId = departureLinkId;
    }

    List<Id<Link>> getLinks() {
        return links;
    }

    void setLinks(List<Id<Link>> links) {
        this.links = links;
    }

    Id<Link> getArrivalLinkId() {
        return arrivalLinkId;
    }

    void setArrivalLinkId(Id<Link> arrivalLinkId) {
        this.arrivalLinkId = arrivalLinkId;
    }
}
