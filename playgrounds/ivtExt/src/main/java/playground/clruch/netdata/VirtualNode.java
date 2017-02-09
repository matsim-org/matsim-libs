package playground.clruch.netdata;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Claudio on 2/8/2017.
 */
public class VirtualNode {
    // only used for debugging
    private final String id;
    private final Set<Id<Link>> linkIDs;

    VirtualNode(String idIn, Set<Id<Link>> linkIDsIn) {
        this.id = idIn;
        this.linkIDs = linkIDsIn;
    }

    Set<Id<Link>> getLinkIDs() {
        return this.linkIDs;
    }

    String getId() {
        return this.id;
    }
}
