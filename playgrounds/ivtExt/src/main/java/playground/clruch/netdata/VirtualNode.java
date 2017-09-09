// code by clruch
package playground.clruch.netdata;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.Tensor;

/** Created by Claudio on 2/8/2017. */
public class VirtualNode implements Serializable {
    /** index is counting from 0,1,... index is used to assign entries in vectors and matrices */
    private final int index;
    /** id is only used for debugging */
    private final String id;
    private transient Set<Link> links;
    private final Set<String> linkIDsforSerialization;
    private final int neighCount;
    private final Tensor coord;

    VirtualNode(int index, String id, Set<Link> links, int neighCount, Tensor coord) {
        this.index = index;
        this.id = id;
        this.links = links;
        this.neighCount = neighCount;
        this.coord = coord;
        this.linkIDsforSerialization = links.stream().map(v -> v.getId().toString())//
                .collect(Collectors.toCollection(HashSet::new));
    }

    /* package */ void setLinks(Set<Link> links) {
        GlobalAssert.that(this.links != null);
        GlobalAssert.that(this.links.size() == 0);
        for (Link link : links) {
            this.links.add(link);
            this.linkIDsforSerialization.add(link.getId().toString());
        }
        GlobalAssert.that(links.size() == linkIDsforSerialization.size());
    }

    /* package */ void setLinksAfterSerialization2(Map<String, Link> map) {
        this.links = new HashSet<>();
        for (String linkIDString : linkIDsforSerialization) {
            Link link = map.get(linkIDString);
            GlobalAssert.that(link != null);
            links.add(link);
        }
    }

    public Set<Link> getLinks() {
        return Collections.unmodifiableSet(links);
    }

    public String getId() {
        return id;
    }

    public Tensor getCoord() {
        return coord;
    }

    public int getIndex() {
        return index;
    }

    public int getNeighCount() {
        return neighCount;
    }

}
