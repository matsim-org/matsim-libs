package playground.clruch.netdata;

/**
 * Created by Claudio on 2/8/2017.
 */
public class VirtualLink {
    /**
     * index is counting from 0,1,...
     * index is used to assign entries in vectors and matrices
     */
    public final int index;
    // only used for debugging
    private final String id;
    private final VirtualNode from;
    private final VirtualNode to;

    VirtualLink(int index, String idIn, VirtualNode fromIn, VirtualNode toIn) {
        this.index = index;
        id = idIn;
        from = fromIn;
        to = toIn;
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

    public int getIndex() {return  index; }
}
