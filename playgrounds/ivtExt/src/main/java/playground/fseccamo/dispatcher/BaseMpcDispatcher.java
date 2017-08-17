package playground.fseccamo.dispatcher;


import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPath;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.sca.Increment;
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
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

abstract class BaseMpcDispatcher extends PartitionedDispatcher {
	protected final int samplingPeriod;
	final InstantPathFactory instantPathFactory;

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








	// requests that haven't received a pickup order yet
	final Map<AVRequest, MpcRequest> mpcRequestsMap = new HashMap<>();



	Map<VirtualNode, List<RoboTaxi>> getDivertableNotRebalancingNotPickupVehicles() {
	    Map<VirtualNode, List<RoboTaxi>> returnMap = virtualNetwork.createVNodeTypeMap();
	    Map<VirtualNode, List<RoboTaxi>> allVehicles = getVirtualNodeDivertablenotRebalancingRoboTaxis();
	    for(VirtualNode vn : allVehicles.keySet()){
	        for(RoboTaxi robotaxi : allVehicles.get(vn)){
	            if(!robotaxi.getAVStatus().equals(AVStatus.DRIVETOCUSTMER)){
	                returnMap.get(vn).add(robotaxi);
	            }
	        }
	    }
	    
	    return returnMap;
	}

//	/**
//	 * @return requests that have received a pickup order (and are therefore
//	 *         contained in considerItDone)
//	 */
//	Map<Link, List<AVRequest>> override_getAVRequestsAtLinks() {
//		// intentionally not parallel to guarantee ordering of requests
//		return getAVRequests().stream() //
//				.filter(avRequest -> getMatchings().containsKey(avRequest)) //
//				.collect(Collectors.groupingBy(AVRequest::getFromLink));
//	}



	/**
	 * function computes mpcRequest objects for unserved requests
	 * 
	 * @param now
	 */
	void manageIncomingRequests(double now) {
		final int m = virtualNetwork.getvLinksCount();

		for (AVRequest avRequest : getUnassignedAVRequests()) // all current
															// requests
			// only count request that haven't received a pickup order yet
			// or haven't been processed yet, i.e. if request has been
			// seen/computed before
			if (!mpcRequestsMap.containsKey(avRequest)) {
				// check if origin and dest are from same virtualNode
				final VirtualNode vnFrom = virtualNetwork.getVirtualNode(avRequest.getFromLink());
				final VirtualNode vnTo = virtualNetwork.getVirtualNode(avRequest.getToLink());
				GlobalAssert.that(vnFrom.equals(vnTo) == (vnFrom.index == vnTo.index));
				if (vnFrom.equals(vnTo)) {
					// self loop
					mpcRequestsMap.put(avRequest, new MpcRequest(avRequest, m, vnFrom));
				} else {
					// non-self loop
					boolean success = false;
					VrpPath vrpPath = instantPathFactory.getVrpPathWithTravelData( //
							avRequest.getFromLink(), avRequest.getToLink(), now); // TODO
																					// perhaps
																					// add
																					// expected
																					// waitTime
					VirtualNode fromIn = null;
					for (Link link : vrpPath) {
						final VirtualNode toIn = virtualNetwork.getVirtualNode(link);
						if (fromIn == null)
							fromIn = toIn;
						else //
						if (fromIn != toIn) { // found adjacent node
							VirtualLink virtualLink = virtualNetwork.getVirtualLink(fromIn, toIn);
							mpcRequestsMap.put(avRequest, new MpcRequest(avRequest, virtualLink));
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
	
    final Map<VirtualNode, List<RoboTaxi>> getVirtualNodeStayVehicles(){
        Map<VirtualNode,List<RoboTaxi>> returnMap = virtualNetwork.createVNodeTypeMap();
        for(RoboTaxi robotaxi :getRoboTaxis()){
            if(robotaxi.isInStayTask()){
                VirtualNode vnode = virtualNetwork.getVirtualNode(robotaxi.getDivertableLocation());
                returnMap.get(vnode).add(robotaxi);
            }
        }
        return returnMap;
    };
	
	

}



// @Override
// protected final void protected_setAcceptRequest_postProcessing(AVVehicle
// avVehicle, AVRequest avRequest) {
// boolean succPR = pendingRequests.remove(avRequest);
// GlobalAssert.that(succPR);
// {
// AVVehicle former = matchings.remove(avRequest);
// GlobalAssert.that(former != null);
// }
// {
// MpcRequest former = mpcRequestsMap.remove(avRequest);
// GlobalAssert.that(former != null);
// }
// }

//Map<VirtualNode, List<VehicleLinkPair>> getDivertableNotRebalancingNotPickupVehicles() {
//  Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeDivertableNotRebalancingVehicles();
//  Set<AVVehicle> pickupBusy = getMatchings().values().stream().collect(Collectors.toSet());
//  Map<VirtualNode, List<VehicleLinkPair>> map = new HashMap<>();
//  for (Entry<VirtualNode, List<VehicleLinkPair>> entry : availableVehicles.entrySet()) {
//      List<VehicleLinkPair> list = entry.getValue().stream() //
//              .filter(vlp -> !pickupBusy.contains(vlp.avVehicle)) //
//              .collect(Collectors.toList());
//      map.put(entry.getKey(), list);
//  }
//  return map;
//}
