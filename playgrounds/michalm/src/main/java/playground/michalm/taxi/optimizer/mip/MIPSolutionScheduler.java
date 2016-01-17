package playground.michalm.taxi.optimizer.mip;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.dvrp.router.DijkstraWithThinPath;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.router.util.LeastCostPathCalculator;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.mip.MIPProblem.MIPSolution;


class MIPSolutionScheduler
{
    private final TaxiOptimizerContext optimContext;
    private final MIPRequestData rData;
    private final VehicleData vData;
    private final int m;
    private final int n;

    private LeastCostPathCalculator router;

    private MIPSolution solution;
    private Vehicle currentVeh;


    MIPSolutionScheduler(TaxiOptimizerContext optimContext, MIPRequestData rData,
            VehicleData vData)
    {
        this.optimContext = optimContext;
        this.rData = rData;
        this.vData = vData;
        this.m = vData.dimension;
        this.n = rData.dimension;
        
        router = new DijkstraWithThinPath(optimContext.context.getScenario().getNetwork(),
                optimContext.travelDisutility, optimContext.travelTime);
    }


    void updateSchedules(MIPSolution solution)
    {
        this.solution = solution;

        for (int k = 0; k < m; k++) {
            currentVeh = vData.entries.get(k).vehicle;
            appendSubsequentRequestsToCurrentVehicle(k);
        }
    }


    private void appendSubsequentRequestsToCurrentVehicle(int u)
    {
        boolean[] x_u = solution.x[u];
        for (int i = 0; i < n; i++) {
            if (x_u[m + i]) {
                appendRequestToCurrentVehicle(i);
                appendSubsequentRequestsToCurrentVehicle(m + i);
                return;
            }
        }
    }


    private void appendRequestToCurrentVehicle(int i)
    {
        LinkTimePair earliestDeparture = optimContext.scheduler.getEarliestIdleness(currentVeh);
        TaxiRequest req = rData.requests[i];

        //use earliestDeparture.time instead of w[i]-tt (latest departure time) due to:
        // - possible inaccuracy of the optimization results
        //   (if x[.][m+i] = 0.9999 then w[i] < earliestDeparture.time may occur 
        // - we want to dispatch vehicles as soon as possible
        //   (because tt in MIP are based on the free flow speed estimates, while the actual
        //   times are usually longer hence the vehicle is likely to arrive after w[i])
        VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(earliestDeparture.link,
                req.getFromLink(), earliestDeparture.time, router, optimContext.travelTime);

        optimContext.scheduler.scheduleRequest(currentVeh, req, path);
    }
}
