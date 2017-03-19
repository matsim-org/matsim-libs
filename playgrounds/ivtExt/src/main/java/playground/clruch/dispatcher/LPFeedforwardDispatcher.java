/**
 * Dispatcher implementing the linear program from
 * Pavone, Marco, Stephen Smith, Emilio Frazzoli, and Daniela Rus. 2011.
 * “Load Balancing for Mobility-on-Demand Systems.” In Robotics: Science and Systems VII. doi:10.15607/rss.2011.vii.034.
 * <p>
 * <p>
 * Implemented by Claudio Ruch on 2017, 02, 25
 */


package playground.clruch.dispatcher;

import ch.ethz.idsc.tensor.*;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Floor;
import ch.ethz.idsc.tensor.alg.Total;
import ch.ethz.idsc.tensor.alg.Transpose;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.gnu.glpk.GLPK;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LPFeedforwardDispatcher extends PartitionedDispatcher {
    public final int rebalancingPeriod;
    public final int redispatchPeriod;
    final AbstractVirtualNodeDest virtualNodeDest;
    final AbstractRequestSelector requestSelector;
    final AbstractVehicleDestMatcher vehicleDestMatcher;
    final Map<VirtualLink, Double> travelTimes;
    final int numberOfAVs;
    private int total_rebalanceCount = 0;
    Tensor printVals = Tensors.empty();
    ArrivalInformation arrivalInformation;
    Tensor rebalancingRate;
    Tensor rebalanceCount = Tensors.matrix((i, j) -> RealScalar.of(0.0), virtualNetwork.getvNodesCount(), virtualNetwork.getvNodesCount());
    Tensor rebalanceCountInteger = Tensors.matrix((i, j) -> RealScalar.of(0.0), virtualNetwork.getvNodesCount(), virtualNetwork.getvNodesCount());


    public LPFeedforwardDispatcher( //
                                    AVDispatcherConfig config, //
                                    AVGeneratorConfig generatorConfig, //
                                    TravelTime travelTime, //
                                    ParallelLeastCostPathCalculator router, //
                                    EventsManager eventsManager, //
                                    VirtualNetwork virtualNetwork, //
                                    AbstractVirtualNodeDest abstractVirtualNodeDest, //
                                    AbstractRequestSelector abstractRequestSelector, //
                                    AbstractVehicleDestMatcher abstractVehicleDestMatcher, //
                                    Map<VirtualLink, Double> travelTimesIn,
                                    ArrivalInformation arrivalInformationIn
    ) {
        super(config, travelTime, router, eventsManager, virtualNetwork);
        this.virtualNodeDest = abstractVirtualNodeDest;
        this.requestSelector = abstractRequestSelector;
        this.vehicleDestMatcher = abstractVehicleDestMatcher;
        travelTimes = travelTimesIn;
        numberOfAVs = (int) generatorConfig.getNumberOfVehicles();
        rebalancingPeriod = Integer.parseInt(config.getParams().get("rebalancingPeriod"));
        redispatchPeriod = Integer.parseInt(config.getParams().get("redispatchPeriod"));
        arrivalInformation = arrivalInformationIn;
    }


    @Override
    public void redispatch(double now) {
        // PART 0: match vehicles at a customer link
        new InOrderOfArrivalMatcher(this::setAcceptRequest) //
                .match(getStayVehicles(), getAVRequestsAtLinks());
        final long round_now = Math.round(now);

        // Recalculate new rebalancing rates periodically
        if (round_now % rebalancingPeriod == 0) {
            // setup linear program
            LPVehicleRebalancing lpVehicleRebalancing = new LPVehicleRebalancing(virtualNetwork, travelTimes);


            Map<VirtualNode, List<AVRequest>> requests = getVirtualNodeRequests();
            // II.i compute rebalancing vehicles and send to virtualNodes
            {
                // lambdas = [lambda_1, lambda_2, ... , lambda_n] where n number of virtual nodes and lambdas a row rector
                Tensor lambdas = Tensors.matrix((i, j) -> getLambdai(now, j), 1, virtualNetwork.getvNodesCount());
                // p_ij_bar row-stochastic matrix of dimension nxn with transition probabilities from i to j
                // p_ij = p_ij_bar with the diagonal elements set to zero
                Tensor p_ij = Tensors.matrix((i, j) -> getPij(now, i, j), virtualNetwork.getvNodesCount(), virtualNetwork.getvNodesCount());

                // fill right-hand-side, i.e. rhs(i) = -lambda_i + sum_j lambda_j p_ji
                Tensor rhs = (lambdas.multiply(RealScalar.of(-1)).add(lambdas.dot(p_ij))).get(0);

                // solve the linear program with updated right-hand side
                rebalancingRate = lpVehicleRebalancing.solveUpdatedLP(rhs);

                // TODO this should never become active, can be removed later (nonnegative solution)
                for (int i = 0; i < virtualNetwork.getvNodesCount(); ++i) {
                    for (int j = 0; j < virtualNetwork.getvNodesCount(); ++j) {
                        RealScalar value = (RealScalar) rebalancingRate.Get(i, j);
                        double entry = value.getRealDouble();
                        GlobalAssert.that(entry >= 0);
                    }
                }

                // close the LP to avoid data leaks
                // TODO can this be taken out to not delete the LP in every instance?
                lpVehicleRebalancing.closeLP();
            }
        }


        // Part II: outside rebalancing periods, permanently assign vehicles to requests if they have arrived at a customer i.e. stay on the same link
        // in the identical vNode match customers to requests, assign desitnations to vehicles using bipartite matching
        if (round_now % redispatchPeriod == 0) {
            // update rebalance count
            rebalanceCount = rebalanceCount.add(rebalancingRate.multiply(RealScalar.of(redispatchPeriod)));

            // redispatch integer values and remove from rebalanceCount
            // TODO check for more elegant formulation
            for (int i = 0; i < rebalanceCount.dimensions().get(0); ++i) {
                for (int j = 0; j < rebalanceCount.dimensions().get(1); ++j) {
                    double toSend = rebalanceCount.get(i).Get(j).getAbsDouble();
                    if (toSend > 1.0) {
                        Tensor lineToUpdate = rebalanceCountInteger.get(i);
                        lineToUpdate.set(RealScalar.of(1.0),j);
                        rebalanceCountInteger.set(lineToUpdate,i);


                        Tensor lineToupdate2 = rebalanceCount.get(i);
                        lineToupdate2.set(lineToupdate2.Get(j).subtract(RealScalar.of(1.0)),j);
                        rebalanceCount.set(lineToupdate2,i);
                    }
                }
            }

            // ensure that not more vehicles are sent away than available
            Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeDivertableNotRebalancingVehicles();
            Tensor feasibleRebalanceCount = returnFeasibleRebalance(rebalanceCountInteger.unmodifiable(), availableVehicles);
            total_rebalanceCount += (int) ((RealScalar) Total.of(Tensor.of(feasibleRebalanceCount.flatten(-1)))).getRealDouble();

            // generate routing instructions for rebalancing vehicles
            Map<VirtualNode, List<Link>> destinationLinks = createvNodeLinksMap();

            // fill rebalancing destinations
            for (int i = 0; i < virtualNetwork.getvLinksCount(); ++i) {
                VirtualLink virtualLink = this.virtualNetwork.getVirtualLink(i);
                VirtualNode toNode = virtualLink.getTo();
                VirtualNode fromNode = virtualLink.getFrom();
                int numreb = (int) ((RealScalar) feasibleRebalanceCount.Get(fromNode.index, toNode.index)).getRealDouble();
                List<Link> rebalanceTargets = virtualNodeDest.selectLinkSet(toNode, numreb);
                destinationLinks.get(fromNode).addAll(rebalanceTargets);
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


            // reset vector
            rebalanceCountInteger = Tensors.matrix((i, j) -> RealScalar.of(0.0), virtualNetwork.getvNodesCount(), virtualNetwork.getvNodesCount());
        }


        // assign desitnations to vehicles using bipartite matching
        printVals = HungarianDispatcher.globalBipartiteMatching(this, () -> getVirtualNodeDivertableNotRebalancingVehicles().values()
                .stream().flatMap(v -> v.stream()).collect(Collectors.toList()));
    }


    /**
     * @param rebalanceInput    entry i,j contains the rebalance input from virtualNode i to virtualNode j
     * @param availableVehicles the available vehicles per virtualNode
     * @return
     */
    private Tensor returnFeasibleRebalance(Tensor rebalanceInput, Map<VirtualNode, List<VehicleLinkPair>> availableVehicles) {
        Tensor feasibleRebalance = rebalanceInput.copy();

        for (int i = 0; i < virtualNetwork.getvNodesCount(); ++i) {
            // count number of outgoing vehicles per vNode
            double outgoingNmrvNode = 0.0;
            Tensor outgoingVehicles = rebalanceInput.get(i);
            for (int j = 0; j < virtualNetwork.getvNodesCount(); ++j) {
                outgoingNmrvNode = outgoingNmrvNode + ((RealScalar) outgoingVehicles.Get(j)).getRealDouble();
            }
            int outgoingVeh = (int) outgoingNmrvNode;
            int finalI = i;
            int availableVehvNode = availableVehicles.get(availableVehicles.keySet().stream().filter(v -> v.index == finalI).findAny().get()).size();
            // if number of outoing vehicles too small, reduce proportionally
            if (availableVehvNode < outgoingVeh) {
                long shrinkingFactor = ((long) availableVehvNode / ((long) outgoingVeh));
                Tensor newRow = Floor.of(rebalanceInput.get(i).multiply(RealScalar.of(shrinkingFactor)));
                feasibleRebalance.set(newRow, i);
            }
        }
        return feasibleRebalance;
    }

    /**
     * @param time  current time
     * @param index index of node to be checked
     * @return lambda value of this node for given time
     */
    Scalar getLambdai(double time, int index) {
        return arrivalInformation.getLambdaforTime((int) time, index);
    }


    /**
     * @param time current time
     * @param row  from node index
     * @param col  to node index
     * @return p_ij = p_fromTo for that time
     */
    Scalar getPij(double time, int row, int col) {
        if (row == col) { // diagonal elements have to be zero
            return RealScalar.of(0.0);
        } else { // off-diagonal elements according to data input
            return arrivalInformation.getpijforTime((int) time, row, col);
        }
    }


    @Override
    public String getInfoLine() {
        return String.format("%s RV=%s H=%s", //
                super.getInfoLine(), //
                total_rebalanceCount, //
                printVals.toString() //
        );
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
        public static Map<VirtualLink, Double> travelTimes;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig) {

            AbstractVirtualNodeDest abstractVirtualNodeDest = new KMeansVirtualNodeDest();
            AbstractRequestSelector abstractRequestSelector = new OldestRequestSelector();
            AbstractVehicleDestMatcher abstractVehicleDestMatcher = new HungarBiPartVehicleDestMatcher();

            File virtualnetworkXML = new File(config.getParams().get("virtualNetworkFile"));
            File lambdaFileXML = new File(config.getParams().get("lambdaFile"));
            File pijFileXML = new File(config.getParams().get("pijFile"));
            System.out.println("" + virtualnetworkXML.getAbsoluteFile());
            virtualNetwork = VirtualNetworkLoader.fromXML(network, virtualnetworkXML);
            travelTimes = vLinkDataReader.fillvLinkData(virtualnetworkXML, virtualNetwork, "Ttime");
            ArrivalInformation arrivalInformation = null;
            try {
                arrivalInformation = new ArrivalInformation(virtualNetwork, lambdaFileXML, pijFileXML);
            } catch (Exception e) {
                System.out.println("something went wrong.");
            }


            return new LPFeedforwardDispatcher(
                    config,
                    generatorConfig,
                    travelTime,
                    router,
                    eventsManager,
                    virtualNetwork,
                    abstractVirtualNodeDest,
                    abstractRequestSelector,
                    abstractVehicleDestMatcher,
                    travelTimes,
                    arrivalInformation
            );
        }
    }
}





