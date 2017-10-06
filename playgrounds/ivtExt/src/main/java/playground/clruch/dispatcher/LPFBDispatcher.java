/**
 * Dispatcher implementing the linear program from Pavone, Marco, Stephen Smith, Emilio Frazzoli, and Daniela Rus. 2011.
 * “Load Balancing for Mobility-on-Demand Systems.” In Robotics: Science and Systems VII. doi:10.15607/rss.2011.vii.034.
 * Implemented by Claudio Ruch on 2017, 02, 25
 */

package playground.clruch.dispatcher;

import java.util.List;
import java.util.Map;

import org.gnu.glpk.GLPKConstants;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.queuey.core.networks.VirtualLink;
import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNode;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.RationalScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.Round;
import playground.clruch.dispatcher.core.PartitionedDispatcher;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.dispatcher.utils.AbstractVehicleDestMatcher;
import playground.clruch.dispatcher.utils.BipartiteMatchingUtils;
import playground.clruch.dispatcher.utils.EuclideanDistanceFunction;
import playground.clruch.dispatcher.utils.FeasibleRebalanceCreator;
import playground.clruch.dispatcher.utils.HungarBiPartVehicleDestMatcher;
import playground.clruch.dispatcher.utils.LPVehicleRebalancing;
import playground.clruch.dispatcher.utils.NetworkDistanceFunction;
import playground.clruch.dispatcher.utils.virtualnodedestselector.AbstractVirtualNodeDest;
import playground.clruch.dispatcher.utils.virtualnodedestselector.RandomVirtualNodeDest;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class LPFBDispatcher extends PartitionedDispatcher {
    private final int rebalancingPeriod;
    private final int dispatchPeriod;
    private final AbstractVirtualNodeDest virtualNodeDest;
    private final AbstractVehicleDestMatcher vehicleDestMatcher;
    private final int numRobotaxi;
    private int total_rebalanceCount = 0;
    private Tensor printVals = Tensors.empty();
    private final LPVehicleRebalancing lpVehicleRebalancing;
    private final NetworkDistanceFunction ndf;
    private final Network network;

    public LPFBDispatcher( //
            AVDispatcherConfig config, //
            AVGeneratorConfig generatorConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            Network network, //
            VirtualNetwork<Link> virtualNetwork, //
            AbstractVirtualNodeDest abstractVirtualNodeDest, //
            AbstractVehicleDestMatcher abstractVehicleDestMatcher) {
        super(config, travelTime, router, eventsManager, virtualNetwork);
        virtualNodeDest = abstractVirtualNodeDest;
        vehicleDestMatcher = abstractVehicleDestMatcher;
        numRobotaxi = (int) generatorConfig.getNumberOfVehicles();
        lpVehicleRebalancing = new LPVehicleRebalancing(virtualNetwork);
        SafeConfig safeConfig = SafeConfig.wrap(config);
        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 30);
        rebalancingPeriod = safeConfig.getInteger("rebalancingPeriod", 300);
        this.network = network;
        this.ndf = new NetworkDistanceFunction(network);

    }

    @Override
    public void redispatch(double now) {

        // PART I: rebalance all vehicles periodically
        final long round_now = Math.round(now);

        // System.out.println("TALLY BEG="+Tally.sorted(Tensors.vectorInt(getRoboTaxis().stream().mapToInt(rt -> rt.getAVStatus().ordinal()).toArray())));
        if (round_now % rebalancingPeriod == 0 && round_now > 10) { // needed because of problems with robotaxi
                                                                    // inititalization.

            Map<VirtualNode<Link>, List<AVRequest>> requests = getVirtualNodeRequests();
            // II.i compute rebalancing vehicles and send to virtualNodes
            {
                Map<VirtualNode<Link>, List<RoboTaxi>> availableVehicles = getVirtualNodeDivertableNotRebalancingRoboTaxis();
                int totalAvailable = 0;
                for (List<RoboTaxi> robotaxiList : availableVehicles.values()) {
                    totalAvailable += robotaxiList.size();
                }

                // calculate desired vehicles per vNode
                int num_requests = requests.values().stream().mapToInt(List::size).sum();
                double vi_desired_num = ((numRobotaxi - num_requests) / (double) virtualNetwork.getvNodesCount());
                int vi_desired_numint = (int) Math.floor(vi_desired_num);
                Tensor vi_desiredT = Tensors.vector(i -> RationalScalar.of(vi_desired_numint, 1), virtualNetwork.getvNodesCount());

                // calculate excess vehicles per virtual Node i, where v_i excess = vi_own - c_i =
                // v_i + sum_j (v_ji) - c_i
                Map<VirtualNode<Link>, List<RoboTaxi>> v_ij_reb = getVirtualNodeRebalancingToRoboTaxis();
                Map<VirtualNode<Link>, List<RoboTaxi>> v_ij_cust = getVirtualNodeArrivingWithCustomerRoboTaxis();
                Tensor vi_excessT = Array.zeros(virtualNetwork.getvNodesCount());
                for (VirtualNode<Link> virtualNode : availableVehicles.keySet()) {
                    int viExcessVal = availableVehicles.get(virtualNode).size() + v_ij_reb.get(virtualNode).size() + v_ij_cust.get(virtualNode).size()
                            - requests.get(virtualNode).size();
                    vi_excessT.set(RealScalar.of(viExcessVal), virtualNode.getIndex());
                }

                // solve the linear program with updated right-hand side
                Tensor rhs = vi_desiredT.subtract(vi_excessT);
                Tensor rebalanceCount2 = Tensors.empty();
                if (totalAvailable > 0) {
                    rebalanceCount2 = lpVehicleRebalancing.solveUpdatedLP(rhs, GLPKConstants.GLP_LO);
                } else {
                    rebalanceCount2 = Array.zeros(virtualNetwork.getvNodesCount(), virtualNetwork.getvNodesCount());
                }
                Tensor rebalanceCount = Round.of(rebalanceCount2);
                // assert that solution is integer and does not contain negative values
                GlobalAssert.that(rebalanceCount.flatten(-1).map(Scalar.class::cast).map(s -> s.number().doubleValue()).allMatch(e -> e >= 0));

                // ensure that not more vehicles are sent away than available
                Tensor feasibleRebalanceCount = FeasibleRebalanceCreator.returnFeasibleRebalance(rebalanceCount.unmodifiable(), availableVehicles);
                total_rebalanceCount += (Integer) ((Scalar) Total.of(Tensor.of(feasibleRebalanceCount.flatten(-1)))).number();

                // generate routing instructions for rebalancing vehicles
                Map<VirtualNode<Link>, List<Link>> rebalanceDestinations = virtualNetwork.createVNodeTypeMap();

                // fill rebalancing destinations
                for (int i = 0; i < virtualNetwork.getvLinksCount(); ++i) {
                    VirtualLink<Link> virtualLink = this.virtualNetwork.getVirtualLink(i);
                    VirtualNode<Link> toNode = virtualLink.getTo();
                    VirtualNode<Link> fromNode = virtualLink.getFrom();
                    int numreb = (Integer) (feasibleRebalanceCount.Get(fromNode.getIndex(), toNode.getIndex())).number();
                    List<Link> rebalanceTargets = virtualNodeDest.selectLinkSet(toNode, numreb);
                    rebalanceDestinations.get(fromNode).addAll(rebalanceTargets);
                }

                // consistency check: rebalancing destination links must not exceed available
                // vehicles in virtual node
                Map<VirtualNode<Link>, List<RoboTaxi>> finalAvailableVehicles = availableVehicles;
                GlobalAssert.that(
                        virtualNetwork.getVirtualNodes().stream().allMatch(v -> finalAvailableVehicles.get(v).size() >= rebalanceDestinations.get(v).size()));

                // send rebalancing vehicles using the setVehicleRebalance command
                for (VirtualNode<Link> virtualNode : rebalanceDestinations.keySet()) {
                    Map<RoboTaxi, Link> rebalanceMatching = vehicleDestMatcher.matchLink(availableVehicles.get(virtualNode),
                            rebalanceDestinations.get(virtualNode));
                    rebalanceMatching.keySet().forEach(v -> setRoboTaxiRebalance(v, rebalanceMatching.get(v)));
                }

            }
        }

        // Part II: outside rebalancing periods, permanently assign destinations to vehicles using
        // bipartite matching
        if (round_now % dispatchPeriod == 0) {
            printVals = BipartiteMatchingUtils.executePickup(this::setRoboTaxiPickup, getDivertableRoboTaxis(), getAVRequests(), 
                    new EuclideanDistanceFunction(), network,false);
        }
    }

    @Override
    protected String getInfoLine() {
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

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig) {

            AbstractVirtualNodeDest abstractVirtualNodeDest = new RandomVirtualNodeDest();
            AbstractVehicleDestMatcher abstractVehicleDestMatcher = new HungarBiPartVehicleDestMatcher(new EuclideanDistanceFunction());
            virtualNetwork = VirtualNetworkGet.readDefault(network);

            return new LPFBDispatcher(config, generatorConfig, travelTime, router, eventsManager, network, virtualNetwork, abstractVirtualNodeDest,
                    abstractVehicleDestMatcher);
        }
    }
}
