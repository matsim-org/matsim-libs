package playground.fseccamo.dispatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPath;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.dispatcher.core.AVStatus;
import playground.clruch.dispatcher.core.PartitionedDispatcher;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.router.InstantPathFactory;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

abstract class BaseMpcDispatcher extends PartitionedDispatcher {
    protected final int samplingPeriod;
    final InstantPathFactory instantPathFactory;
    final Map<AVRequest, Integer> requestVectorIndexMap = new HashMap<>();

    BaseMpcDispatcher( //
            AVDispatcherConfig config, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager, //
            VirtualNetwork virtualNetwork) {
        super(config, travelTime, parallelLeastCostPathCalculator, eventsManager, virtualNetwork);
        this.instantPathFactory = new InstantPathFactory(parallelLeastCostPathCalculator, travelTime);

        // period between calls to MPC
        samplingPeriod = Integer.parseInt(config.getParams().get("samplingPeriod"));
    }

    Map<VirtualNode, List<RoboTaxi>> getDivertableNotRebalancingNotPickupVehicles() {
        Map<VirtualNode, List<RoboTaxi>> returnMap = virtualNetwork.createVNodeTypeMap();
        Map<VirtualNode, List<RoboTaxi>> allVehicles = getVirtualNodeDivertableNotRebalancingRoboTaxis();
        for (VirtualNode vn : allVehicles.keySet()) {
            for (RoboTaxi robotaxi : allVehicles.get(vn)) {
                if (!robotaxi.getAVStatus().equals(AVStatus.DRIVETOCUSTMER)) {
                    returnMap.get(vn).add(robotaxi);
                }
            }
        }

        return returnMap;
    }

    /** function computes mpcRequest objects for unserved requests
     * 
     * @param now */
    void manageIncomingRequests(double now) {
        final int m = virtualNetwork.getvLinksCount();

        for (AVRequest avRequest : getUnassignedAVRequests())
            // all current requests only count request that haven't received a pickup order yet
            // or haven't been processed yet, i.e. if request has been seen/computed before
            if (!requestVectorIndexMap.containsKey(avRequest)) {

                // check if origin and dest are from same virtualNode
                final VirtualNode vnFrom = virtualNetwork.getVirtualNode(avRequest.getFromLink());
                final VirtualNode vnTo = virtualNetwork.getVirtualNode(avRequest.getToLink());
                GlobalAssert.that(vnFrom.equals(vnTo) == (vnFrom.index == vnTo.index));
                if (vnFrom.equals(vnTo)) {
                    // self loop
                    requestVectorIndexMap.put(avRequest, m + vnFrom.index);
                } else {
                    // non-self loop
                    boolean success = false;
                    VrpPath vrpPath = instantPathFactory.getVrpPathWithTravelData( //
                            avRequest.getFromLink(), avRequest.getToLink(), now);
                    // TODO perhaps add expected waitTime
                    VirtualNode fromIn = null;
                    for (Link link : vrpPath) {
                        final VirtualNode toIn = virtualNetwork.getVirtualNode(link);
                        if (fromIn == null)
                            fromIn = toIn;
                        else //
                        if (fromIn != toIn) { // found adjacent node
                            VirtualLink virtualLink = virtualNetwork.getVirtualLink(fromIn, toIn);
                            requestVectorIndexMap.put(avRequest, virtualLink.getIndex());
                            success = true;
                            break;
                        }
                    }
                    if (!success) {
                        new RuntimeException("VirtualLink of request could not be identified") //
                                .printStackTrace();
                    }
                }
            }
    }



}