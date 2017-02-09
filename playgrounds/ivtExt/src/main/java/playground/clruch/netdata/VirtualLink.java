package playground.clruch.netdata;

/**
 * Created by Claudio on 2/8/2017.
 */
public class VirtualLink {
    // only used for debugging
    final private String id;
    final private VirtualNode from;
    final private VirtualNode to;

    VirtualLink(String idIn, VirtualNode fromIn, VirtualNode toIn){
        this.id = idIn;
        this.from = fromIn;
        this.to = toIn;
    }

    public String getId(){
        return this.id;
    }

    public VirtualNode getFrom(){
        return  this.from;
    }

    public VirtualNode getTo(){
        return  this.to;
    }
}
