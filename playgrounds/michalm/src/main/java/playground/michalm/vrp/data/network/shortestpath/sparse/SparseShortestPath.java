package playground.michalm.vrp.data.network.shortestpath.sparse;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.router.util.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.shortestpath.*;


public class SparseShortestPath
    implements ShortestPath
{
    private int timeInterval;
    private int intervalCoutn;
    private boolean cyclic;

    private SPEntry entries[];

    private TravelTime travelTime;
    private TravelDisutility travelCost;
    private LeastCostPathCalculator router;
    private Link fromLink;
    private Link toLink;


    public SparseShortestPath(int numIntervals, int timeInterval, boolean cyclic,
            LeastCostPathCalculator router, TravelTime travelTime, TravelDisutility travelCost,
            MATSimVertex fromVertex, MATSimVertex toVertex)
    {
        this.timeInterval = timeInterval;
        this.intervalCoutn = numIntervals;
        this.cyclic = cyclic;

        entries = new SPEntry[numIntervals];

        this.router = router;
        this.travelTime = travelTime;
        this.travelCost = travelCost;

        fromLink = fromVertex.getLink();
        toLink = toVertex.getLink();
    }


    private int getIdx(int departTime)
    {
        int idx = (departTime / timeInterval);
        return cyclic ? (idx % intervalCoutn) : idx;
    }


    @Override
    public SPEntry getSPEntry(int departTime)
    {
        int idx = getIdx(departTime);
        SPEntry entry = entries[idx];

        // loads necessary data on demand
        if (entry == null) {
            entry = entries[idx] = calculateSPEntry(departTime);
        }

        return entry;
    }


    private SPEntry calculateSPEntry(int departTime)
    {
        if (fromLink != toLink) {
            Path path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(),
                    departTime);

            double time = path.travelTime;
            double cost = path.travelCost;
            int idCount = path.links.size() + 1;
            Id[] ids = new Id[idCount];
            int idxShift;

            if (ShortestPath.INCLUDE_TO_LINK) {
                time += travelTime.getLinkTravelTime(toLink, departTime);
                cost += travelCost.getLinkTravelDisutility(toLink, time);
                ids[idCount - 1] = toLink.getId();
                idxShift = 0;
            }
            else {
                time += travelTime.getLinkTravelTime(fromLink, departTime);
                cost += travelCost.getLinkTravelDisutility(fromLink, time);
                ids[0] = fromLink.getId();
                idxShift = 1;
            }

            for (int idx = 0; idx < idCount - 1; idx++) {
                ids[idx + idxShift] = path.links.get(idx).getId();
            }

            return new SPEntry((int)time, cost, ids);
        }
        else {
            return ShortestPath.ZERO_PATH_ENTRY;
        }
    }
}
