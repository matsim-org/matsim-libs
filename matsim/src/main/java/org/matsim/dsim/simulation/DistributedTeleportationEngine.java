package org.matsim.dsim.simulation;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.mobsim.dsim.DistributedDepartureHandler;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.dsim.DistributedMobsimEngine;
import org.matsim.core.mobsim.dsim.Teleportation;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.dsim.scoring.BackpackDataCollector;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;

import java.util.*;

public class DistributedTeleportationEngine implements DistributedDepartureHandler, DistributedMobsimEngine, TeleportationEngine {

	private final EventsManager em;
	private final Queue<TeleportationEntry> personsTeleporting = new PriorityQueue<>(Comparator.comparingDouble(TeleportationEntry::exitTime));
	//private final SimStepMessaging simStepMessaging;
	private final PartitionTransfer partitionTransfer;
	private final AgentSourcesContainer asc;
	private final BackpackDataCollector bdc;

	private InternalInterface internalInterface;
	// store the current time, so we can use it for after sim.
	private double now;

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	@Inject
	DistributedTeleportationEngine(EventsManager em, PartitionTransfer partitionTransfer, AgentSourcesContainer asc,
								   BackpackDataCollector bdc) {
		this.partitionTransfer = partitionTransfer;
		this.asc = asc;
		this.em = em;
		this.bdc = bdc;
	}

	@Override
	public double priority() {
		return Double.NEGATIVE_INFINITY;
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {

		if (!(agent instanceof DistributedMobsimAgent person)) {
			throw new IllegalStateException("Teleported agent must implement DistributedMobsimAgent");
		}

		// the original engine has a travel time check. The default config parameter (qsim.usingTravelTimeCheckInTeleportation)
		// is 'false' and is not settable from xml. This is usually an indicator that a feature is rarely used.
		// calculate travel time
		if (!person.getExpectedTravelTime().isDefined()) {
			throw new IllegalStateException(String.format("Expected travel time is undefined for %s", agent.getId()));
		}

		var travelTime = person.getExpectedTravelTime().seconds();
		var exitTime = now + travelTime;

		if (partitionTransfer.isLocal(person.getDestinationLinkId())) {
			personsTeleporting.add(new TeleportationEntry(person, exitTime));
		} else {
			// TODO: replace with general mechanism for notifying person is leaving
			bdc.teleportedPersonLeavesPartition(person);
			partitionTransfer.collect(new Teleportation(person.getClass(), person.toMessage(), exitTime), person.getDestinationLinkId());
		}

		return true;
	}

	@Override
	public Map<Integer, MessageHandler> getMessageHandlers() {
		return Map.of(
			Teleportation.class.getName().hashCode(),
			this::processTeleportationMessages
		);
	}

	private void processTeleportationMessages(List<Message> messages, double now) {
		for (var m : messages) {
			var msg = (Teleportation) m;
			if (msg.exitTime() < now) {
				throw new IllegalStateException("Teleportation message was received too late. Exit time is supposed to be " +
					msg.exitTime() + " but simulation time is already at: " + now + ". This might happen, if partitions " +
					"diverge in simulation time. We don't really have a solution to this problem yet. However, this " +
					"error might be an indicator, that the speed of the teleported leg is too fast.");
			}
			var agent = asc.agentFromMessage(msg.type(), msg.agent());
			personsTeleporting.add(new TeleportationEntry(agent, msg.exitTime()));
		}
	}

	@Override
	public void doSimStep(double now) {
		this.now = now;

		while (firstPersonReady(now)) {
			TeleportationEntry entry = personsTeleporting.remove();
			DistributedMobsimAgent person = entry.person();
			person.notifyArrivalOnLinkByNonNetworkMode(person.getDestinationLinkId());

			String mode = person.getMode();
			Double distance = person.getExpectedTravelDistance();
			em.processEvent(new TeleportationArrivalEvent(now, person.getId(), distance, mode));

			person.endLegAndComputeNextState(now);
			internalInterface.arrangeNextAgentState(person);
		}
	}

	@Override
	public void afterSim() {
		for (var entry : personsTeleporting) {
			var person = entry.person();
			em.processEvent(new PersonStuckEvent(now, person.getId(), person.getDestinationLinkId(), person.getMode()));
		}
		personsTeleporting.clear();
	}

	@Override
	public double getEnginePriority() {
		return -10.; // this engine should handle things last.
	}

	private boolean firstPersonReady(double now) {
		return !personsTeleporting.isEmpty() && personsTeleporting.peek().exitTime() <= now;
	}

	@Override
	public Collection<AgentSnapshotInfo> addAgentSnapshotInfo(Collection<AgentSnapshotInfo> positions) {
		throw new RuntimeException("Snapshot Positions are not implemented for Distributed Teleportation Engine. This method is only here because the 'TeleportationInterface' requires it.");
	}

	record TeleportationEntry(DistributedMobsimAgent person, double exitTime) {
	}
}
