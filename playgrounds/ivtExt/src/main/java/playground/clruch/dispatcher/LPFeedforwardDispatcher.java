/**
 * Dispatcher implementing the linear program from
 * Pavone, Marco, Stephen Smith, Emilio Frazzoli, and Daniela Rus. 2011.
 * “Load Balancing for Mobility-on-Demand Systems.” In Robotics: Science and Systems VII. doi:10.15607/rss.2011.vii.034.
 * <p>
 * <p>
 * Implemented by Claudio Ruch on 2017, 02, 25
 */

package playground.clruch.dispatcher;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.red.Total;
import playground.clruch.dispatcher.core.PartitionedDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.AbstractRequestSelector;
import playground.clruch.dispatcher.utils.AbstractVehicleDestMatcher;
import playground.clruch.dispatcher.utils.AbstractVirtualNodeDest;
import playground.clruch.dispatcher.utils.FeasibleRebalanceCreator;
import playground.clruch.dispatcher.utils.HungarBiPartVehicleDestMatcher;
import playground.clruch.dispatcher.utils.InOrderOfArrivalMatcher;
import playground.clruch.dispatcher.utils.KMeansVirtualNodeDest;
import playground.clruch.dispatcher.utils.LPVehicleRebalancing;
import playground.clruch.dispatcher.utils.OldestRequestSelector;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkIO;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataIO;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class LPFeedforwardDispatcher extends PartitionedDispatcher {
    public final int redispatchPeriod;
    final AbstractVirtualNodeDest virtualNodeDest;
    final AbstractRequestSelector requestSelector;
    final AbstractVehicleDestMatcher vehicleDestMatcher;
    final int numberOfAVs;
    private int total_rebalanceCount = 0;
    private final int nVNodes;
    private final int nVLinks;
    Tensor printVals = Tensors.empty();
    TravelData travelData;
    Tensor rebalancingRate;
    Tensor rebalanceCount;
    Tensor rebalanceCountInteger;
    LPVehicleRebalancing lpVehicleRebalancing;

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
            TravelData arrivalInformationIn) {
        super(config, travelTime, router, eventsManager, virtualNetwork);
        this.virtualNodeDest = abstractVirtualNodeDest;
        this.requestSelector = abstractRequestSelector;
        this.vehicleDestMatcher = abstractVehicleDestMatcher;
        numberOfAVs = (int) generatorConfig.getNumberOfVehicles();
        redispatchPeriod = Integer.parseInt(config.getParams().get("redispatchPeriod"));
        travelData = arrivalInformationIn;
        nVNodes = virtualNetwork.getvNodesCount();
        nVLinks = virtualNetwork.getvLinksCount();
        rebalanceCount = Array.zeros(nVNodes, nVNodes);
        rebalanceCountInteger = Array.zeros(nVNodes, nVNodes);
        lpVehicleRebalancing = new LPVehicleRebalancing(virtualNetwork);
    }

    @Override
    public void redispatch(double now) {
        // PART 0: match vehicles at a customer link
        new InOrderOfArrivalMatcher(this::setAcceptRequest) //
                .match(getStayVehicles(), getAVRequestsAtLinks());
        final long round_now = Math.round(now);


        // permanently rebalance vehicles according to the rates output by the LP
        if (round_now % redispatchPeriod == 0) {
            rebalancingRate = travelData.getAlphaijforTime((int)round_now);
            
            // update rebalance count using current rate
            rebalanceCount = rebalanceCount.add(rebalancingRate.multiply(RealScalar.of(redispatchPeriod)));

            {
                List<Integer> dims =Dimensions.of(rebalanceCount); 
            // redispatch values > 0 and remove from rebalanceCount
            for (int i = 0; i < dims.get(0); ++i) {
                for (int j = 0; j < dims.get(1); ++j) {
//                    double toSend = rebalanceCount.Get(i, j).number().doubleValue();
                    if (Scalars.lessThan(RealScalar.ONE, rebalanceCount.Get(i, j))) {
//                    if (toSend > 1.0) {
                        rebalanceCountInteger.set(RealScalar.ONE, i, j);
                        rebalanceCount.set(s->s.subtract(RealScalar.ONE), i, j);
                    }
                }
            }
            }

            // ensure that not more vehicles are sent away than available
            Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeDivertableNotRebalancingVehicles();
            Tensor feasibleRebalanceCount = FeasibleRebalanceCreator.returnFeasibleRebalance(rebalanceCountInteger.unmodifiable(), availableVehicles);
            total_rebalanceCount += (Integer) ((Scalar) Total.of(Tensor.of(feasibleRebalanceCount.flatten(-1)))).number();

            // generate routing instructions for rebalancing vehicles
            Map<VirtualNode, List<Link>> destinationLinks = createvNodeLinksMap();

            // fill rebalancing destinations
            for (int i = 0; i < nVLinks; ++i) {
                VirtualLink virtualLink = this.virtualNetwork.getVirtualLink(i);
                VirtualNode toNode = virtualLink.getTo();
                VirtualNode fromNode = virtualLink.getFrom();
                int numreb = (Integer) (feasibleRebalanceCount.Get(fromNode.index, toNode.index)).number();
                List<Link> rebalanceTargets = virtualNodeDest.selectLinkSet(toNode, numreb);
                destinationLinks.get(fromNode).addAll(rebalanceTargets);
            }

            // consistency check: rebalancing destination links must not exceed
            // available vehicles in virtual node
            Map<VirtualNode, List<VehicleLinkPair>> finalAvailableVehicles = availableVehicles;
            GlobalAssert.that(!virtualNetwork.getVirtualNodes().stream()
                    .filter(v -> finalAvailableVehicles.get(v).size() < destinationLinks.get(v).size()).findAny().isPresent());

            // send rebalancing vehicles using the setVehicleRebalance command
            for (VirtualNode virtualNode : destinationLinks.keySet()) {
                Map<VehicleLinkPair, Link> rebalanceMatching = vehicleDestMatcher.match(availableVehicles.get(virtualNode),
                        destinationLinks.get(virtualNode));
                rebalanceMatching.keySet().forEach(v -> setVehicleRebalance(v, rebalanceMatching.get(v)));
            }

            // reset vector
            rebalanceCountInteger = Array.zeros(nVNodes, nVNodes);
//                    Tensors.matrix((i, j) -> RealScalar.of(0.0), nVNodes, nVNodes);
        }

        // assign destinations to vehicles using bipartite matching
        printVals = HungarianUtils.globalBipartiteMatching(this,
                () -> getVirtualNodeDivertableNotRebalancingVehicles().values() //
                .stream().flatMap(v -> v.stream()).collect(Collectors.toList()));
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

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig) {

            AbstractVirtualNodeDest abstractVirtualNodeDest = new KMeansVirtualNodeDest();
            AbstractRequestSelector abstractRequestSelector = new OldestRequestSelector();
            AbstractVehicleDestMatcher abstractVehicleDestMatcher = new HungarBiPartVehicleDestMatcher();

            final File virtualnetworkDir = new File(config.getParams().get("virtualNetworkDirectory"));
            GlobalAssert.that(virtualnetworkDir.isDirectory());
            {
                final File virtualnetworkFile = new File(virtualnetworkDir, "virtualNetwork");
                GlobalAssert.that(virtualnetworkFile.isFile());
                try {
                    virtualNetwork = VirtualNetworkIO.fromByte(network, virtualnetworkFile);
                } catch (ClassNotFoundException | DataFormatException | IOException e) {
                    e.printStackTrace();
                }
            }

            TravelData travelData = null;
            try {
                travelData = TravelDataIO.fromByte(network, virtualNetwork, new File(virtualnetworkDir, "travelData"));
            } catch (ClassNotFoundException | DataFormatException | IOException e) {
                System.out.println("problem reading travelData");
                e.printStackTrace();
            }

            return new LPFeedforwardDispatcher(config, generatorConfig, travelTime, router, eventsManager, virtualNetwork, abstractVirtualNodeDest,
                    abstractRequestSelector, abstractVehicleDestMatcher, travelData);
        }
    }
}