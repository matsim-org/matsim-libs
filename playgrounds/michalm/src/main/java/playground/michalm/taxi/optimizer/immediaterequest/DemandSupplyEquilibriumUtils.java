package playground.michalm.taxi.optimizer.immediaterequest;

import java.util.Collection;

import playground.michalm.taxi.model.TaxiRequest;


public class DemandSupplyEquilibriumUtils
{
    public static int countAwaitingUnplannedRequests(Collection<TaxiRequest> unplannedRequests,
            double now)
    {
        //count unplannedRequests such that req.T0 < now
        //not: req.T0 <= now,  because optimization is always performed BEFORE time step
        int waitingUnplannedRequestsCount = 0;

        for (TaxiRequest r : unplannedRequests) {
            if (r.getT0() < now) {
                waitingUnplannedRequestsCount++;
            }
        }

        return waitingUnplannedRequestsCount;
    }
}
