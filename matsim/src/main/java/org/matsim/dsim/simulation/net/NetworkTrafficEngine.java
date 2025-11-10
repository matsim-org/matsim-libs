package org.matsim.dsim.simulation.net;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.dsim.*;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.dsim.DSimConfigGroup;
import org.matsim.dsim.simulation.AgentSourcesContainer;

public class NetworkTrafficEngine implements DistributedMobsimEngine {

	private final SimNetwork simNetwork;
	private final EventsManager em;

	private final ActiveNodes activeNodes;
	private final ActiveLinks activeLinks;
	private final ParkedVehicles parkedVehicles;

	private final AgentSourcesContainer asc;
	private final Wait2Link wait2Link;
	//private final Set<String> modes;

	private InternalInterface internalInterface;

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

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
		var dsimConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), DSimConfigGroup.class);
		//this.modes = new HashSet<>(dsimConfig.getNetworkModes());
	}

	@Override
	public void onPrepareSim() {
		for (SimLink link : simNetwork.getLinks().values()) {

			// Split out links don't have queue and buffer and have no leave handler
			if (link instanceof SimLink.SplitOutLink)
				continue;

			// add a leave handler to each link that delegates to the 'handleVehicleIsFinished' method.
			// also give it a low priority, to make sure that this handler is called last if more handlers
			// are active
			link.addLeaveHandler(new SimLink.OnLeaveQueue() {
				@Override
				public SimLink.OnLeaveQueueInstruction apply(DistributedMobsimVehicle vehicle, SimLink link, double now) {
					return handleVehicleIsFinished(vehicle, link, now);
				}

				@Override
				public double getPriority() {
					return -10.;
				}
			});
		}
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
