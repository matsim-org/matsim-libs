package org.matsim.dsim.simulation.net;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.DistributedMobsimEngine;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.DistributedMobsimAgent;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.DistributedMobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleMessage;
import org.matsim.dsim.QSimCompatibility;
import org.matsim.dsim.messages.CapacityUpdate;
import org.matsim.dsim.messages.SimStepMessage;
import org.matsim.dsim.messages.VehicleContainer;
import org.matsim.dsim.simulation.SimStepMessaging;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Log4j2
public class NetworkTrafficEngine implements DistributedMobsimEngine {

	@Getter
	private final SimNetwork simNetwork;
	private final EventsManager em;

	private final ActiveNodes activeNodes;
	private final ActiveLinks activeLinks;
	private final Wait2Link wait2Link;
	private final boolean wait2LinkFirst;

	private final Map<Id<Vehicle>, DistributedMobsimVehicle> parkedVehicles = new HashMap<>();
	private final QSimCompatibility qSimCompatibility;

	@Setter
	private InternalInterface internalInterface;

	public NetworkTrafficEngine(Scenario scenario, QSimCompatibility qSimCompatibility, SimStepMessaging simStepMessaging, EventsManager em, int part) {
		this.qSimCompatibility = qSimCompatibility;
		simNetwork = new SimNetwork(scenario.getNetwork(), scenario.getConfig(), this::handleVehicleIsFinished, part);
		this.em = em;
		activeNodes = new ActiveNodes(em);
		activeLinks = new ActiveLinks(simStepMessaging);
		activeLinks.setActivateNode(id -> this.activeNodes.activate(this.simNetwork.getNodes().get(id)));
		activeNodes.setActivateLink(activeLinks::activate);
		wait2Link = new Wait2Link(em, activeLinks);
		wait2LinkFirst = scenario.getConfig().qsim().isInsertingWaitingVehiclesBeforeDrivingVehicles();
	}

	@Override
	public void accept(DistributedMobsimAgent person, double now) {

		if (!(person instanceof MobsimDriverAgent driver)) {
			throw new RuntimeException("Only driver agents are supported");
		}

		// place person into vehicle
		DistributedMobsimVehicle mobsimVehicle = Objects.requireNonNull(
			parkedVehicles.remove(driver.getPlannedVehicleId()),
			() -> "Vehicle not found: " + driver.getPlannedVehicleId()
		);
		driver.setVehicle(mobsimVehicle);
		mobsimVehicle.setDriver(driver);

		Id<Link> currentRouteElement = person.getCurrentLinkId();
		assert currentRouteElement != null : "Vehicle %s has no current route element".formatted(mobsimVehicle.getId());

		SimLink link = simNetwork.getLinks().get(currentRouteElement);
		assert link != null : "Link %s not found in partition on partition #%d".formatted(currentRouteElement, simNetwork.getPart());

		wait2Link.accept(mobsimVehicle, link);
	}

	@Override
	public void process(SimStepMessage stepMessage, double now) {
		for (VehicleContainer vehicleMessage : stepMessage.getVehicles()) {
			processVehicleMessage(vehicleMessage, now);
		}

		for (CapacityUpdate updateMessage : stepMessage.getCapacityUpdates()) {
			processUpdateMessage(updateMessage);
		}
	}

	private void processVehicleMessage(VehicleContainer vehicleContainer, double now) {
		DistributedMobsimVehicle vehicle = qSimCompatibility.vehicleFromContainer(vehicleContainer);

		Id<Link> linkId = vehicle.getDriver().getCurrentLinkId();
		SimLink link = simNetwork.getLinks().get(linkId);

		link.pushVehicle(vehicle, SimLink.LinkPosition.QStart, now);
		activeLinks.activate(link);
	}

	private void processUpdateMessage(CapacityUpdate updateMessage) {

		Id<Link> linkId = updateMessage.getLinkId();
		double released = updateMessage.getReleased();
		double consumed = updateMessage.getConsumed();
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
		if (wait2LinkFirst) {
			wait2Link.doSimStep(now);
		}
		activeNodes.doSimStep(now);
		if (!wait2LinkFirst) {
			wait2Link.doSimStep(now);
		}
		activeLinks.doSimStep(now);
	}

	private SimLink.OnLeaveQueueInstruction handleVehicleIsFinished(DistributedMobsimVehicle vehicle, SimLink link, double now) {

		var driver = vehicle.getDriver();
		// the vehicle has more elements in the route. Keep going.
		if (!driver.isWantingToArriveOnCurrentLink())
			return SimLink.OnLeaveQueueInstruction.MoveToBuffer;

		// the vehicle has no more route elements. It should leave the network
		em.processEvent(new VehicleLeavesTrafficEvent(
			now, driver.getId(), link.getId(), vehicle.getId(), driver.getMode(),
			1.0
		));

		this.parkedVehicles.put(vehicle.getId(), vehicle);

		// TODO: assumes driver is person arriving
		// Assumes legMode=networkMode, which is not always the case
		em.processEvent(new PersonArrivalEvent(
			now, driver.getId(), link.getId(), driver.getMode()
		));

		internalInterface.arrangeNextAgentState(vehicle.getDriver());
		return SimLink.OnLeaveQueueInstruction.RemoveVehicle;
	}

	public void addParkedVehicle(MobsimVehicle veh, Id<Link> startLinkId) {
		if (veh instanceof DistributedMobsimVehicle dv) {
			parkedVehicles.put(veh.getId(), dv);
		} else {
			throw new RuntimeException("Only QVehicles are supported");
		}
	}
}
