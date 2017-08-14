package playground.fseccamo.dispatcher;

import java.awt.Robot;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import playground.sebhoerl.avtaxi.data.AVVehicle;
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

		samplingPeriod = Integer.parseInt(config.getParams().get("samplingPeriod")); // period
																						// between
																						// calls
																						// to
																						// MPC
	}

	private static final int NOLINKFOUND = -1;

	protected Tensor countVehiclesPerVLink(List<RoboTaxi> map) {
		final int m = virtualNetwork.getvLinksCount();
		final Tensor vector = Array.zeros(m + virtualNetwork.getvNodesCount()); // self
																				// loops
		for (RoboTaxi entry : map) { // for each
																// vehicle
			final AVVehicle avVehicle = entry.getAVVehicle();
			final Link current = entry.getCurrentDriveDestination();
			Task task = avVehicle.getSchedule().getCurrentTask();
			int vli = -1;
			if (task instanceof AVPickupTask) {
				List<? extends Task> list = avVehicle.getSchedule().getTasks();
				int taskIndex = list.indexOf(task); //
				task = list.get(taskIndex + 1); // immediately try next
												// condition with task as
												// AVDriveTask
			} // <- do not put "else" here
			if (task instanceof AVDriveTask) {
				vli = getVirtualLinkOfVehicle((AVDriveTask) task, current);
				if (NOLINKFOUND == vli) {
					// if no transition between virtual nodes is detected, ...
					// then the vehicle is considered to remain within current
					// virtual node
					VirtualNode vnode = virtualNetwork.getVirtualNode(current);
					vector.set(Increment.ONE, m + vnode.index); // self loop
				} else {
					vector.set(Increment.ONE, vli);
				}
			}
			if (task instanceof AVDropoffTask) {
				// consider the vehicle on the self loop of current virtual node
				VirtualNode vnode = virtualNetwork.getVirtualNode(current);
				vector.set(Increment.ONE, m + vnode.index); // self loop
			}
		}
		return vector;
	}

	/**
	 * @param driveTask
	 * @param current
	 * @return virtual link index on which vehicle of drive task is traversing
	 *         on, or NOLINKFOUND if such link cannot be identified
	 */
	private int getVirtualLinkOfVehicle(AVDriveTask driveTask, Link current) {
		return getNextVirtualLinkOnPath(driveTask.getPath(), current);
	}

	int getNextVirtualLinkOnPath(VrpPath vrpPath, Link current) {
		VirtualNode fromIn = null;
		VirtualNode toIn = null;
		boolean fused = false;
		for (Link link : vrpPath) {
			fused |= link == current;
			if (fused) {
				if (fromIn == null)
					fromIn = virtualNetwork.getVirtualNode(link);
				else {
					VirtualNode candidate = virtualNetwork.getVirtualNode(link);
					if (fromIn != candidate) {
						toIn = candidate;
						VirtualLink virtualLink = virtualNetwork.getVirtualLink(fromIn, toIn);
						return virtualLink.index;
					}
				}
			}
		}
		// we can reach this point if vehicle is in last virtual node of path
		// System.out.println("failed to find virtual link of transition.");
		return NOLINKFOUND;
	}

	// requests that haven't received a pickup order yet
	final Map<AVRequest, MpcRequest> mpcRequestsMap = new HashMap<>();

	// requests that have received a pickup order but are not yet "pickedup"
	// private final Map<AVRequest, AVVehicle> matchings = new HashMap<>();
	//
	protected Map<AVRequest, RoboTaxi> getMatchings() {
		return pickupRegister;
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

//	Map<VirtualNode, List<VehicleLinkPair>> getDivertableNotRebalancingNotPickupVehicles() {
//		Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeDivertableNotRebalancingVehicles();
//		Set<AVVehicle> pickupBusy = getMatchings().values().stream().collect(Collectors.toSet());
//		Map<VirtualNode, List<VehicleLinkPair>> map = new HashMap<>();
//		for (Entry<VirtualNode, List<VehicleLinkPair>> entry : availableVehicles.entrySet()) {
//			List<VehicleLinkPair> list = entry.getValue().stream() //
//					.filter(vlp -> !pickupBusy.contains(vlp.avVehicle)) //
//					.collect(Collectors.toList());
//			map.put(entry.getKey(), list);
//		}
//		return map;
//	}

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

	/**
	 * @return requests that have received a pickup order (and are therefore
	 *         contained in considerItDone)
	 */
	Map<Link, List<AVRequest>> override_getAVRequestsAtLinks() {
		// intentionally not parallel to guarantee ordering of requests
		return getAVRequests().stream() //
				.filter(avRequest -> getMatchings().containsKey(avRequest)) //
				.collect(Collectors.groupingBy(AVRequest::getFromLink));
	}

	Collection<AVRequest> getAVRequestsUnserved() {
		Collection<AVRequest> collection = new LinkedList<>();
		for (AVRequest avRequest : getAVRequests()) // all current requests
			// only count request that haven't received a pickup order yet
			if (!getMatchings().containsKey(avRequest))
				collection.add(avRequest);
		return collection;

	}

	Set<RoboTaxi> getRoboTaxiInMatching() {
		return getMatchings().values().stream().collect(Collectors.toSet());
	}

	/**
	 * function computes mpcRequest objects for unserved requests
	 * 
	 * @param now
	 */
	void manageIncomingRequests(double now) {
		final int m = virtualNetwork.getvLinksCount();

		for (AVRequest avRequest : getAVRequestsUnserved()) // all current
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
