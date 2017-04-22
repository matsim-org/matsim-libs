package playground.clruch.netdata;

import java.util.Collections;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

/**
 * Created by Claudio on 2/8/2017.
 */
public class VirtualNode {
    /**
     * index is counting from 0,1,...
     * index is used to assign entries in vectors and matrices
     */
    public final int index;
    /** id is only used for debugging */
    private final String id;
    private final Set<Link> links;
    private final int neighCount;
    private final Coord coord;

    VirtualNode(int index, String idIn, Set<Link> linksIn, int neighCount, Coord coordIn) {
        this.index = index;
        this.id = idIn;
        this.links = linksIn;
        this.neighCount = neighCount;
        this.coord = coordIn;

        //EVTL GET RID OF THIS -> LEFTOVER NODE or deal differently with it or test if last idx is not leftOver-> problem & fill last one in!!sth like this... TODO
        if (!idIn.contains("" + (index + 1)))
            throw new RuntimeException("node index mismatch:" + idIn + " != " + (index + 1));
    }

    public Set<Link> getLinks() {
        return Collections.unmodifiableSet(links);
    }

    public String getId() {
        return id;
    }
    
    public Coord getCoord(){
        return coord;
    }

    public int getIndex() {return  index; }

    public int getNeighCount() {return neighCount;}
}
