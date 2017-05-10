package playground.clruch.netdata;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import playground.clruch.utils.GlobalAssert;

/**
 * Created by Claudio on 2/8/2017.
 */
// TODO VirtualNode implements Comparable<VirtualNode> based on index
public class VirtualNode implements Serializable, Comparable<VirtualNode> {
    /**
     * index is counting from 0,1,... index is used to assign entries in vectors and matrices
     */
    public final int index;
    /** id is only used for debugging */
    private final String id;
    // TODO how to make links final again?
    private transient Set<Link> links;
    private final Set<String> linkIDsforSerialization;
    private final int neighCount;
    private final Coord coord;

    VirtualNode(int index, String idIn, Set<Link> linksIn, int neighCount, Coord coordIn) {
        this.index = index;
        this.id = idIn;
        this.links = linksIn;
        this.neighCount = neighCount;
        this.coord = coordIn;
        this.linkIDsforSerialization = linksIn.stream().map(v -> v.getId().toString()).collect(Collectors.toCollection(HashSet::new));

        // TODO remove check
        // EVTL GET RID OF THIS -> LEFTOVER NODE or deal differently with it or test if last idx is
        // not leftOver-> problem & fill last one in!!sth like this... TODO
        if (!idIn.contains("" + (index + 1)))
            throw new RuntimeException("node index mismatch:" + idIn + " != " + (index + 1));
    }

    VirtualNode(int index, String idIn, int neighCount, Coord coordIn) {
        this(index, idIn, new LinkedHashSet<>(), neighCount, coordIn);
    }

    public void setLinks(Set<Link> linksIn) {
        GlobalAssert.that(this.links.size() == 0);
        for (Link link : linksIn) {
            this.links.add(link);
            this.linkIDsforSerialization.add(link.getId().toString());
        }
        GlobalAssert.that(links.size() == linkIDsforSerialization.size());
    }

    /**
     * Function initializes the Set<Link> links with references to the simulation network after it was loaded from a serialized bitmap
     * @param network
     */
    public void setLinksAfterSerialization(Network network) {
        this.links = new HashSet<>();
        for (String linkIDString : linkIDsforSerialization) {            
            Id<Link> linkID = Id.createLinkId(linkIDString);
            Link link = network.getLinks().get(linkID);
            GlobalAssert.that(link!=null);
            links.add(link);
        }

    }

    public Set<Link> getLinks() {
        return Collections.unmodifiableSet(links);
    }

    public String getId() {
        return id;
    }

    public Coord getCoord() {
        return coord;
    }

    public int getIndex() {
        return index;
    }

    public int getNeighCount() {
        return neighCount;
    }

    @Override
    public int compareTo(VirtualNode virtualNode) {
        return Integer.compare(index, virtualNode.index);
    }
}
