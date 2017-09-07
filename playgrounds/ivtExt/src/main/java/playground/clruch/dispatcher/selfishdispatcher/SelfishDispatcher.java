package playground.clruch.dispatcher.selfishdispatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.dispatcher.core.AVStatus;
import playground.clruch.dispatcher.core.PartitionedDispatcher;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.dispatcher.utils.robotaxirequestmatcher.AbstractRoboTaxiRequestMatcher;
import playground.clruch.dispatcher.utils.robotaxirequestmatcher.RoboTaxiCloseRequestMatcher;
import playground.clruch.dispatcher.utils.virtualnodedestselector.RandomVirtualNodeDest;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.netdata.VirtualNode;
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

    private SelfishDispatcher(//
            AVDispatcherConfig config, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            Network network, VirtualNetwork virtualNetwork) {
        super(config, travelTime, router, eventsManager, virtualNetwork);
        SafeConfig safeConfig = SafeConfig.wrap(config);
        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 30);
        this.network = network;
        roboTaxiRequestMatcher = new RoboTaxiCloseRequestMatcher();
    }

    @Override
    public void redispatch(double now) {
        // TestBedDispatcher implemenatation
        final long round_now = Math.round(now);
        if (round_now % dispatchPeriod == 0) {

            /** assign all stay vehicles to an unassigned request */
            roboTaxiRequestMatcher.match(getRoboTaxiSubset(AVStatus.STAY), getUnassignedAVRequests(), //
                    this::setRoboTaxiPickup);

            /** for remaining stay vehicles, chose a location in A,B to rebalance to */
            for (RoboTaxi robotaxi : getRoboTaxiSubset(AVStatus.STAY)) {
                VirtualNode vn = selectRebalanceNode();
                Link link = (new RandomVirtualNodeDest()).selectLinkSet(vn, 1).get(0);
                setRoboTaxiRebalance(robotaxi, link);
            }
        }
    }

    /** @return VirtualNode selected by a selfish agent. */
    private VirtualNode selectRebalanceNode() {
        Map<VirtualNode, Double> scores = new HashMap<>();

        for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
            double averageFare = calcAverageFare(virtualNode, //
                    getVirtualNodeRequests().get(virtualNode));
            double openRequests = getVirtualNodeRequests().get(virtualNode).size();
            double waitingTaxis = getVirtualNodeStayVehicles().get(virtualNode).size();
            //
            double score = waitingTaxis/(averageFare * openRequests);
            scores.put(virtualNode, score);
        }

        VirtualNode vnOpt = scores.entrySet().stream().sorted(new ScoreComparator()).findFirst().get().getKey();
        virtualNetwork.getVirtualNodes().forEach(vn -> GlobalAssert.that(scores.get(vn) <= scores.get(vnOpt)));
        return vnOpt;

    }
    
    
    
    private double calcAverageFare(VirtualNode virtualNode, List<AVRequest> requests){
        //TODO implement this. 
        return 1.0;
    }
    
    

    @Override
    protected String getInfoLine() {
        return String.format("%s AT=%5d", //
                super.getInfoLine());
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
            virtualNetwork = VirtualNetworkGet.readDefault(network);
            return new SelfishDispatcher(config, travelTime, router, eventsManager, network, virtualNetwork);
        }
    }

}
