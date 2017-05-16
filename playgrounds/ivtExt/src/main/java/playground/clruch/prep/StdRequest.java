package playground.clruch.prep;

import org.matsim.api.core.v01.network.Link;

public class StdRequest {
    public double departureTime;
    public final Link from;
    public Link to;

    public StdRequest(Link from) {
        this.from = from;
    }
}
