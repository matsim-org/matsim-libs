// code by clruch
package playground.clruch.netdata;

import java.io.Serializable;

/** Created by Claudio on 2/8/2017. */
public class VirtualLink implements Serializable {
    /** index is counting from 0,1,...
     * index is used to assign entries in vectors and matrices */
    private final int index;
    private final String id;
    private final VirtualNode from;
    private final VirtualNode to;
    private final double distance;

    VirtualLink(int index, String id, VirtualNode from, VirtualNode to, double distance) {
        this.index = index;
        this.id = id;
        this.from = from;
        this.to = to;
        this.distance = distance;
    }

    public String getId() {
        return id;
    }

    public VirtualNode getFrom() {
        return from;
    }

    public VirtualNode getTo() {
        return to;
    }

    public double getDistance() {
        return distance;
    }

    public int getIndex() {
        return index;
    }
}
