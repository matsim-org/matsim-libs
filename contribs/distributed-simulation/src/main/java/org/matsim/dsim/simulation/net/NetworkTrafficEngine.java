package org.matsim.dsim.simulation.net;

import com.google.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.dsim.*;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.dsim.QSimCompatibility;
import org.matsim.dsim.simulation.AgentSourcesContainer;
import org.matsim.dsim.simulation.SimStepMessaging;
import org.matsim.vehicles.Vehicle;

import java.util.*;

@Log4j2
public class NetworkTrafficEngine implements DistributedDepartureHandler, DistributedMobsimEngine {

	@Getter
	private final SimNetwork simNetwork;
	private final EventsManager em;

	private final ActiveNodes activeNodes;
	private final ActiveLinks activeLinks;

	private final Map<Id<Vehicle>, DistributedMobsimVehicle> parkedVehicles = new HashMap<>();
	private final AgentSourcesContainer asc;
	private final Set<String> transportModes;

	private final Wait2Link wait2Link;

	@Setter
	private InternalInterface internalInterface;

	@Inject
	public NetworkTrafficEngine(Scenario scenario, NetworkPartition partition, AgentSourcesContainer asc,
								SimStepMessaging simStepMessaging, EventsManager em) {
		this.asc = asc;
		this.em = em;
		// TODO: need to be replaced by injection, currently the pt variant has problems with circular dependencies
		this.wait2Link = new DefaultWait2Link(em);
		simNetwork = new SimNetwork(scenario.getNetwork(), scenario.getConfig(), this::handleVehicleIsFinished, partition.getIndex());
		activeNodes = new ActiveNodes(em);
		activeLinks = new ActiveLinks(simStepMessaging);
		activeLinks.setActivateNode(this::activateNode);
		activeNodes.setActivateLink(this::activateLink);
		transportModes = new HashSet<>(scenario.getConfig().qsim().getMainModes());
	}

	public void activateLink(Id<Link> id) {
		activeLinks.activate(simNetwork.getLinks().get(id));
	}

	public void activateNode(Id<Node> id) {
		activeNodes.activate(simNetwork.getNodes().get(id));
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {

		if (!transportModes.contains(agent.getMode())) {
			return false;
		}

		if (!(agent instanceof MobsimDriverAgent driver)) {
			throw new RuntimeException("Only driver agents are supported");
		}

		// place person into vehicle
		DistributedMobsimVehicle vehicle = Objects.requireNonNull(
			parkedVehicles.remove(driver.getPlannedVehicleId()),
			() -> "Vehicle not found: %s for agent %s on part %d".formatted(
				driver.getPlannedVehicleId(),
				driver.getId(),
				this.getSimNetwork().getPart())
		);
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
		activeLinks.activate(link);
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
		// this inserts waiting vehicles, then moves vehicles over intersections, and then updates bookkeeping.
		// if the config flag is false, we move vehicles, insert waiting vehicles and then update bookkeeping.
		wait2Link.moveWaiting(now);
		activeNodes.doSimStep(now);
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

		this.parkedVehicles.put(vehicle.getId(), vehicle);
		em.processEvent(new PersonLeavesVehicleEvent(now, driver.getId(), vehicle.getId()));

		driver.endLegAndComputeNextState(now);
		internalInterface.arrangeNextAgentState(driver);
		return SimLink.OnLeaveQueueInstruction.RemoveVehicle;
	}

	public void addParkedVehicle(MobsimVehicle veh) {
		if (veh instanceof DistributedMobsimVehicle dv) {
			parkedVehicles.put(veh.getId(), dv);
		} else {
			throw new RuntimeException("Only QVehicles are supported");
		}
	}
}
