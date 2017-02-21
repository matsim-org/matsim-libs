package playground.clruch.dispatcher;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.*;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class ConsensusDispatcher extends PartitionedDispatcher {
    public static final int REBALANCING_PERIOD = 5 * 60; // TODO
    final AbstractVirtualNodeDest abstractVirtualNodeDest;
    final AbstractRequestSelector abstractRequestSelector;
    final AbstractVehicleDestMatcher abstractVehicleDestMatcher;
    final Map<VirtualLink, Double> linkWeights;
    Map<VirtualLink, Double> rebalanceFloating;


    public ConsensusDispatcher( //
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
        this.abstractVirtualNodeDest = abstractVirtualNodeDest;
        this.abstractRequestSelector = abstractRequestSelector;
        this.abstractVehicleDestMatcher = abstractVehicleDestMatcher;
        rebalanceFloating = new HashMap<>();
        for (VirtualLink virtualLink : virtualNetwork.getVirtualLinks()) {
            rebalanceFloating.put(virtualLink, 0.0);
        }
        linkWeights = linkWeightsIn;
    }

    @Override
    public void redispatch(double now) {

        // match requests and vehicles if they are at t
        // he same link
        int seconds = (int) Math.round(now);
        {
            MatchRequestsWithStayVehicles.inOrderOfArrival(this);
        }


        // for available vhicles, perform a rebalancing computation after REBALANCING_PERIOD seconds.
        if (seconds % REBALANCING_PERIOD == 0) {
            // 0 get available vehicles and requests per virtual node
            Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeAvailableVehicles();
            Map<VirtualNode, List<AVRequest>> requests = getVirtualNodeRequests();

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
                        VirtualLink oppositeLink = virtualNetwork.getVirtualLinks().stream().filter(v-> (v.getFrom().equals(vlink.getTo()) && v.getTo().equals(vlink.getFrom()) ) ).findFirst().get();
                        rebalanceCount.put(oppositeLink,0);
                        rebalanceFloating.put(oppositeLink,-leftover);

                    }
                }
            }


            // Debugging, print nonzero rebalancing orders
            System.out.println("Print nonzero rebalancing: ");
            rebalanceCount.values().stream().filter(v->v>0).forEach(v-> System.out.println("value is " +v));




            // 2 generate routing instructions for vehicles
            // 2.1 gather the destination links
            Map<VirtualNode, List<Link>> destinationLinks = new HashMap<>();
            for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes())
                destinationLinks.put(virtualNode, new ArrayList<>());

            // 2.2 fill rebalancing destinations

            // TODO size negative?
            for (Entry<VirtualLink, Integer> entry : rebalanceCount.entrySet()) {
                final VirtualLink virtualLink = entry.getKey();
                final int size = entry.getValue();
                final VirtualNode fromNode = virtualLink.getFrom();
                // Link origin = fromNode.getLinks().iterator().next(); //
                VirtualNode toNode = virtualLink.getTo();

                Set<Link> rebalanceTargets = abstractVirtualNodeDest.selectLinkSet(toNode, size);

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

                Collection<AVRequest> collection = abstractRequestSelector.selectRequests( //
                        availableVehicles.get(virtualNode), //
                        requests.get(virtualNode), //
                        size);

                // TODO

                destinationLinks.get(virtualNode).addAll( // stores from links
                        collection.stream().map(AVRequest::getFromLink).collect(Collectors.toList()));
            }

            // 2.4 fill extra destinations for left over vehicles
            for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
                // number of vehicles that can be matched to requests
                int size = availableVehicles.get(virtualNode).size() - destinationLinks.get(virtualNode).size(); //

                // TODO maybe not final API; may cause excessive diving
                Set<Link> localTargets = abstractVirtualNodeDest.selectLinkSet(virtualNode, size);
                destinationLinks.get(virtualNode).addAll(localTargets);

            }

            // 2.5 assign destinations to the available vehicles
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
            AbstractVirtualNodeDest abstractVirtualNodeDest = new RandomVirtualNodeDest();
            AbstractRequestSelector abstractRequestSelector = new OldestRequestSelector();
            AbstractVehicleDestMatcher abstractVehicleDestMatcher = new NativeVehicleDestMatcher();
            // TODO relate to general directory given in argument of main function


            // TODO:

            return new ConsensusDispatcher(
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


