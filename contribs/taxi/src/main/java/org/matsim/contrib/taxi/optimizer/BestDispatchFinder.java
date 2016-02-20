package org.matsim.contrib.taxi.optimizer;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.router.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;


public class BestDispatchFinder
{
    public static class Dispatch
    {
        public final Vehicle vehicle;
        public final TaxiRequest request;
        public final VrpPathWithTravelData path;


        public Dispatch(Vehicle vehicle, TaxiRequest request, VrpPathWithTravelData path)
        {
            this.vehicle = vehicle;
            this.request = request;
            this.path = path;
        }
    }


    private final TaxiOptimizerContext optimContext;
    private final MultiNodeDijkstra router;
    private final TaxiScheduler scheduler;


    public BestDispatchFinder(TaxiOptimizerContext optimContext)
    {
        this.optimContext = optimContext;
        this.scheduler = optimContext.scheduler;

        router = new MultiNodeDijkstra(optimContext.context.getScenario().getNetwork(),
                optimContext.travelDisutility, optimContext.travelTime, false);
    }


    //for immediate requests only
    //minimize TW
    public Dispatch findBestVehicleForRequest(TaxiRequest req, Iterable<? extends Vehicle> vehicles)
    {
        double currTime = optimContext.context.getTime();
        Link toLink = req.getFromLink();
        Node toNode = toLink.getFromNode();

        Map<Id<Node>, Vehicle> initialVehicles = new HashMap<>();
        Map<Id<Node>, InitialNode> initialNodes = new HashMap<>();
        for (Vehicle veh : vehicles) {
            LinkTimePair departure = scheduler.getImmediateDiversionOrEarliestIdleness(veh);
            if (departure != null) {

                Node vehNode;
                double delay = departure.time - currTime;
                if (departure.link == toLink) {
                    //hack: we are basically there (on the same link), so let's pretend vehNode == toNode
                    vehNode = toNode;
                }
                else {
                    vehNode = departure.link.getToNode();

                    //simplified, but works for taxis, since pickup trips are short (about 5 mins)
                    delay += 1 + toLink.getFreespeed(departure.time);
                }

                InitialNode existingInitialNode = initialNodes.get(vehNode.getId());
                if (existingInitialNode == null || existingInitialNode.initialCost > delay) {
                    InitialNode newInitialNode = new InitialNode(vehNode, delay, delay);
                    initialNodes.put(vehNode.getId(), newInitialNode);
                    initialVehicles.put(vehNode.getId(), veh);
                }
            }
        }

        if (initialNodes.isEmpty()) {
            return null;
        }

        ImaginaryNode fromNodes = router.createImaginaryNode(initialNodes.values());

        Path path = router.calcLeastCostPath(fromNodes, toNode, currTime, null, null);
        //the calculated path contains real nodes (no imaginary/initial nodes),
        //the time and cost are of real travel (between the first and last real node)
        //(no initial times/costs for imaginary<->initial are included)
        Node fromNode = path.nodes.get(0);
        Vehicle bestVehicle = initialVehicles.get(fromNode.getId());
        LinkTimePair bestDeparture = scheduler.getImmediateDiversionOrEarliestIdleness(bestVehicle);

        VrpPathWithTravelData vrpPath = VrpPaths.createPath(bestDeparture.link, toLink,
                bestDeparture.time, path, optimContext.travelTime);
        return new Dispatch(bestVehicle, req, vrpPath);
    }


    //for immediate requests only
    //minimize TP
    public Dispatch findBestRequestForVehicle(Vehicle veh, Iterable<TaxiRequest> unplannedRequests)
    {
        LinkTimePair departure = scheduler.getImmediateDiversionOrEarliestIdleness(veh);
        Node fromNode = departure.link.getToNode();

        Map<Id<Node>, TaxiRequest> initialRequests = new HashMap<>();
        Map<Id<Node>, InitialNode> initialNodes = new HashMap<>();
        for (TaxiRequest req : unplannedRequests) {
            Link reqLink = req.getFromLink();

            if (departure.link == reqLink) {
                VrpPathWithTravelData vrpPath = VrpPaths.createPath(departure.link, reqLink,
                        departure.time, null, optimContext.travelTime);
                return new Dispatch(veh, req, vrpPath);
            }

            Id<Node> reqNodeId = reqLink.getFromNode().getId();

            if (!initialNodes.containsKey(reqNodeId)) {
                //simplified, but works for taxis, since pickup trips are short (about 5 mins)
                double delayAtLastLink = reqLink.getFreespeed(departure.time);

                //works most fair (FIFO) if unplannedRequests are sorted by T0 (ascending)
                InitialNode newInitialNode = new InitialNode(reqLink.getFromNode(), delayAtLastLink,
                        delayAtLastLink);
                initialNodes.put(reqNodeId, newInitialNode);
                initialRequests.put(reqNodeId, req);
            }
        }

        ImaginaryNode toNodes = router.createImaginaryNode(initialNodes.values());

        //calc path for departure.time+1 (we need 1 second to move over the node)
        Path path = router.calcLeastCostPath(fromNode, toNodes, departure.time + 1, null, null);

        //the calculated path contains real nodes (no imaginary/initial nodes),
        //the time and cost are of real travel (between the first and last real node)
        //(no initial times/costs for imaginary<->initial are included)
        Node toNode = path.nodes.get(path.nodes.size() - 1);
        TaxiRequest bestRequest = initialRequests.get(toNode.getId());
        VrpPathWithTravelData vrpPath = VrpPaths.createPath(departure.link,
                bestRequest.getFromLink(), departure.time, path, optimContext.travelTime);
        return new Dispatch(veh, bestRequest, vrpPath);
    }
}
