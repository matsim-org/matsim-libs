/**
 * Dispatcher implementing the linear program from
 * Pavone, Marco, Stephen Smith, Emilio Frazzoli, and Daniela Rus. 2011.
 * “Load Balancing for Mobility-on-Demand Systems.” In Robotics: Science and Systems VII. doi:10.15607/rss.2011.vii.034.
 * <p>
 * <p>
 * Implemented by Claudio Ruch on 2017, 02, 25
 */

package playground.clruch.dispatcher;

import java.util.List;
import java.util.Map;

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
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.Floor;
import playground.clruch.dispatcher.core.PartitionedDispatcher;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.dispatcher.utils.AbstractVehicleDestMatcher;
import playground.clruch.dispatcher.utils.BipartiteMatchingUtils;
import playground.clruch.dispatcher.utils.EuclideanDistanceFunction;
import playground.clruch.dispatcher.utils.FeasibleRebalanceCreator;
import playground.clruch.dispatcher.utils.HungarBiPartVehicleDestMatcher;
import playground.clruch.dispatcher.utils.virtualnodedestselector.AbstractVirtualNodeDest;
import playground.clruch.dispatcher.utils.virtualnodedestselector.KMeansVirtualNodeDest;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataGet;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class LPFFDispatcher extends PartitionedDispatcher {
    public final int dispatchPeriod;
    public final int rebalancingPeriod;
    final AbstractVirtualNodeDest virtualNodeDest;
    final AbstractVehicleDestMatcher vehicleDestMatcher;
    private int total_rebalanceCount = 0;
    private final int nVNodes;
    private final int nVLinks;
    private final Network network; 
    Tensor printVals = Tensors.empty();
    TravelData travelData;
    Tensor rebalancingRate;
    Tensor rebalanceCount;
    Tensor rebalanceCountInteger;
    

    public LPFFDispatcher( //
            AVDispatcherConfig config, //
            AVGeneratorConfig generatorConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            Network network,//
            VirtualNetwork<Link> virtualNetwork, //
            AbstractVirtualNodeDest abstractVirtualNodeDest, //
            AbstractVehicleDestMatcher abstractVehicleDestMatcher, //
            TravelData travelData) {
        super(config, travelTime, router, eventsManager, virtualNetwork);
        virtualNodeDest = abstractVirtualNodeDest;
        vehicleDestMatcher = abstractVehicleDestMatcher;
        this.travelData = travelData;
        this.network = network;
        nVNodes = virtualNetwork.getvNodesCount();
        nVLinks = virtualNetwork.getvLinksCount();
        rebalanceCount = Array.zeros(nVNodes, nVNodes);
        rebalanceCountInteger = Array.zeros(nVNodes, nVNodes);
        SafeConfig safeConfig = SafeConfig.wrap(config);
        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 30);
        rebalancingPeriod = safeConfig.getInteger("rebalancingPeriod", 30);
    }

    @Override
    public void redispatch(double now) {
        final long round_now = Math.round(now);

        // Part I: permanently rebalance vehicles according to the rates output by the LP
        if (round_now % rebalancingPeriod == 0) {
            rebalancingRate = travelData.getAlphaijPSFforTime((int) round_now);

            // update rebalance count using current rate
            rebalanceCount = rebalanceCount.add(rebalancingRate.multiply(RealScalar.of(rebalancingPeriod)));
            rebalanceCountInteger = Floor.of(rebalanceCount);
            rebalanceCount = rebalanceCount.subtract(rebalanceCountInteger);
            
            

            // ensure that not more vehicles are sent away than available
            Map<VirtualNode<Link>, List<RoboTaxi>> availableVehicles = getVirtualNodeDivertableNotRebalancingRoboTaxis();
            Tensor feasibleRebalanceCount = FeasibleRebalanceCreator.returnFeasibleRebalance(rebalanceCountInteger.unmodifiable(), availableVehicles);
            total_rebalanceCount += (Integer) ((Scalar) Total.of(Tensor.of(feasibleRebalanceCount.flatten(-1)))).number();

            // generate routing instructions for rebalancing vehicles
            Map<VirtualNode<Link>, List<Link>> destinationLinks = virtualNetwork.createVNodeTypeMap();

            // fill rebalancing destinations
            for (int i = 0; i < nVLinks; ++i) {
                VirtualLink virtualLink = this.virtualNetwork.getVirtualLink(i);
                VirtualNode toNode = virtualLink.getTo();
                VirtualNode fromNode = virtualLink.getFrom();
                int numreb = (Integer) (feasibleRebalanceCount.Get(fromNode.getIndex(), toNode.getIndex())).number();
                List<Link> rebalanceTargets = virtualNodeDest.selectLinkSet(toNode, numreb);
                destinationLinks.get(fromNode).addAll(rebalanceTargets);
            }

            // consistency check: rebalancing destination links must not exceed available vehicles
            // in virtual node
            GlobalAssert.that(!virtualNetwork.getVirtualNodes().stream().filter(v -> availableVehicles.get(v).size() < destinationLinks.get(v).size()).findAny()
                    .isPresent());

            // send rebalancing vehicles using the setVehicleRebalance command
            for (VirtualNode virtualNode : destinationLinks.keySet()) {
                Map<RoboTaxi, Link> rebalanceMatching = vehicleDestMatcher.matchLink(availableVehicles.get(virtualNode), destinationLinks.get(virtualNode));
                rebalanceMatching.keySet().forEach(v -> setRoboTaxiRebalance(v, rebalanceMatching.get(v)));
            }

            // reset vector
            rebalanceCountInteger = Array.zeros(nVNodes, nVNodes);
        }

        // Part II: outside rebalancing periods, permanently assign destinations to vehicles using
        // bipartite matching
        if (round_now % dispatchPeriod == 0) {
            printVals = BipartiteMatchingUtils.executePickup(this::setRoboTaxiPickup, getDivertableRoboTaxis(), getAVRequests(),//
                    new EuclideanDistanceFunction(),network,false);
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

            AbstractVirtualNodeDest abstractVirtualNodeDest = new KMeansVirtualNodeDest();
            AbstractVehicleDestMatcher abstractVehicleDestMatcher = new HungarBiPartVehicleDestMatcher(new EuclideanDistanceFunction());

            virtualNetwork = VirtualNetworkGet.readDefault(network);
            TravelData travelData = TravelDataGet.readDefault(virtualNetwork);

            return new LPFFDispatcher(config, generatorConfig, travelTime, router, eventsManager, network,virtualNetwork, abstractVirtualNodeDest,
                    abstractVehicleDestMatcher, travelData);
        }
    }
}