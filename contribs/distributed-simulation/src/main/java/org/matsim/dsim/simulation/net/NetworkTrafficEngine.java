package org.matsim.dsim.simulation.net;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.dsim.*;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.dsim.QSimCompatibility;
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

	private final boolean wait2LinkFirst;

	private final Map<Id<Vehicle>, DistributedMobsimVehicle> parkedVehicles = new HashMap<>();
	private final QSimCompatibility qSimCompatibility;

	@Setter
	private InternalInterface internalInterface;
	// TODO don't expose this. Fix it when fixing how we are injecting engines into disim
	@Getter
	private final List<Wait2Link> wait2Link = new ArrayList<>();

	public NetworkTrafficEngine(Scenario scenario, QSimCompatibility qSimCompatibility, SimStepMessaging simStepMessaging, EventsManager em, int part) {
		this.qSimCompatibility = qSimCompatibility;
		simNetwork = new SimNetwork(scenario.getNetwork(), scenario.getConfig(), this::handleVehicleIsFinished, part);
		this.em = em;
		activeNodes = new ActiveNodes(em);
		activeLinks = new ActiveLinks(simStepMessaging);
		activeLinks.setActivateNode(this::activateNode);
		activeNodes.setActivateLink(this::activateLink);
		wait2LinkFirst = scenario.getConfig().qsim().isInsertingWaitingVehiclesBeforeDrivingVehicles();
	}

	public void activateLink(Id<Link> id) {
		activeLinks.activate(simNetwork.getLinks().get(id));
	}

	public void activateNode(Id<Node> id) {
		activeNodes.activate(simNetwork.getNodes().get(id));
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {

		if (!(agent instanceof MobsimDriverAgent driver)) {
			throw new RuntimeException("Only driver agents are supported");
		}

		// place person into vehicle
		DistributedMobsimVehicle vehicle = Objects.requireNonNull(
			parkedVehicles.remove(driver.getPlannedVehicleId()),
			() -> "Vehicle not found: " + driver.getPlannedVehicleId()
		);
		driver.setVehicle(vehicle);
		vehicle.setDriver(driver);
		em.processEvent(new PersonEntersVehicleEvent(now, driver.getId(), vehicle.getId()));

		Id<Link> currentRouteElement = agent.getCurrentLinkId();
		assert currentRouteElement != null : "Vehicle %s has no current route element".formatted(vehicle.getId());

		SimLink link = simNetwork.getLinks().get(currentRouteElement);
		assert link != null : "Link %s not found in partition on partition #%d".formatted(currentRouteElement, simNetwork.getPart());

		for (Wait2Link w2l : wait2Link) {
			if (w2l.accept(vehicle, link, now)) {
				break;
			}
		}
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
		DistributedMobsimVehicle vehicle = qSimCompatibility.vehicleFromContainer(vehicleContainer);

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
		for (var wait2Link : wait2Link) {
			wait2Link.moveWaiting(now);
		}
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

	public void addParkedVehicle(MobsimVehicle veh, Id<Link> _startLinkId) {
		if (veh instanceof DistributedMobsimVehicle dv) {
			parkedVehicles.put(veh.getId(), dv);
		} else {
			throw new RuntimeException("Only QVehicles are supported");
		}
	}
}
