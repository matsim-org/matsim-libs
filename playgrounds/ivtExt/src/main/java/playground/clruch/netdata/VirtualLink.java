package playground.clruch.netdata;

/**
 * Created by Claudio on 2/8/2017.
 */
public class VirtualLink {
    // only used for debugging
    private final String id;
    private final VirtualNode from;
    private final VirtualNode to;

    VirtualLink(String idIn, VirtualNode fromIn, VirtualNode toIn) {
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
}
