// code by francesco, jph, clruch
package playground.fseccamo.dispatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPath;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;

import ch.ethz.idsc.queuey.core.networks.VirtualLink;
import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNode;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.matsim.av.config.AVDispatcherConfig;
import ch.ethz.matsim.av.passenger.AVRequest;
import ch.ethz.matsim.av.plcpc.ParallelLeastCostPathCalculator;
import playground.clruch.dispatcher.core.AVStatus;
import playground.clruch.dispatcher.core.PartitionedDispatcher;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.router.InstantPathFactory;

abstract class BaseMpcDispatcher extends PartitionedDispatcher {
    protected final int samplingPeriod;
    final InstantPathFactory instantPathFactory;
    final Map<AVRequest, Integer> requestVectorIndexMap = new HashMap<>();

    BaseMpcDispatcher( //
            Config config, //
            AVDispatcherConfig avconfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager, //
            VirtualNetwork<Link> virtualNetwork) {
        super(config, avconfig, travelTime, parallelLeastCostPathCalculator, eventsManager, virtualNetwork);
        this.instantPathFactory = new InstantPathFactory(parallelLeastCostPathCalculator, travelTime);

        // period between calls to MPC
        samplingPeriod = Integer.parseInt(avconfig.getParams().get("samplingPeriod"));
    }

    Map<VirtualNode<Link>, List<RoboTaxi>> getDivertableNotRebalancingNotPickupVehicles() {
        Map<VirtualNode<Link>, List<RoboTaxi>> returnMap = virtualNetwork.createVNodeTypeMap();
        Map<VirtualNode<Link>, List<RoboTaxi>> allVehicles = getVirtualNodeDivertableNotRebalancingRoboTaxis();
        for (VirtualNode<Link> vn : allVehicles.keySet()) {
            for (RoboTaxi robotaxi : allVehicles.get(vn)) {
                if (!robotaxi.getAVStatus().equals(AVStatus.DRIVETOCUSTOMER)) {
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
                final VirtualNode<Link> vnFrom = virtualNetwork.getVirtualNode(avRequest.getFromLink());
                final VirtualNode<Link> vnTo = virtualNetwork.getVirtualNode(avRequest.getToLink());
                GlobalAssert.that(vnFrom.equals(vnTo) == (vnFrom.getIndex() == vnTo.getIndex()));
                if (vnFrom.equals(vnTo)) {
                    // self loop
                    requestVectorIndexMap.put(avRequest, m + vnFrom.getIndex());
                } else {
                    // non-self loop
                    boolean success = false;
                    VrpPath vrpPath = instantPathFactory.getVrpPathWithTravelData( //
                            avRequest.getFromLink(), avRequest.getToLink(), now);
                    // TODO perhaps add expected waitTime
                    VirtualNode<Link> fromIn = null;
                    for (Link link : vrpPath) {
                        final VirtualNode<Link> toIn = virtualNetwork.getVirtualNode(link);
                        if (fromIn == null)
                            fromIn = toIn;
                        else //
                        if (fromIn != toIn) { // found adjacent node
                            VirtualLink<Link> virtualLink = virtualNetwork.getVirtualLink(fromIn, toIn);
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