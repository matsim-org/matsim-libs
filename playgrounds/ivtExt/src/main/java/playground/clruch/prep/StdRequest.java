// code by clruch
package playground.clruch.prep;

import org.matsim.api.core.v01.network.Link;

/* package */ class StdRequest {
    public double departureTime;
    public final Link ante;
    public Link post;

    public StdRequest(Link from) {
        this.ante = from;
    }
}
