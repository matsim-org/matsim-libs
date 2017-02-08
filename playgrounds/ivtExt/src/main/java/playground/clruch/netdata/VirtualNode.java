package playground.clruch.netdata;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.Set;

/**
 * Created by Claudio on 2/8/2017.
 */
public class VirtualNode {
    // only used for debugging
    String id;
    Set<Id<Link>> linkIDs;
}
