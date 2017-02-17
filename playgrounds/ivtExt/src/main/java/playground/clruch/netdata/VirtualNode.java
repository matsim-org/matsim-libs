package playground.clruch.netdata;

import java.util.Collections;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;

/**
 * Created by Claudio on 2/8/2017.
 */
public class VirtualNode {
    /** id is only used for debugging */
    private final String id;
    private final Set<Link> links;

    VirtualNode(String idIn, Set<Link> linksIn) {
        this.id = idIn;
        this.links = linksIn;
    }

    public Set<Link> getLinks() {
        return Collections.unmodifiableSet(links);
    }

    public String getId() {
        return id;
    }
}
