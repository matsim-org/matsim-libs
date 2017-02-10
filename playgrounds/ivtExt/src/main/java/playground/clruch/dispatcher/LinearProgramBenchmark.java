package playground.clruch.dispatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.dispatcher.utils.AbstractRequestSelector;
import playground.clruch.dispatcher.utils.AbstractVehicleDestMatcher;
import playground.clruch.dispatcher.utils.AbstractVirtualNodeDest;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class LinearProgramBenchmark extends PartitionedDispatcher {
    public static final int REBALANCING_PERIOD = 5 * 60; // TODO
    final AbstractVirtualNodeDest abstractVirtualNodeDest;
    final AbstractRequestSelector abstractRequestSelector;
    final AbstractVehicleDestMatcher abstractVehicleDestMatcher;

    public LinearProgramBenchmark( //
            AVDispatcherConfig config, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            VirtualNetwork virtualNetwork, //
            AbstractVirtualNodeDest abstractVirtualNodeDest, //
            AbstractRequestSelector abstractRequestSelector, //
            AbstractVehicleDestMatcher abstractVehicleDestMatcher //
    ) {
        super(config, travelTime, router, eventsManager, virtualNetwork);
        this.abstractVirtualNodeDest = abstractVirtualNodeDest;
        this.abstractRequestSelector = abstractRequestSelector;
        this.abstractVehicleDestMatcher = abstractVehicleDestMatcher;
    }

    @Override
    public void redispatch(double now) {
        // 0 filter away requests
        int seconds = (int) Math.round(now);

        {
            // TODO also send vehicles within node in a smart way...
            Map<Link, Queue<AVVehicle>> map = getStayVehicles(); // link -> stayed vehicles
            Collection<AVRequest> collection = getAVRequests(); // all requests
            if (!map.isEmpty() && !collection.isEmpty()) { // has stay vehicles and has requests
                // System.out.println(now + " @ " + map.size() + " <-> " + collection.size());
                for (AVRequest avRequest : collection) {
                    Link link = avRequest.getFromLink();
                    if (map.containsKey(link)) {
                        Queue<AVVehicle> queue = map.get(link);
                        if (queue.isEmpty()) {
                            // unmatched.add(link);
                        } else {
                            AVVehicle avVehicle = queue.poll();
                            setAcceptRequest(avVehicle, avRequest); // PICKUP+DRIVE+DROPOFF
                        }
                    }
                }
            }
        }

        if (seconds % REBALANCING_PERIOD == 0) {
            
            Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeAvailableVehicles();
            Map<VirtualNode, List<AVRequest>> requests = getVirtualNodeRequests();

            // TODO

            // ------------------------------------
            // 1 solve on VN level

            // output
            // TODO implement random Rebalance
            Map<VirtualLink, Integer> rebalanceCount = new HashMap<>(); // TODO result from SOLVER

            // -------------------------------------
            // 2 specific routing instructions

            // ACTION TYPE 1: setAcceptRequest(avVehicle, avRequest); // !!! requires that avVehicle is at same link as request

            // ACTION TYPE 2:
            // setVehicleDiversion(vehicleLinkPair, dest); // single vehicle to new destination link
            // virtualNetwork.
            Map<VirtualNode, List<Link>> destinationLinks = new HashMap<>();
            for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes())
                destinationLinks.put(virtualNode, new ArrayList<>());

            // fill rebalancing destinations
            for (Entry<VirtualLink, Integer> entry : rebalanceCount.entrySet()) {
                final VirtualLink virtualLink = entry.getKey();
                final int size = entry.getValue();
                final VirtualNode fromNode = virtualLink.getFrom();
                // Link origin = fromNode.getLinks().iterator().next(); //
                VirtualNode toNode = virtualLink.getTo();

                Set<Link> rebalanceTargets = abstractVirtualNodeDest.selectLinkSet(toNode, size);

                destinationLinks.get(fromNode).addAll(rebalanceTargets);
            }

            // consistency check: rebalancing destination links must not exceed available vehicles in virtual node
            for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
                int sizeV = availableVehicles.get(virtualNode).size();
                int sizeL = destinationLinks.get(virtualNode).size();
                if (sizeL > sizeV)
                    throw new RuntimeException("rebalancing inconsistent " + sizeL + " > " + sizeV);
            }

            // fill request destinations
            for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
                // number of vehicles that can be matched to requests
                int size = Math.min( //
                        availableVehicles.get(virtualNode).size() - destinationLinks.get(virtualNode).size(), //
                        requests.get(virtualNode).size());

                Collection<AVRequest> collection = abstractRequestSelector.selectRequests( //
                        availableVehicles.get(virtualNode), //
                        requests.get(virtualNode), //
                        size);

                // TODO

                destinationLinks.get(virtualNode).addAll( // stores from links
                        collection.stream().map(AVRequest::getFromLink).collect(Collectors.toList()));
            }

            // fill extra destinations for left over vehicles

            for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
                // number of vehicles that can be matched to requests
                int size = availableVehicles.get(virtualNode).size() - destinationLinks.get(virtualNode).size(); //

                // TODO maybe not final API; may cause excessive diving
                Set<Link> localTargets = abstractVirtualNodeDest.selectLinkSet(virtualNode, size);
                destinationLinks.get(virtualNode).addAll(localTargets);

            }

            //
            for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {

                Map<VehicleLinkPair, Link> map = abstractVehicleDestMatcher.match(availableVehicles.get(virtualNode), destinationLinks.get(virtualNode));
                for (Entry<VehicleLinkPair, Link> entry : map.entrySet()) {
                    VehicleLinkPair vehicleLinkPair = entry.getKey();
                    Link link = entry.getValue();
                    setVehicleDiversion(vehicleLinkPair, link);
                }
            }
        }
    }

}
