/**
 * Dispatcher implementing the linear program from Pavone, Marco, Stephen Smith, Emilio Frazzoli, and Daniela Rus. 2011.
 * “Load Balancing for Mobility-on-Demand Systems.” In Robotics: Science and Systems VII. doi:10.15607/rss.2011.vii.034.
 * Implemented by Claudio Ruch on 2017, 02, 25
 */

package playground.clruch.dispatcher;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.tensor.RationalScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.Round;
import playground.clruch.dispatcher.core.PartitionedDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.AbstractVehicleDestMatcher;
import playground.clruch.dispatcher.utils.AbstractVirtualNodeDest;
import playground.clruch.dispatcher.utils.FeasibleRebalanceCreator;
import playground.clruch.dispatcher.utils.HungarBiPartVehicleDestMatcher;
import playground.clruch.dispatcher.utils.InOrderOfArrivalMatcher;
import playground.clruch.dispatcher.utils.KMeansVirtualNodeDest;
import playground.clruch.dispatcher.utils.LPVehicleRebalancing;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkIO;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class LPFeedbackLIPDispatcher extends PartitionedDispatcher {
    public final int rebalancingPeriod;
    public final int redispatchPeriod;
    final AbstractVirtualNodeDest virtualNodeDest;
    final AbstractVehicleDestMatcher vehicleDestMatcher;
    final int numberOfAVs;
    private int total_rebalanceCount = 0;
    Tensor printVals = Tensors.empty();
    LPVehicleRebalancing lpVehicleRebalancing;

    public LPFeedbackLIPDispatcher( //
            AVDispatcherConfig config, //
            AVGeneratorConfig generatorConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            VirtualNetwork virtualNetwork, //
            AbstractVirtualNodeDest abstractVirtualNodeDest, //
            AbstractVehicleDestMatcher abstractVehicleDestMatcher) {
        super(config, travelTime, router, eventsManager, virtualNetwork);
        this.virtualNodeDest = abstractVirtualNodeDest;

        this.vehicleDestMatcher = abstractVehicleDestMatcher;
        numberOfAVs = (int) generatorConfig.getNumberOfVehicles();
        rebalancingPeriod = Integer.parseInt(config.getParams().get("rebalancingPeriod"));
        redispatchPeriod = Integer.parseInt(config.getParams().get("redispatchPeriod"));
        // setup linear program
        lpVehicleRebalancing = new LPVehicleRebalancing(virtualNetwork);
    }

    @Override
    public void redispatch(double now) {
        // PART 0: match vehicles at a customer link
        new InOrderOfArrivalMatcher(this::setAcceptRequest) //
                .match(getStayVehicles(), getAVRequestsAtLinks());

        // PART I: rebalance all vehicles periodically
        final long round_now = Math.round(now);

        if (round_now % rebalancingPeriod == 0) {

            Map<VirtualNode, List<AVRequest>> requests = getVirtualNodeRequests();
            // II.i compute rebalancing vehicles and send to virtualNodes
            {
                Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeDivertableNotRebalancingVehicles();

                // Debug:
                System.out.println("printing the sizes of the availableVehicles:");
                availableVehicles.values().stream().forEach(v -> System.out.print("  " + v.size()));
                System.out.println();

                // calculate desired vehicles per vNode
                int num_requests = requests.values().stream().mapToInt(List::size).sum();
                int vi_desired_num = (int) ((numberOfAVs - num_requests) / (double) virtualNetwork.getvNodesCount());
                GlobalAssert.that(vi_desired_num * virtualNetwork.getvNodesCount() <= numberOfAVs);
                Tensor vi_desiredT = Tensors.vector(i -> RationalScalar.of(vi_desired_num, 1), virtualNetwork.getvNodesCount());

                // calculate excess vehicles per virtual Node i, where v_i excess = vi_own - c_i =
                // v_i + sum_j (v_ji) - c_i
                Map<VirtualNode, Set<AVVehicle>> v_ij_reb = getVirtualNodeRebalancingToVehicles();
                Map<VirtualNode, Set<AVVehicle>> v_ij_cust = getVirtualNodeArrivingWCustomerVehicles();
                Tensor vi_excessT = Array.zeros(virtualNetwork.getvNodesCount());
                for (VirtualNode virtualNode : availableVehicles.keySet()) {
                    int viExcessVal = availableVehicles.get(virtualNode).size() + v_ij_reb.get(virtualNode).size()
                            + v_ij_cust.get(virtualNode).size() - requests.get(virtualNode).size();
                    vi_excessT.set(RealScalar.of(viExcessVal), virtualNode.index);
                }

                // solve the linear program with updated right-hand side
                // fill right-hand-side
                Tensor rhs = vi_desiredT.subtract(vi_excessT);
                Tensor rebalanceCount2 = lpVehicleRebalancing.solveUpdatedLP(rhs);
                Tensor rebalanceCount = Round.of(rebalanceCount2);
                // TODO this should never become active, can be possibly removed later
                // assert that solution is integer and does not contain negative values
                GlobalAssert.that(!rebalanceCount.flatten(-1).map(Scalar.class::cast).map(s -> s.number().doubleValue()).filter(e -> e < 0)
                        .findAny().isPresent());

                // ensure that not more vehicles are sent away than available
                Tensor feasibleRebalanceCount = FeasibleRebalanceCreator.returnFeasibleRebalance(rebalanceCount.unmodifiable(),
                        availableVehicles);
                total_rebalanceCount += (Integer) ((Scalar) Total.of(Tensor.of(feasibleRebalanceCount.flatten(-1)))).number();

                // generate routing instructions for rebalancing vehicles
                Map<VirtualNode, List<Link>> destinationLinks = createvNodeLinksMap();

                // fill rebalancing destinations
                for (int i = 0; i < virtualNetwork.getvLinksCount(); ++i) {
                    VirtualLink virtualLink = this.virtualNetwork.getVirtualLink(i);
                    VirtualNode toNode = virtualLink.getTo();
                    VirtualNode fromNode = virtualLink.getFrom();
                    int numreb = (Integer) (feasibleRebalanceCount.Get(fromNode.index, toNode.index)).number();
                    List<Link> rebalanceTargets = virtualNodeDest.selectLinkSet(toNode, numreb);
                    destinationLinks.get(fromNode).addAll(rebalanceTargets);
                }

                // consistency check: rebalancing destination links must not exceed available
                // vehicles in virtual node
                Map<VirtualNode, List<VehicleLinkPair>> finalAvailableVehicles = availableVehicles;
                GlobalAssert.that(!virtualNetwork.getVirtualNodes().stream()
                        .filter(v -> finalAvailableVehicles.get(v).size() < destinationLinks.get(v).size()).findAny().isPresent());

                // send rebalancing vehicles using the setVehicleRebalance command
                for (VirtualNode virtualNode : destinationLinks.keySet()) {
                    Map<VehicleLinkPair, Link> rebalanceMatching = vehicleDestMatcher.match(availableVehicles.get(virtualNode),
                            destinationLinks.get(virtualNode));
                    rebalanceMatching.keySet().forEach(v -> setVehicleRebalance(v, rebalanceMatching.get(v)));
                }

            }
        }

        // Part II: outside rebalancing periods, permanently assign desitnations to vehicles using
        // bipartite matching
        if (round_now % redispatchPeriod == 0) {
            printVals = HungarianUtils.globalBipartiteMatching(this, () -> getVirtualNodeDivertableNotRebalancingVehicles().values()
                    .stream().flatMap(v -> v.stream()).collect(Collectors.toList()));
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
        public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig){

            AbstractVirtualNodeDest abstractVirtualNodeDest = new KMeansVirtualNodeDest();
            AbstractVehicleDestMatcher abstractVehicleDestMatcher = new HungarBiPartVehicleDestMatcher();

            final File virtualnetworkDir = new File(config.getParams().get("virtualNetworkDirectory"));
            GlobalAssert.that(virtualnetworkDir.isDirectory());
            {
                final File virtualnetworkFile = new File(virtualnetworkDir, "virtualNetwork");
                System.out.println("" + virtualnetworkFile.getAbsoluteFile());
                try {
                    virtualNetwork = VirtualNetworkIO.fromByte(network, virtualnetworkFile);
                } catch (ClassNotFoundException | DataFormatException | IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            return new LPFeedbackLIPDispatcher(config, generatorConfig, travelTime, router, eventsManager, virtualNetwork,
                    abstractVirtualNodeDest, abstractVehicleDestMatcher);
        }
    }
}
