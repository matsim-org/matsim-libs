// code by clruch
package playground.clruch.netdata;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.Tensor;

/** Created by Claudio on 2/8/2017. */
public class VirtualNode<T> implements Serializable {
    /** index is counting from 0,1,... index is used to assign entries in vectors and matrices */
    private final int index;
    /** id is only used for debugging */
    private final String id;
    private transient Set<T> links;
    private final Set<String> linkIDsforSerialization;
    private final int neighCount;
    private final Tensor coord;

    VirtualNode(int index, String id, Map<String, T> links, int neighCount, Tensor coord) {
        this.index = index;
        this.id = id;
        this.links = new HashSet<T>();
        links.values().stream().forEach(l -> this.links.add(l));
        this.neighCount = neighCount;
        this.coord = coord;
        this.linkIDsforSerialization = new HashSet<String>();
        links.keySet().stream().forEach(s -> this.linkIDsforSerialization.add(s));
        GlobalAssert.that(links.size() == linkIDsforSerialization.size());
    }

    /* package */ void setLinks(Map<String,T> links) {
        GlobalAssert.that(this.links != null);
        GlobalAssert.that(this.links.size() == 0);
        for(Entry<String,T> entry : links.entrySet()){
            this.links.add(entry.getValue());
            this.linkIDsforSerialization.add(entry.getKey());
        }        
        GlobalAssert.that(links.size() == linkIDsforSerialization.size());
    }

    /* package */ void setLinksAfterSerialization2(Map<String, T> map) {
        this.links = new HashSet<>();
        for (String linkIDString : linkIDsforSerialization) {
            T link = map.get(linkIDString);
            GlobalAssert.that(link != null);
            links.add(link);
        }
    }

    public Set<T> getLinks() {
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
