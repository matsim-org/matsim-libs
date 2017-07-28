// code by clruch
package playground.clruch.netdata;

import java.io.Serializable;

/**
 * Created by Claudio on 2/8/2017.
 */
public class VirtualLink implements Serializable {
    /**
     * index is counting from 0,1,...
     * index is used to assign entries in vectors and matrices
     */
    public final int index;
    // only used for debugging
    private final String id;
    private final VirtualNode from;
    private final VirtualNode to;
    private final double distance;

    VirtualLink(int index, String idIn, VirtualNode fromIn, VirtualNode toIn, double distance) {
        this.index = index;
        id = idIn;
        from = fromIn;
        to = toIn;
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
    
    public double getDistance(){
        return distance;
    }

    public int getIndex() {return  index; }
}
