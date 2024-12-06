package org.matsim.dsim.simulation.net;

import com.google.inject.Inject;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.dsim.*;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.dsim.simulation.AgentSourcesContainer;

import java.util.HashSet;
import java.util.Set;

@Log4j2
public class NetworkTrafficEngine implements DistributedDepartureHandler, DistributedMobsimEngine {

	private final SimNetwork simNetwork;
	private final EventsManager em;

	private final ActiveNodes activeNodes;
	private final ActiveLinks activeLinks;
	private final ParkedVehicles parkedVehicles;

	private final AgentSourcesContainer asc;
	private final Wait2Link wait2Link;
	private final Set<String> modes;

	@Setter
	private InternalInterface internalInterface;

	@Inject
	public NetworkTrafficEngine(Scenario scenario, AgentSourcesContainer asc,
								SimNetwork simNetwork, ActiveNodes activeNodes, ActiveLinks activeLinks, ParkedVehicles parkedVehicles,
								Wait2Link wait2Link, EventsManager em) {
		this.asc = asc;
		this.em = em;
		this.wait2Link = wait2Link;
		this.activeNodes = activeNodes;
		this.activeLinks = activeLinks;
		this.parkedVehicles = parkedVehicles;
		this.simNetwork = simNetwork;
		this.modes = new HashSet<>(scenario.getConfig().qsim().getMainModes());
	}

	@Override
	public void onPrepareSim() {
		for (SimLink link : simNetwork.getLinks().values()) {

			// Split out links don't have queue and buffer and have no leave handler
			if (link instanceof SimLink.SplitOutLink)
				continue;

			link.addLeaveHandler(this::handleVehicleIsFinished);
		}
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {

		if (!modes.contains(agent.getMode())) {
			return false;
		}

		if (!(agent instanceof MobsimDriverAgent driver)) {
			throw new RuntimeException("Only driver agents are supported");
		}

		var vehicle = parkedVehicles.unpark(driver.getPlannedVehicleId(), linkId);
		driver.setVehicle(vehicle);
		vehicle.setDriver(driver);
		em.processEvent(new PersonEntersVehicleEvent(now, driver.getId(), vehicle.getId()));

		Id<Link> currentRouteElement = agent.getCurrentLinkId();
		assert currentRouteElement != null : "Vehicle %s has no current route element".formatted(vehicle.getId());

		SimLink link = simNetwork.getLinks().get(currentRouteElement);
		assert link != null : "Link %s not found in partition on partition #%d".formatted(currentRouteElement, simNetwork.getPart());

		wait2Link.accept(vehicle, link, now);
		return true;
	}

	@Override
	public void process(SimStepMessage stepMessage, double now) {
		for (VehicleContainer vehicleMessage : stepMessage.vehicles()) {
			processVehicleMessage(vehicleMessage, now);
		}

		for (CapacityUpdate updateMessage : stepMessage.capUpdates()) {
			processUpdateMessage(updateMessage);
		}
	}

	private void processVehicleMessage(VehicleContainer vehicleContainer, double now) {
		DistributedMobsimVehicle vehicle = asc.vehicleFromContainer(vehicleContainer);

		Id<Link> linkId = vehicle.getDriver().getCurrentLinkId();
		SimLink link = simNetwork.getLinks().get(linkId);

		link.pushVehicle(vehicle, SimLink.LinkPosition.QStart, now);
	}

	private void processUpdateMessage(CapacityUpdate updateMessage) {

		Id<Link> linkId = updateMessage.linkId();
		double released = updateMessage.released();
		double consumed = updateMessage.consumed();
		SimLink link = simNetwork.getLinks().get(linkId);

		if (link instanceof SimLink.SplitOutLink so) {
			so.applyCapacityUpdate(released, consumed);
		} else {
			throw new RuntimeException("Only expecting capacity updates for SplitOutLinks");
		}
	}

	@Override
	public void doSimStep(double now) {
		// Move vehicles over nodes, then add waiting vehicles onto links and then move vehicles from the queue into link buffers
		// This mimiks the order in which the QSim does it.
		activeNodes.doSimStep(now);
		wait2Link.moveWaiting(now);
		activeLinks.doSimStep(now);
	}

	private SimLink.OnLeaveQueueInstruction handleVehicleIsFinished(DistributedMobsimVehicle vehicle, SimLink link, double now) {

		// TODO: assumes driver is person arriving
		var driver = vehicle.getDriver();
		// the vehicle has more elements in the route. Keep going.
		if (!driver.isWantingToArriveOnCurrentLink())
			return SimLink.OnLeaveQueueInstruction.MoveToBuffer;

		// the vehicle has no more route elements. It should leave the network
		// Assumes legMode=networkMode, which is not always the case
		em.processEvent(new VehicleLeavesTrafficEvent(
			now, driver.getId(), link.getId(), vehicle.getId(), driver.getMode(),
			1.0
		));

		this.parkedVehicles.park(vehicle, link);
		em.processEvent(new PersonLeavesVehicleEvent(now, driver.getId(), vehicle.getId()));

		driver.endLegAndComputeNextState(now);
		internalInterface.arrangeNextAgentState(driver);
		return SimLink.OnLeaveQueueInstruction.RemoveVehicle;
	}

	public void addParkedVehicle(MobsimVehicle veh, Id<Link> startLinkId) {
		if (veh instanceof DistributedMobsimVehicle dv) {
			var link = simNetwork.getLinks().get(startLinkId);
			parkedVehicles.park(dv, link);
		} else {
			throw new RuntimeException("Only QVehicles are supported");
		}
	}
}
