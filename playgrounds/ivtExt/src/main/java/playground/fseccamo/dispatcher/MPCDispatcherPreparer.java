package playground.fseccamo.dispatcher;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.path.VrpPath;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.jmex.java.JavaContainerSocket;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.io.Export;
import ch.ethz.idsc.tensor.io.Pretty;
import playground.clruch.dispatcher.utils.AbstractVehicleDestMatcher;
import playground.clruch.dispatcher.utils.AbstractVirtualNodeDest;
import playground.clruch.dispatcher.utils.HungarBiPartVehicleDestMatcher;
import playground.clruch.dispatcher.utils.KMeansVirtualNodeDest;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.router.InstantPathFactory;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class MPCDispatcherPreparer extends BaseMpcDispatcher {
    final AbstractVirtualNodeDest virtualNodeDest;
    final AbstractVehicleDestMatcher vehicleDestMatcher;
    final Map<VirtualLink, Double> travelTimes;
    final InstantPathFactory instantPathFactory;

    JavaContainerSocket javaContainerSocket;

    public MPCDispatcherPreparer( //
            AVDispatcherConfig config, //
            AVGeneratorConfig generatorConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager, //
            VirtualNetwork virtualNetwork, //
            AbstractVirtualNodeDest abstractVirtualNodeDest, //
            AbstractVehicleDestMatcher abstractVehicleDestMatcher, //
            Map<VirtualLink, Double> travelTimesIn) {
        super(config, travelTime, parallelLeastCostPathCalculator, eventsManager, virtualNetwork);
        this.virtualNodeDest = abstractVirtualNodeDest;
        this.instantPathFactory = new InstantPathFactory(parallelLeastCostPathCalculator, travelTime);
        this.vehicleDestMatcher = abstractVehicleDestMatcher;
        travelTimes = travelTimesIn;
        publishPeriod = 0;
    }

    @Override
    public void redispatch(double now) {
        final int m = virtualNetwork.getvLinksCount();
        publishPeriod = 0;
        final long round_now = Math.round(now);
        if (round_now == 107000) {

            {
                Map<AVRequest, MpcRequest> mpcRequestsMap = new LinkedHashMap<>();
                for (AVRequest avRequest : getAVRequests()) { // if request has been seen/computed before
                    final VirtualNode vnFrom = virtualNetwork.getVirtualNode(avRequest.getFromLink());
                    final VirtualNode vnTo = virtualNetwork.getVirtualNode(avRequest.getToLink());
                    GlobalAssert.that(vnFrom.equals(vnTo) == (vnFrom.index == vnTo.index));
                    if (vnFrom.equals(vnTo))
                        mpcRequestsMap.put(avRequest, new MpcRequest(avRequest, m, vnFrom));
                    else {
                        VirtualNode fromIn = null;
                        VrpPath vrpPath = instantPathFactory.getVrpPathWithTravelData( //
                                avRequest.getFromLink(), avRequest.getToLink(), now); // TODO perhaps add expected waitTime
                        for (Link link : vrpPath) {
                            final VirtualNode toIn = virtualNetwork.getVirtualNode(link);
                            if (fromIn == null)
                                fromIn = toIn;
                            else //
                            if (fromIn != toIn) { // found adjacent node
                                VirtualLink virtualLink = virtualNetwork.getVirtualLink(fromIn, toIn);
                                mpcRequestsMap.put(avRequest, new MpcRequest(avRequest, virtualLink));
                                break;
                            }
                        }
                    }
                }

                Tensor requestSchedule = Tensors.empty();
                for (Entry<AVRequest, MpcRequest> entry : mpcRequestsMap.entrySet()) {
                    MpcRequest mpcRequest = entry.getValue();
                    requestSchedule.append(Tensors.vector( //
                            Math.round(entry.getKey().getSubmissionTime()), //
                            mpcRequest.nodeFrom.index + 1, //
                            mpcRequest.nodeTo.index + 1));
                }
                System.out.println("next node travel index");
                // System.out.println(Pretty.of(requestSchedule));
                System.out.println(Dimensions.of(requestSchedule));
                try {
                    Export.of(getRequestScheduleFileNext(), requestSchedule);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }

            {
                Tensor requestSchedule = Tensors.empty();
                for (AVRequest avRequest : getAVRequests()) { // all current requests
                    final VirtualNode vnFrom = virtualNetwork.getVirtualNode(avRequest.getFromLink());
                    final VirtualNode vnTo = virtualNetwork.getVirtualNode(avRequest.getToLink());
                    requestSchedule.append(Tensors.vector( //
                            Math.round(avRequest.getSubmissionTime()), // 
                            vnFrom.index + 1, // 
                            vnTo.index + 1));
                }

                System.out.println("global travel index");
                // System.out.println(Pretty.of(requestSchedule));
                System.out.println(Dimensions.of(requestSchedule));
                try {
                    Export.of(getRequestScheduleFileGlobal(), requestSchedule);
                } catch (Exception exception) {
                    exception.printStackTrace();
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
        public static Map<VirtualLink, Double> travelTimes;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig) {
            AbstractVirtualNodeDest abstractVirtualNodeDest = new KMeansVirtualNodeDest();
            AbstractVehicleDestMatcher abstractVehicleDestMatcher = new HungarBiPartVehicleDestMatcher();
            final File virtualnetworkDir = new File(config.getParams().get("virtualNetworkDirectory"));
            GlobalAssert.that(virtualnetworkDir.isDirectory());
            virtualNetwork = VirtualNetworkGet.readDefault(network);
            return new MPCDispatcherPreparer( //
                    config, generatorConfig, travelTime, router, eventsManager, //
                    virtualNetwork, abstractVirtualNodeDest, abstractVehicleDestMatcher, travelTimes);
        }
    }
}
