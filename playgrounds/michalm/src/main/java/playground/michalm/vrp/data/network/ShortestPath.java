package playground.michalm.vrp.data.network;

import org.matsim.api.core.v01.*;


public interface ShortestPath
{
    //optimization
    public static final SPEntry ZERO_PATH_ENTRY = new SPEntry(0, 0, new Id[0]);
    
    
    // include toLink or fromLink in time/cost (depends on the way the qsim is implemented...)
    // by default: true (toLinks are included)
    public final static boolean INCLUDE_TO_LINK = true;


    public static class SPEntry
    {
        public final int travelTime;
        public final double travelCost;
        public final Id[] linkIds;


        public SPEntry(int travelTime, double travelCost, Id[] linkIds)
        {
            this.travelTime = travelTime;
            this.travelCost = travelCost;
            this.linkIds = linkIds;
        }
    }


    SPEntry getSPEntry(int departTime);
}
