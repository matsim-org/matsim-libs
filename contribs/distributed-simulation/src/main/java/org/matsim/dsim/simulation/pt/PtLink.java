package org.matsim.dsim.simulation.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.dsim.simulation.SimStepMessaging;
import org.matsim.dsim.simulation.net.SimLink;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

public class PtLink implements SimLink {

	private final SimLink link;
	private final Queue<VehicleAtStop> vehiclesAtStop = new PriorityQueue<>(new VehAtStopComparator());

	public PtLink(SimLink link) {
		link.addLeaveHandler(this::onLeaveQueue);
		this.link = link;
	}

	@Override
	public Id<Link> getId() {
		return link.getId();
	}

	@Override
	public Id<Node> getToNode() {
		return link.getToNode();
	}

	@Override
	public double getMaxFlowCapacity() {
		return link.getMaxFlowCapacity();
	}

	@Override
	public boolean isAccepting(LinkPosition position, double now) {
		return link.isAccepting(position, now);
	}

	@Override
	public boolean isOffering() {
		return link.isOffering();
	}

	@Override
	public boolean isStuck(double now) {
		return link.isStuck(now);
	}

	@Override
	public DistributedMobsimVehicle peekFirstVehicle() {
		return link.peekFirstVehicle();
	}

	@Override
	public DistributedMobsimVehicle popVehicle() {
		return link.popVehicle();
	}

	@Override
	public void pushVehicle(DistributedMobsimVehicle vehicle, LinkPosition position, double now) {
		if (position == LinkPosition.Buffer) {
			checkForTransitStopAndPush(vehicle, position, now);
		} else {
			link.pushVehicle(vehicle, position, now);
		}
	}

	private void checkForTransitStopAndPush(DistributedMobsimVehicle vehicle, LinkPosition position, double now) {

		if (vehicle.getDriver() instanceof TransitDriverAgent tda) {
			var result = handleTransitStop(tda, vehicle, link, now);
			switch (result) {
				case BlockQueue -> link.pushVehicle(vehicle, LinkPosition.QEnd, now);
				case MoveToBuffer -> link.pushVehicle(vehicle, position, now);
				case RemoveVehicle -> {
				}
			}
		} else {
			link.pushVehicle(vehicle, position, now);
		}
	}

	@Override
	public void addLeaveHandler(OnLeaveQueue onLeaveQueue) {
		link.addLeaveHandler(onLeaveQueue);
	}

	@Override
	public boolean doSimStep(SimStepMessaging messaging, double now) {
		// The original implementation fetches vehicles from the stop queue, puts them into a buffer list, and retrieves them in reverse order.
		// This is to keep the original order, by which vehicles are fetched from the stop queue. However, the vehicles in the queue are sorted
		// by exit time and then by id index. The order by which vehicles are polled from the queue is an arbitrary order anyway. So, I think
		// we can poll and push onto the link directly instead.
		while (headVehicleReady(now)) {
			var entry = vehiclesAtStop.remove();
			link.pushVehicle(entry.vehicle(), LinkPosition.QEnd, now);
		}
		return link.doSimStep(messaging, now);
	}

	private boolean headVehicleReady(double now) {
		return !vehiclesAtStop.isEmpty() && vehiclesAtStop.peek().vehicle().getEarliestLinkExitTime() <= now;
	}

	private SimLink.OnLeaveQueueInstruction onLeaveQueue(DistributedMobsimVehicle vehicle, SimLink link, double now) {

		// we are responsible for TransitDrivers and their vehicles
		if (vehicle.getDriver() instanceof TransitDriverAgent tda) {
			return handleTransitStop(tda, vehicle, link, now);
		} else {
			return SimLink.OnLeaveQueueInstruction.MoveToBuffer;
		}
	}

	private SimLink.OnLeaveQueueInstruction handleTransitStop(
		TransitDriverAgent driver, DistributedMobsimVehicle vehicle, SimLink link, double now) {

		var stop = driver.getNextTransitStop();
		if (stop == null || !stop.getLinkId().equals(link.getId())) {
			// if there is no more stop, or if the next stop is not on this link, keep moving.
			return SimLink.OnLeaveQueueInstruction.MoveToBuffer;
		}

		var delay = driver.handleTransitStop(stop, now);
		if (delay <= 0.) {
			// vehicle has delivered passengers without delay. It can keep moving
			return SimLink.OnLeaveQueueInstruction.MoveToBuffer;
		}

		vehicle.setEarliestLinkExitTime(now + delay);
		if (stop.getIsBlockingLane()) {
			// the vehicle stops on the link and blocks following vehicles
			return SimLink.OnLeaveQueueInstruction.BlockQueue;
		} else {
			// the vehicle stops outside the link (e.g. inside a booth) and following vehicles can pass
			vehiclesAtStop.add(new VehicleAtStop(vehicle, link));
			return SimLink.OnLeaveQueueInstruction.RemoveVehicle;
		}
	}

	private record VehicleAtStop(DistributedMobsimVehicle vehicle, SimLink link) {
	}

	private static class VehAtStopComparator implements Comparator<VehicleAtStop> {

		private final Comparator<VehicleAtStop> timeComparator = Comparator.comparingDouble(e -> e.vehicle().getEarliestLinkExitTime());

		@Override
		public int compare(VehicleAtStop o1, VehicleAtStop o2) {
			var timeResult = timeComparator.compare(o2, o1);
			if (timeResult != 0) {
				return o2.vehicle().getId().compareTo(o1.vehicle().getId());
			} else {
				return timeResult;
			}
		}
	}
}
