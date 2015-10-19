package playground.michalm.taxi.vehreqpath;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.router.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;
import playground.michalm.taxi.scheduler.TaxiScheduler;


public class VehicleRequestPathFinder
{
    private final TaxiOptimizerConfiguration optimConfig;
    private final MultiNodeDijkstra router;
    private final TaxiScheduler scheduler;


    public VehicleRequestPathFinder(TaxiOptimizerConfiguration optimConfig)
    {
        this.optimConfig = optimConfig;
        this.scheduler = optimConfig.scheduler;

        router = new MultiNodeDijkstra(optimConfig.context.getScenario().getNetwork(),
                optimConfig.travelDisutility, optimConfig.travelTime, false);
    }


    //for immediate requests only
    //minimize TW
    public VehicleRequestPath findBestVehicleForRequest(TaxiRequest req,
            Iterable<? extends Vehicle> vehicles)
    {
        double currTime = optimConfig.context.getTime();
        Link toLink = req.getFromLink();
        Node toNode = toLink.getFromNode();

        Map<Id<Node>, Vehicle> initialVehicles = new HashMap<>();
        Map<Id<Node>, InitialNode> initialNodes = new HashMap<>();
        for (Vehicle veh : vehicles) {
            LinkTimePair departure = scheduler.getImmediateDiversionOrEarliestIdleness(veh);
            if (departure != null) {
                Node vehNode = departure.link == toLink ? //
                        toNode : //hack: we are basically there (on the same link), so let's pretend vehNode == toNode
                        departure.link.getToNode();

                double delay = departure.time - currTime;

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

        //calc path for currTime+1 (we need 1 second to move over the node)
        Path path = router.calcLeastCostPath(fromNodes, toNode, currTime + 1, null, null);
        //the calculated path contains real nodes (no imaginary/initial nodes),
        //the time and cost are of real travel (between the first and last real node)
        //(no initial times/costs for imaginary<->initial are included)
        Node fromNode = path.nodes.get(0);
        Vehicle bestVehicle = initialVehicles.get(fromNode.getId());
        LinkTimePair bestDeparture = scheduler.getImmediateDiversionOrEarliestIdleness(bestVehicle);

        VrpPathWithTravelData vrpPath = VrpPaths.createPath(bestDeparture.link, toLink,
                bestDeparture.time, path, optimConfig.travelTime, optimConfig.travelDisutility);
        return new VehicleRequestPath(bestVehicle, req, vrpPath);
    }


    //for immediate requests only
    //minimize TP
    public VehicleRequestPath findBestRequestForVehicle(Vehicle veh,
            Iterable<TaxiRequest> unplannedRequests)
    {
        LinkTimePair departure = scheduler.getImmediateDiversionOrEarliestIdleness(veh);
        Link fromLink = departure.link;
        Node fromNode = fromLink.getToNode();

        Map<Id<Node>, TaxiRequest> initialRequests = new HashMap<>();
        Map<Id<Node>, InitialNode> initialNodes = new HashMap<>();
        for (TaxiRequest req : unplannedRequests) {
            Link reqLink = req.getFromLink();

            Node reqNode = fromLink == reqLink ? //
                    fromNode : //hack: we are basically there (on the same link), so let's pretend reqNode == fromNode
                    reqLink.getFromNode();

            InitialNode existingInitialNode = initialNodes.get(reqNode.getId());
            if (existingInitialNode == null) {//works most fair if unplannedRequests are sorted by T0 (ascending)
                InitialNode newInitialNode = new InitialNode(reqNode, 0, 0);
                initialNodes.put(reqNode.getId(), newInitialNode);
                initialRequests.put(reqNode.getId(), req);
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
                bestRequest.getFromLink(), departure.time, path, optimConfig.travelTime,
                optimConfig.travelDisutility);
        return new VehicleRequestPath(veh, bestRequest, vrpPath);
    }
}
