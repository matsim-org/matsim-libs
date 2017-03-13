/**
 * Dispatcher implementing the linear program from
 * Pavone, Marco, Stephen Smith, Emilio Frazzoli, and Daniela Rus. 2011.
 * “Load Balancing for Mobility-on-Demand Systems.” In Robotics: Science and Systems VII. doi:10.15607/rss.2011.vii.034.
 * <p>
 * <p>
 * Implemented by Claudio Ruch on 2017, 02, 25
 */


package playground.clruch.dispatcher;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.*;
import playground.clruch.netdata.*;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ConsensusDispatcherDFR extends PartitionedDispatcher {
    public final int REBALANCING_PERIOD;
    final AbstractVirtualNodeDest virtualNodeDest;
    final AbstractRequestSelector requestSelector;
    final AbstractVehicleDestMatcher vehicleDestMatcher;
    Map<VirtualLink, Double> rebalanceFloating;
    final AbstractVehicleRequestMatcher vehicleRequestMatcher;
    private int total_matchedRequests = 0;
    final Map<VirtualLink, Double> vLinkWeights;
    final int numberOfAVs;
    //Map<VirtualNode, List<VirtualLink>> vLinkSameFromVNode = virtualNetwork.getVirtualLinks()
    //        .stream().collect(Collectors.groupingBy(VirtualLink::getFrom));


    public ConsensusDispatcherDFR( //
                                   AVDispatcherConfig config, //
                                   AVGeneratorConfig generatorConfig, //
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
        vLinkWeights = linkWeightsIn;
        vehicleRequestMatcher = new InOrderOfArrivalMatcher(this::setAcceptRequest);
        numberOfAVs = (int) generatorConfig.getNumberOfVehicles();
        REBALANCING_PERIOD = Integer.parseInt(config.getParams().get("rebalancingPeriod"));
    }


    @Override
    public void redispatch(double now) {
        // A: outside rebalancing periods, permanently assign vehicles to requests if they have arrived at a customer
        //    i.e. stay on the same link
        total_matchedRequests += vehicleRequestMatcher.match(getStayVehicles(), getAVRequestsAtLinks());
        //Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeAvailableNotRebalancingVehicles();


        // B: redispatch all vehicles
        final long round_now = Math.round(now);
        if (round_now % REBALANCING_PERIOD == 0) {
            System.out.println(getClass().getSimpleName() + " @" + round_now + " mr = " + total_matchedRequests);
            Map<VirtualNode, List<AVRequest>> requests = getVirtualNodeRequests();
            // II.i compute rebalancing vehicles and send to virtualNodes
            {
                // TODO: ensure that a rebalanced vehicle is then under the control of the to-virtualNode and can be dispatched there.
                Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeAvailableNotRebalancingVehicles();

                // DEBUGGING STARTED
                if(round_now > 4929){
                    System.out.println("half an hour has passed. ");
                    Map<VirtualNode, List<VehicleLinkPair>> availableVehicless = getVirtualNodeAvailableVehicles();
                    String asd = availableVehicless.toString()+"123";
                }

                // DEBUGGING ENDED


                // Calculate the excess vehicles per virtual Node i, where v_i excess = vi_own - c_i = v_i + sum_j (v_ji) - c_i
                // TODO check if sum_j (v_ji) also contains the customer vehicles travelling to v_i and add if so.
                Map<VirtualNode, Integer> vi_excess = new HashMap<>();
                Map<VirtualNode, Set<AVVehicle>> v_ij_reb = getVirtualNodeRebalancingToVehicles();
                Map<VirtualNode, Set<AVVehicle>> v_ij_cust = getVirtualNodeArrivingWCustomerVehicles();
                for (VirtualNode virtualNode : availableVehicles.keySet()) {
                    if (v_ij_cust.get(virtualNode).size() > 0) {
                        System.out.println("Customer is travelling");
                    }
                    vi_excess.put(virtualNode, availableVehicles.get(virtualNode).size()
                            + v_ij_reb.get(virtualNode).size()
                            - requests.get(virtualNode).size());
                }


                // 1 Calculate the rebalancing action for every virtual link
                Map<VirtualLink, Integer> rebalanceCount = new HashMap<>();
                {
                    for (VirtualLink vLink : virtualNetwork.getVirtualLinks()) {
                        //compute imbalance on nodes of link
                        //if(availableVehicles.containsKey(vlink))
                        int imbalanceFrom = -vi_excess.get(vLink.getFrom());
                        int imbalanceTo = -vi_excess.get(vLink.getTo());


                        // compute the rebalancing vehicles
                        // TODO replace lambda_dummy with data from XML file
                        double lambda_dummy_to = 1.0;
                        double lambda_dummy_from = 1.0;
                        double vehicles_From_to_To =  //
                                REBALANCING_PERIOD * vLinkWeights.get(vLink) * ((double) imbalanceTo / lambda_dummy_to - (double) imbalanceFrom / lambda_dummy_from) +  //
                                        rebalanceFloating.get(vLink);

                        int rebalanceFromTo = (int) Math.round(vehicles_From_to_To);
                        double rebalanceRest = vehicles_From_to_To - (double) rebalanceFromTo;
                        rebalanceCount.put(vLink, rebalanceFromTo);
                        rebalanceFloating.put(vLink, rebalanceRest);
                    }
                }


                // ensure that not more vehicles are sent away than available
                Map<VirtualLink, Integer> feasibleRebalanceCount = new HashMap<>();
                feasibleRebalanceCount = returnFeasibleRebalance(rebalanceCount, availableVehicles);
                // TODO see why LP returns infeasible solutions, extend LP if it is possible to keep totally unimodular matrix


                // generate routing instructions for rebalancing vehicles
                Map<VirtualNode, List<Link>> destinationLinks = new HashMap<>();
                for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes())
                    destinationLinks.put(virtualNode, new ArrayList<>());

                // fill rebalancing destinations
                for (Map.Entry<VirtualLink, Integer> entry : feasibleRebalanceCount.entrySet()) {
                    if (feasibleRebalanceCount.get(entry.getKey()) >= 0) {
                        List<Link> rebalanceTargets = virtualNodeDest.selectLinkSet(entry.getKey().getTo(), entry.getValue());
                        destinationLinks.get(entry.getKey().getFrom()).addAll(rebalanceTargets);
                    } else {
                        List<Link> rebalanceTargets = virtualNodeDest.selectLinkSet(entry.getKey().getFrom(), - entry.getValue());
                        destinationLinks.get(entry.getKey().getTo()).addAll(rebalanceTargets);
                    }
                }

                // consistency check: rebalancing destination links must not exceed available vehicles in virtual node
                Map<VirtualNode, List<VehicleLinkPair>> finalAvailableVehicles = availableVehicles;
                GlobalAssert.that(!virtualNetwork.getVirtualNodes().stream()
                        .filter(v -> finalAvailableVehicles.get(v).size() < destinationLinks.get(v).size())
                        .findAny().isPresent());


                // send rebalancing vehicles using the setVehicleRebalance command
                for (VirtualNode virtualNode : destinationLinks.keySet()) {
                    Map<VehicleLinkPair, Link> rebalanceMatching = vehicleDestMatcher.match(availableVehicles.get(virtualNode), destinationLinks.get(virtualNode));
                    rebalanceMatching.keySet().forEach(v -> setVehicleRebalance(v, rebalanceMatching.get(v)));
                }
            }

            // II.ii if vehilces remain in vNode, send to customers
            {
                // collect destinations per vNode
                Map<VirtualNode, List<Link>> destinationLinks = new HashMap<>();
                virtualNetwork.getVirtualNodes().stream().forEach(v -> destinationLinks.put(v, new ArrayList<>()));
                for (VirtualNode vNode : virtualNetwork.getVirtualNodes()) {
                    destinationLinks.get(vNode).addAll( // stores from links
                            requests.get(vNode).stream().map(AVRequest::getFromLink).collect(Collectors.toList()));
                }

                // collect available vehicles per vNode
                Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeAvailableNotRebalancingVehicles();


                // assign destinations to the available vehicles
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
            }


        }
    }


    private Map<VirtualLink, Integer> returnFeasibleRebalance(Map<VirtualLink, Integer> rebalanceInput, Map<VirtualNode, List<VehicleLinkPair>> availableVehicles) {
        Map<VirtualLink, Integer> feasibleRebalance = new HashMap<>();
        feasibleRebalance = rebalanceInput;

        // for every vNode check if enough vehicles are available to rebalance
        for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {

            // count outgoing rebalancing vehicles from the vNode
            int totRebVecFromvNode = 0;
            for (VirtualLink vLink : rebalanceInput.keySet()) {
                if(vLink.getFrom().equals(virtualNode) || rebalanceInput.get(vLink)>=0){
                    totRebVecFromvNode = totRebVecFromvNode + rebalanceInput.get(vLink);
                }
                if (vLink.getTo().equals(virtualNode) || rebalanceInput.get(vLink)<0){
                    totRebVecFromvNode = totRebVecFromvNode - rebalanceInput.get(vLink);
                }
            }

            // TODO think if instead of shrinking factor just for some links vehicles should be sent instead (less wait time)
            // adapt number of vehicles to be sent
            if (availableVehicles.get(virtualNode).size() < totRebVecFromvNode) {
                // calculate by how much to shrink
                double shrinkingFactor = ((double) availableVehicles.get(virtualNode).size()) / ((double) totRebVecFromvNode);
                // remove rebalancing vehicles
                for (VirtualLink vLink : rebalanceInput.keySet()) {
                    if(vLink.getFrom().equals(virtualNode) || rebalanceInput.get(vLink)>=0){
                        int newIntRebCount = (int) Math.floor(rebalanceInput.get(vLink) * shrinkingFactor);
                        int newLeftOver = rebalanceInput.get(vLink) - newIntRebCount;
                        feasibleRebalance.put(vLink, newIntRebCount);
                        double oldRebFloating = rebalanceFloating.get(vLink);
                        rebalanceFloating.put(vLink, oldRebFloating + (double) newLeftOver);
                    }
                    if (vLink.getTo().equals(virtualNode) || rebalanceInput.get(vLink)<0){
                        int newIntRebCount = (int) Math.floor(rebalanceInput.get(vLink) * shrinkingFactor);
                        int newLeftOver = rebalanceInput.get(vLink) - newIntRebCount;
                        feasibleRebalance.put(vLink, newIntRebCount);
                        double oldRebFloating = rebalanceFloating.get(vLink);
                        rebalanceFloating.put(vLink, oldRebFloating + (double) newLeftOver);
                    }
                }
            }
        }
        return feasibleRebalance;
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
        public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig) {

            AbstractVirtualNodeDest abstractVirtualNodeDest = new KMeansVirtualNodeDest();
            AbstractRequestSelector abstractRequestSelector = new OldestRequestSelector();
            AbstractVehicleDestMatcher abstractVehicleDestMatcher = new HungarBiPartVehicleDestMatcher();

            // TODO get this directory from config / generatorConfig, remove hardcode
            File virtualnetworkXML = new File("C:/Users/Claudio/Documents/matsim_Simulations/2017_03_08_Consensus/virtualNetwork.xml");
            File consensusWeightsXML = new File("C:/Users/Claudio/Documents/matsim_Simulations/2017_03_08_Consensus/consensusWeights.xml");
            virtualNetwork = VirtualNetworkLoader.fromXML(network, virtualnetworkXML);
            linkWeights = vLinkDataReader.fillvLinkData(consensusWeightsXML, virtualNetwork, "weight");


            return new ConsensusDispatcherDFR(
                    config,
                    generatorConfig,
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




