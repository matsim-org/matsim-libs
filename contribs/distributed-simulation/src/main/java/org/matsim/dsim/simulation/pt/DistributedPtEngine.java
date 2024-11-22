package org.matsim.dsim.simulation.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.dsim.DistributedDepartureHandler;
import org.matsim.core.mobsim.dsim.DistributedMobsimEngine;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.dsim.simulation.net.SimLink;
import org.matsim.dsim.simulation.net.SimNetwork;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import java.util.*;
import java.util.stream.Stream;

public class DistributedPtEngine implements DistributedMobsimEngine, DistributedDepartureHandler {

	private final TransitQSimEngine transitQSimEngine;
	private final Queue<VehicleAtStop> vehiclesAtStop = new PriorityQueue<>(new VehAtStopComparator());

	DistributedPtEngine(Scenario scenario, TransitQSimEngine transitQSimEngine, SimNetwork simNetwork) {
		this.transitQSimEngine = transitQSimEngine;

		scenario.getTransitSchedule().getTransitLines().values().stream()
			.flatMap(line -> line.getRoutes().values().stream())
			.map(TransitRoute::getRoute)
			.flatMap(netRoute -> Stream.concat(Stream.of(netRoute.getStartLinkId(), netRoute.getEndLinkId()), netRoute.getLinkIds().stream()))
			.distinct()
			.map(id -> simNetwork.getLinks().get(id))
			.forEach(link -> link.addLeaveHandler(this::onLeaveQueue));
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

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
		return transitQSimEngine.handleDeparture(now, agent, linkId);
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		transitQSimEngine.setInternalInterface(internalInterface);
	}

	@Override
	public void doSimStep(double time) {

		// Because we push vehicles to the end of the queue, vehicles which are pushed later, get to the head of the queue.
		// since we want to preserve the order of vehicles, we put them into a Deque first, and then push vehicles
		// in a second step. Since Deque is Last in First out, the order is preserved

		// TODO: Is this really necessary? Vehicles which are in the vehiclesAtStop queue are sorted by exit time and then by id index.
		// This means the order of vehicles on a link, which leave in the same timestep is not preserved anyway. This would mean, we
		// could also push vehicles onto links directly.
		Deque<VehicleAtStop> buffer = new ArrayDeque<>();
		while (headVehicleReady(time)) {
			var entry = vehiclesAtStop.poll();
			buffer.add(entry);
		}

		for (var entry : buffer) {
			entry.link().pushVehicle(entry.vehicle(), SimLink.LinkPosition.QEnd, time);
		}
	}

	private boolean headVehicleReady(double now) {
		return !vehiclesAtStop.isEmpty() && vehiclesAtStop.peek().vehicle().getEarliestLinkExitTime() <= now;
	}

	@Override
	public void afterSim() {
		transitQSimEngine.afterSim();
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
