package playground.jbischoff.taxibus.vehreqpath;

import java.util.*;

import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.util.*;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.collections.Tuple;

import playground.jbischoff.taxibus.optimizer.TaxibusOptimizerConfiguration;
import playground.jbischoff.taxibus.passenger.TaxibusRequest;
import playground.jbischoff.taxibus.passenger.TaxibusRequest.TaxibusRequestStatus;
import playground.jbischoff.taxibus.scheduler.TaxibusScheduler;
import playground.jbischoff.taxibus.utils.TaxibusUtils;


public class TaxibusVehicleRequestPathFinder
{
    private final TaxibusOptimizerConfiguration optimConfig;
    private final TaxibusScheduler scheduler;
    private final LeastCostPathCalculatorWithCache routerWithCache;


    public TaxibusVehicleRequestPathFinder(TaxibusOptimizerConfiguration optimConfig)
    {
        this.optimConfig = optimConfig;
        this.scheduler = optimConfig.scheduler;

        LeastCostPathCalculator router = new Dijkstra(
                optimConfig.context.getScenario().getNetwork(), optimConfig.travelDisutility,
                optimConfig.travelTime);

        routerWithCache = new DefaultLeastCostPathCalculatorWithCache(router,
                TimeDiscretizer.ACYCLIC_15_MIN);
    }


    public TaxibusVehicleRequestPath findBestVehicleForRequest(TaxibusRequest req,
            Iterable<? extends Vehicle> vehicles, TaxibusVehicleRequestPathCost vrpCost)
    {
        TaxibusVehicleRequestPath bestVrp = null;
        double bestCost = Double.MAX_VALUE;

        for (Vehicle veh : vehicles) {
            VrpPathWithTravelData path = calculateVrpPath(veh, req);
            if (path == null) {
                continue;
            }

            TaxibusVehicleRequestPath vrp = new TaxibusVehicleRequestPath(veh, req, path);
            double cost = vrpCost.getCost(vrp);
            if (cost < bestCost) {
                bestVrp = vrp;
                bestCost = cost;
            }
        }
        if(bestVrp!= null) bestVrp.path.add(calculateVrpPath(req, bestVrp.path.get(0).getArrivalTime()));
        return bestVrp;
    }


    private VrpPathWithTravelData calculateVrpPath(Vehicle veh, TaxibusRequest req)
    {
        LinkTimePair departure = scheduler.getImmediateDiversionOrEarliestIdleness(veh);
        return departure == null ? //
                null : VrpPaths.calcAndCreatePath(departure.link, req.getFromLink(), departure.time,
                        routerWithCache, optimConfig.travelTime);
    }
    
    private VrpPathWithTravelData calculateVrpPath(TaxibusRequest req, double start)
    {
        
    	return VrpPaths.calcAndCreatePath(req.getFromLink(), req.getToLink(), start,
                        routerWithCache, optimConfig.travelTime);
    }


    public TaxibusVehicleRequestPath findBestAdditionalVehicleForRequestPath(
            TaxibusVehicleRequestPath best, Iterable<TaxibusRequest> filteredReqs)
    {
        int onBoard = best.requests.size();
        double cap = best.vehicle.getCapacity();
        if (onBoard >= cap)
            return null;

        double bestCost = Double.MAX_VALUE;
        TaxibusRequest bestNextRequest = null;
        ArrayList<VrpPathWithTravelData> bestNewPath = null;

        for (TaxibusRequest request : filteredReqs) {
            if (best.requests.contains(request))
                continue;
            if (request.getStatus() != TaxibusRequestStatus.UNPLANNED)
                continue;

            ArrayList<VrpPathWithTravelData> tentativeNewPath = calculateVrpPaths(best, request);
            double currentCost = TaxibusUtils.calcPathCost(tentativeNewPath);
            if (currentCost < bestCost) {
                bestCost = currentCost;
                bestNextRequest = request;
                bestNewPath = tentativeNewPath;
            }
        }
        if ( (bestNextRequest != null) && (bestNewPath != null)) {
            Set<TaxibusRequest> newRequests = best.requests;
            newRequests.add(bestNextRequest);
            TaxibusVehicleRequestPath result = new TaxibusVehicleRequestPath(best.vehicle,
                    newRequests, bestNewPath);
            if (detourIsAcceptable(result, best.path)) {
                return result;
            }
        }

        return null;
    }


    private boolean detourIsAcceptable(TaxibusVehicleRequestPath result,
            ArrayList<VrpPathWithTravelData> path)
    {
        // TODO Auto-generated method stub
        return true;
    }


    private ArrayList<VrpPathWithTravelData> calculateVrpPaths(TaxibusVehicleRequestPath best,
            TaxibusRequest request)
    {
        TreeSet<TaxibusRequest> allRequests = new TreeSet<TaxibusRequest>(
                Requests.ABSOLUTE_COMPARATOR);
        allRequests.addAll(best.requests);

        if (!allRequests.add(request)) {
            throw new IllegalStateException();
        }
        ArrayList<VrpPathWithTravelData> segments = new ArrayList<>();
        Iterator<TaxibusRequest> iterator = allRequests.iterator();
        VrpPathWithTravelData currentSegment = VrpPaths.calcAndCreatePath(
                best.path.get(0).getFromLink(), iterator.next().getFromLink(),
                best.path.get(0).getDepartureTime(), routerWithCache, optimConfig.travelTime);
        segments.add(currentSegment);
        //Pickups
        while (iterator.hasNext()) {
            VrpPathWithTravelData nextSegment = VrpPaths.calcAndCreatePath(
                    currentSegment.getToLink(), iterator.next().getFromLink(),
                    currentSegment.getArrivalTime(), routerWithCache, optimConfig.travelTime);
            segments.add(nextSegment);
            currentSegment = nextSegment;
        }

        //Dropoffs
        while (!allRequests.isEmpty()) {
            Tuple<VrpPathWithTravelData, TaxibusRequest> nextTuple = getNextDropoffSegment(
                    allRequests, currentSegment);
            segments.add(nextTuple.getFirst());
            currentSegment = nextTuple.getFirst();
            allRequests.remove(nextTuple.getSecond());
        }

        return segments;
    }


    private  Tuple<VrpPathWithTravelData, TaxibusRequest> getNextDropoffSegment(
            TreeSet<TaxibusRequest> allRequests, VrpPathWithTravelData currentSegment)
    {

        double bestTime = Double.MAX_VALUE;
        Tuple<VrpPathWithTravelData, TaxibusRequest> bestSegment = null;
        for (TaxibusRequest request : allRequests) {
            VrpPathWithTravelData segment = VrpPaths.calcAndCreatePath(currentSegment.getToLink(),
                    request.getToLink(), currentSegment.getDepartureTime(), routerWithCache,
                    optimConfig.travelTime);
            if (segment.getTravelTime() < bestTime) {
                bestTime = segment.getTravelTime();
                bestSegment = new Tuple<>(segment, request);

            }
        }

        return bestSegment;
    }
}
