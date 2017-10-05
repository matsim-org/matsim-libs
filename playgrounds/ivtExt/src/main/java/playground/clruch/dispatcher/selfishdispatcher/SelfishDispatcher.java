package playground.clruch.dispatcher.selfishdispatcher;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNode;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Dimensions;
import playground.clruch.dispatcher.core.AVStatus;
import playground.clruch.dispatcher.core.PartitionedDispatcher;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.dispatcher.utils.robotaxirequestmatcher.AbstractRoboTaxiRequestMatcher;
import playground.clruch.dispatcher.utils.robotaxirequestmatcher.RoboTaxiCloseRequestMatcher;
import playground.clruch.dispatcher.utils.virtualnodedestselector.RandomVirtualNodeDest;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataGet;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/** Dispatcher used to create datasets to verify the theory of selfish fleet performance.
 * 
 * @author Claudio Ruch */
public class SelfishDispatcher extends PartitionedDispatcher {
    private final int dispatchPeriod;
    private final Network network;
    private final AbstractRoboTaxiRequestMatcher roboTaxiRequestMatcher;
    private Set<RoboTaxi> waitingTaxis = new HashSet<>();
    private final TravelData travelData;
    private final double fareRatioMultiply;

    private SelfishDispatcher(//
            AVDispatcherConfig config, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            Network network, VirtualNetwork<Link> virtualNetwork, //
            TravelData travelData) {
        super(config, travelTime, router, eventsManager, virtualNetwork);
        SafeConfig safeConfig = SafeConfig.wrap(config);
        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 30);
        this.network = network;
        roboTaxiRequestMatcher = new RoboTaxiCloseRequestMatcher();
        GlobalAssert.that(travelData != null);
        this.travelData = travelData;
        this.fareRatioMultiply = safeConfig.getDouble("fareRatioMultiply", 1.0);
        System.out.println("fare ratio multiply = " + fareRatioMultiply);
    }

    @Override
    public void redispatch(double now) {
        // TestBedDispatcher implemenatation
        final long round_now = Math.round(now);
        if (round_now % dispatchPeriod == 0) {

            /** assign all stay vehicles to an unassigned request */
            roboTaxiRequestMatcher.match(getRoboTaxiSubset(AVStatus.STAY), getUnassignedAVRequests(), //
                    this::setRoboTaxiPickup);

            /** remove all drivewithcustomer taxis from waiting taxi set */
            getRoboTaxiSubset(AVStatus.DRIVEWITHCUSTOMER).forEach(rt -> waitingTaxis.remove(rt));

            /** for remaining stay vehicles, chose a location in A,B to rebalance to */
            for (RoboTaxi robotaxi : getRoboTaxiSubset(AVStatus.STAY)) {
                if (!waitingTaxis.contains(robotaxi)) {
                    VirtualNode<Link> vn = selectRebalanceNode((int) round_now);
                    Link link = (new RandomVirtualNodeDest()).selectLinkSet(vn, 1).get(0);
                    setRoboTaxiRebalance(robotaxi, link);
                    waitingTaxis.add(robotaxi);
                }
            }
        }
    }

    /** @return VirtualNode selected by a selfish agent. */
    private VirtualNode<Link> selectRebalanceNode(int time) {

        Map<VirtualNode<Link>, Double> scores = new HashMap<>();

        for (VirtualNode<Link> virtualNode : virtualNetwork.getVirtualNodes()) {
            double averageFare = calcAverageFare(virtualNode, //
                    getVirtualNodeRequests().get(virtualNode), time);
            GlobalAssert.that(averageFare >= 0.0);
            int openRequests = getVirtualNodeRequests().get(virtualNode).size();
            GlobalAssert.that(openRequests >= 0);
            double waitingTaxis = getVirtualNodeStayVehicles().get(virtualNode).size();
            GlobalAssert.that(waitingTaxis >= 0);
            //
            double score;
            if (waitingTaxis > 0) {
                score = (averageFare * openRequests) / ((double) waitingTaxis);
            } else {
                score = (averageFare * openRequests);
            }

            scores.put(virtualNode, score);
        }

        Entry<VirtualNode<Link>, Double> maxEntry = Collections.max(scores.entrySet(), new ScoreComparator());
        virtualNetwork.getVirtualNodes().forEach(vn -> GlobalAssert.that(scores.get(vn) <= scores.get(maxEntry.getKey())));

        Set<VirtualNode<Link>> maxNodes = scores.entrySet().stream().filter(e -> e.getValue().equals(maxEntry.getValue())).map(e -> e.getKey())
                .collect(Collectors.toSet());

        GlobalAssert.that(maxNodes.size() > 0);
        return maxNodes.stream().skip((int) (maxNodes.size() * Math.random())).findFirst().get();

    }

    private double calcAverageFare(VirtualNode virtualNode, List<AVRequest> requests, int time) {
        double fareRatio = FareRatioCalculator.calcOptLightLoadFareRatio(travelData, time, getRoboTaxis().size());
        fareRatio *= fareRatioMultiply;
        double basicFare = 1;

        if (virtualNode.getIndex() == 0)
            return basicFare * fareRatio;
        else
            return fareRatio;
    }

    @Override
    protected String getInfoLine() {
        return String.format("%s H=%s", //
                super.getInfoLine(), //
                "abc");
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

        public static VirtualNetwork<Link> virtualNetwork;
        public static TravelData travelData;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig) {
            virtualNetwork = VirtualNetworkGet.readDefault(network);
            travelData = TravelDataGet.readDefault(virtualNetwork);
            GlobalAssert.that(virtualNetwork != null);
            GlobalAssert.that(travelData != null);
            return new SelfishDispatcher(config, travelTime, router, eventsManager, network, virtualNetwork, travelData);
        }
    }

}
