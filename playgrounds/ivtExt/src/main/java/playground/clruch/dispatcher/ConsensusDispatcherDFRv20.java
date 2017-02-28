package playground.clruch.dispatcher;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.*;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Claudio on 2/24/2017.
 */


public class ConsensusDispatcherDFRv20 extends PartitionedDispatcher {
    public static final int REBALANCING_PERIOD = 5 * 60; // TODO
    final AbstractVirtualNodeDest virtualNodeDest;
    final AbstractRequestSelector requestSelector;
    final AbstractVehicleDestMatcher vehicleDestMatcher;
    final Map<VirtualLink, Double> linkWeights;
    Map<VirtualLink, Double> rebalanceFloating;


    public ConsensusDispatcherDFRv20( //
                                      AVDispatcherConfig config, //
                                      TravelTime travelTime, //
                                      ParallelLeastCostPathCalculator router, //
                                      EventsManager eventsManager, //
                                      VirtualNetwork virtualNetwork, //
                                      AbstractVirtualNodeDest abstractVirtualNodeDest, //
                                      AbstractRequestSelector abstractRequestSelector, //
                                      AbstractVehicleDestMatcher abstractVehicleDestMatcher, //
                                      Map<VirtualLink, Double> linkWeightsIn
    ) {
        super(config, travelTime, router, eventsManager, virtualNetwork);
        this.virtualNodeDest = abstractVirtualNodeDest;
        this.requestSelector = abstractRequestSelector;
        this.vehicleDestMatcher = abstractVehicleDestMatcher;
        rebalanceFloating = new HashMap<>();
        for (VirtualLink virtualLink : virtualNetwork.getVirtualLinks()) {
            rebalanceFloating.put(virtualLink, 0.0);
        }
        linkWeights = linkWeightsIn;
    }

    private int total_matchedRequests = 0;

    @Override
    public void redispatch(double now) {
        // do not execute the redispatching method at the end of the horizon // TODO find more elegant way to solve this
        // if (now>LAST_REDISPATCH_ITER) return; // not needed anymore, notification will be given instead
        // match requests and vehicles if they are at the same link
        int seconds = (int) Math.round(now);

        total_matchedRequests += MatchRequestsWithStayVehicles.inOrderOfArrival(this);

        // for available vhicles, perform a rebalancing computation after REBALANCING_PERIOD seconds.
        if (seconds % REBALANCING_PERIOD == 0) {
            System.out.println("@" + seconds + " mr = " + total_matchedRequests);
            // 0 get available vehicles and requests per virtual node
            // number of customers, where there has not yet been made a car2customer matching
            Map<VirtualNode, List<AVRequest>> requests = getVirtualNodeRequests();
            Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeAvailableVehicles();


            // 1 Calculate the rebalancing action for every virtual link
            Map<VirtualLink, Integer> rebalanceCount = new HashMap<>();
            {
                for (VirtualLink vlink : virtualNetwork.getVirtualLinks()) {
                    //compute imbalance on nodes of link
                    //if(availableVehicles.containsKey(vlink))
                    int imbalanceFrom = requests.get(vlink.getFrom()).size() - availableVehicles.get(vlink.getFrom()).size();
                    int imbalanceTo = requests.get(vlink.getTo()).size() - availableVehicles.get(vlink.getTo()).size();


                    // compute the rebalancing vehicles
                    double vehicles_From_to_To =  //
                            REBALANCING_PERIOD * linkWeights.get(vlink) * ((double) imbalanceTo - (double) imbalanceFrom) +  //
                                    rebalanceFloating.get(vlink);


                    // assign integer number to rebalance vehicles and store float for next iteration
                    // only consider the results which are >= 0. This assumes an undirected graph.
                    // TODO see if this is possible without searching for the virtualLink in the opposite direction
                    if (vehicles_From_to_To >= 0) {
                        // calculate the integer number of vehicles to actually be sent
                        int rebalanceNmbr = Math.min((int) Math.floor(vehicles_From_to_To),//
                                availableVehicles.get(vlink.getFrom()).size());
                        double leftover = vehicles_From_to_To - rebalanceNmbr;
                        // assign rebalanceCount and leftover for positive edge
                        rebalanceCount.put(vlink, rebalanceNmbr);
                        rebalanceFloating.put(vlink, leftover);

                        // look for edge in opposite direction and set leftover
                        VirtualLink oppositeLink = virtualNetwork.getVirtualLinks().stream().filter(v -> (v.getFrom().equals(vlink.getTo()) && v.getTo().equals(vlink.getFrom()))).findFirst().get();
                        rebalanceCount.put(oppositeLink, 0);
                        rebalanceFloating.put(oppositeLink, -leftover);
                    }
                }
            }

            // create a Map that contains all the outgoing vLinks for a vNode
            Map<VirtualNode, List<VirtualLink>> vLinkShareFromvNode = virtualNetwork.getVirtualLinks().stream().collect(Collectors.groupingBy(VirtualLink::getFrom));

            // ensure that not more vehicles are sent away than available
            for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {

                // count outgoing vehicles from this node:
                int totRebVehicles = 0;
                for (VirtualLink vLink : vLinkShareFromvNode.get(virtualNode)) {
                    totRebVehicles = totRebVehicles + rebalanceCount.get(vLink);
                }

                // not enough available vehicles
                if (availableVehicles.get(virtualNode).size() < totRebVehicles) {
                    // calculate by how much to shrink
                    double shrinkingFactor = ((double) availableVehicles.get(virtualNode).size()) / ((double) totRebVehicles);
                    // remove rebalancing vehicles
                    for (VirtualLink virtualLink : vLinkShareFromvNode.get(virtualNode)) {
                        if (rebalanceCount.get(virtualLink) > 0) {
                            double newRebCountTot = rebalanceCount.get(virtualLink) * shrinkingFactor;
                            int newIntRebCount = (int) Math.floor(newRebCountTot);
                            int newLeftOver = rebalanceCount.get(virtualLink) - newIntRebCount;

                            rebalanceCount.put(virtualLink, newIntRebCount);
                            rebalanceFloating.put(virtualLink, rebalanceFloating.get(virtualLink) + newLeftOver);
                            VirtualLink oppositeLink = virtualNetwork.getVirtualLinks().stream().filter(v -> (v.getFrom().equals(virtualLink.getTo()) && v.getTo().equals(virtualLink.getFrom()))).findFirst().get();
                            rebalanceCount.put(oppositeLink, 0);
                            rebalanceFloating.put(oppositeLink, rebalanceFloating.get(oppositeLink) - newLeftOver);
                        }
                    }
                }
            }

            // 2 generate routing instructions for vehicles
            // 2.1 gather the destination links
            Map<VirtualNode, List<Link>> destinationLinks = new HashMap<>();
            for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes())
                destinationLinks.put(virtualNode, new ArrayList<>());

            // 2.2 fill rebalancing destinations
            // TODO size negative?
            for (Map.Entry<VirtualLink, Integer> entry : rebalanceCount.entrySet()) {
                final VirtualLink virtualLink = entry.getKey();
                final int size = entry.getValue();
                final VirtualNode fromNode = virtualLink.getFrom();
                // Link origin = fromNode.getLinks().iterator().next(); //
                VirtualNode toNode = virtualLink.getTo();

                List<Link> rebalanceTargets = virtualNodeDest.selectLinkSet(toNode, size);

                destinationLinks.get(fromNode).addAll(rebalanceTargets);
            }

            // 2.3 consistency check: rebalancing destination links must not exceed available vehicles in virtual node
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

                Collection<AVRequest> collection = requestSelector.selectRequests( //
                        availableVehicles.get(virtualNode), //
                        requests.get(virtualNode), //
                        size);

                // TODO

                destinationLinks.get(virtualNode).addAll( // stores from links
                        collection.stream().map(AVRequest::getFromLink).collect(Collectors.toList()));
            }


            // 2.4 assign destinations to the available vehicles
            {
                GlobalAssert.that(availableVehicles.keySet().containsAll(virtualNetwork.getVirtualNodes()));
                GlobalAssert.that(destinationLinks.keySet().containsAll(virtualNetwork.getVirtualNodes()));

                long tic = System.nanoTime(); // DO NOT PUT PARALLEL anywhere in this loop !
                for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes())
                    vehicleDestMatcher //
                            .match(availableVehicles.get(virtualNode), destinationLinks.get(virtualNode)) //
                            .entrySet().stream().forEach(this::setVehicleDiversion);
                long dur = System.nanoTime() - tic;
            }

            // 2.5 define action for leftover vehicles
            // no action taken here, possible to send leftover vehicles to new destination in
            // virtual node where they currently stay.
        }
    }

    public static class Factory implements AVDispatcherFactory {
        @Inject
        @Named(AVModule.AV_MODE)
        private ParallelLeastCostPathCalculator router;

        @Inject
        @Named(AVModule.AV_MODE)
        private TravelTime travelTime;

        @Inject
        private EventsManager eventsManager;

        @Inject
        private Network network;

        public static VirtualNetwork virtualNetwork;
        public static Map<VirtualLink, Double> linkWeights;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config) {

            //intstatiate a ConsensusDispatcher for testing
            AbstractVirtualNodeDest abstractVirtualNodeDest = new KMeansVirtualNodeDest();
            AbstractRequestSelector abstractRequestSelector = new OldestRequestSelector();
            AbstractVehicleDestMatcher abstractVehicleDestMatcher = new HungarBiPartVehicleDestMatcher();
            // TODO relate to general directory given in argument of main function


            // TODO:

            return new ConsensusDispatcherDFRv20(
                    config,
                    travelTime,
                    router,
                    eventsManager,
                    virtualNetwork,
                    abstractVirtualNodeDest,
                    abstractRequestSelector,
                    abstractVehicleDestMatcher,
                    linkWeights
            );
        }
    }


}


