package org.matsim.dsim.simulation.pt;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.dsim.DistributedDepartureHandler;
import org.matsim.core.mobsim.dsim.DistributedMobsimEngine;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.dsim.simulation.net.DefaultWait2Link;
import org.matsim.dsim.simulation.net.SimLink;
import org.matsim.dsim.simulation.net.SimNetwork;
import org.matsim.dsim.simulation.net.Wait2Link;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import java.util.*;
import java.util.stream.Stream;

public class DistributedPtEngine implements DistributedMobsimEngine, DistributedDepartureHandler, Wait2Link {

	private final Scenario scenario;
	private final SimNetwork simNetwork;
	private final TransitQSimEngine transitQSimEngine;
	private final Map<Id<Link>, Queue<VehicleAtStop>> activeStops = new HashMap<>();
	private final Map<Id<Link>, Queue<DefaultWait2Link.Waiting>> waitingVehicles = new HashMap<>();
	private final EventsManager em;
	private final Wait2Link vehicleWait2Link;

	@Inject
	public DistributedPtEngine(Scenario scenario, SimNetwork simNetwork, TransitQSimEngine transitQSimEngine, EventsManager em) {
		this.scenario = scenario;
		this.simNetwork = simNetwork;
		this.transitQSimEngine = transitQSimEngine;
		this.vehicleWait2Link = new DefaultWait2Link(em);
		this.em = em;
	}

	@Override
	public void onPrepareSim() {

		// find out which links are pt links and hook into the leaveQ handler.
		scenario.getTransitSchedule().getTransitLines().values().stream()
			.flatMap(line -> line.getRoutes().values().stream())
			.map(TransitRoute::getRoute)
			.flatMap(netRoute -> Stream.concat(Stream.of(netRoute.getStartLinkId(), netRoute.getEndLinkId()), netRoute.getLinkIds().stream()))
			.distinct()
			.filter(id -> simNetwork.getLinks().containsKey(id))
			.map(id -> simNetwork.getLinks().get(id))
			.filter(link -> link instanceof SimLink.LocalLink || link instanceof SimLink.SplitInLink)
			.forEach(link -> link.addLeaveHandler(this::onLeaveQueue));

		transitQSimEngine.onPrepareSim();
	}

	@Override
	public void doSimStep(double now) {

		var it = activeStops.entrySet().iterator();
		while (it.hasNext()) {
			var entry = it.next();
			while (headVehicleReady(entry.getValue(), now)) {
				var linkVehicle = entry.getValue().remove();
				linkVehicle.link().pushVehicle(linkVehicle.vehicle(), SimLink.LinkPosition.QEnd, now);
			}
			if (entry.getValue().isEmpty()) {
				it.remove();
			}
		}
		transitQSimEngine.doSimStep(now);
	}

	private boolean headVehicleReady(Queue<VehicleAtStop> queue, double now) {
		return !queue.isEmpty() && queue.peek().vehicle().getEarliestLinkExitTime() <= now;
	}

	@Override
	public boolean accept(DistributedMobsimVehicle vehicle, SimLink link, double now) {

		// TODO: Check if this is correct

		if (!(vehicle.getDriver() instanceof TransitDriverAgent)) {
			return vehicleWait2Link.accept(vehicle, link, now);
		}

		waitingVehicles
			.computeIfAbsent(link.getId(), _ -> new ArrayDeque<>())
			.add(new DefaultWait2Link.Waiting(vehicle, link));
		return true;
	}

	@Override
	public void moveWaiting(double now) {
		vehicleWait2Link.moveWaiting(now);

		var it = waitingVehicles.values().iterator();
		while (it.hasNext()) {
			var entry = it.next();
			while (!entry.isEmpty()) {
				var veh = entry.peek().vehicle();
				var link = entry.peek().link();
				if (moveVehicle(veh, link, now)) {
					entry.remove();
				} else {
					break;
				}
			}
			if (entry.isEmpty()) {
				it.remove();
			}
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
	public void afterSim() {
		transitQSimEngine.afterSim();
	}

	private boolean moveVehicle(DistributedMobsimVehicle vehicle, SimLink link, double now) {

		if (!link.isAccepting(SimLink.LinkPosition.Buffer, now)) {
			return false;
		}

		var tda = (TransitDriverAgent) vehicle.getDriver();
		var result = handleTransitStop(tda, vehicle, link, now);
		em.processEvent(new VehicleEntersTrafficEvent(
			now, vehicle.getDriver().getId(), link.getId(), vehicle.getId(),
			vehicle.getDriver().getMode(), 1.0)
		);
		switch (result) {
			case BlockQueue -> link.pushVehicle(vehicle, SimLink.LinkPosition.QEnd, now);
			case MoveToBuffer -> link.pushVehicle(vehicle, SimLink.LinkPosition.Buffer, now);
			case RemoveVehicle -> { // nothing to do. The vehicle should be in vehicles at stop
			}
		}
		return true;
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
			activeStops
				.computeIfAbsent(link.getId(), _ -> new PriorityQueue<>(new VehAtStopComparator()))
				.add(new VehicleAtStop(vehicle, link));
			return SimLink.OnLeaveQueueInstruction.RemoveVehicle;
		}
	}

	private record VehicleAtStop(DistributedMobsimVehicle vehicle, SimLink link) {
	}

	private static class VehAtStopComparator implements Comparator<VehicleAtStop> {

		private final Comparator<VehicleAtStop> timeComparator = Comparator.comparingDouble(e -> e.vehicle().getEarliestLinkExitTime());

		@Override
		public int compare(VehicleAtStop o1, VehicleAtStop o2) {
			var timeResult = timeComparator.compare(o1, o2);
			if (timeResult != 0) {
				return o1.vehicle().getId().compareTo(o2.vehicle().getId());
			} else {
				return timeResult;
			}
		}
	}
}
