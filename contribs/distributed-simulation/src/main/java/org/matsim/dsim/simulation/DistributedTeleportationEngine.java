package org.matsim.dsim.simulation;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.mobsim.dsim.DistributedDepartureHandler;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.dsim.DistributedMobsimEngine;
import org.matsim.core.mobsim.dsim.SimStepMessage;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.dsim.QSimCompatibility;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

@Log4j2
public class DistributedTeleportationEngine implements DistributedDepartureHandler, DistributedMobsimEngine {

	private final EventsManager em;
	private final Queue<TeleportationEntry> personsTeleporting = new PriorityQueue<>(Comparator.comparingDouble(TeleportationEntry::exitTime));
	private final SimStepMessaging simStepMessaging;
	private final QSimCompatibility qsimCompatibility;

	@Setter
	private InternalInterface internalInterface;

	DistributedTeleportationEngine(EventsManager em, SimStepMessaging simStepMessaging, QSimCompatibility qsimCompatibility) {
		this.simStepMessaging = simStepMessaging;
		this.em = em;
		this.qsimCompatibility = qsimCompatibility;
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {

		if (!(agent instanceof DistributedMobsimAgent person)) {
			throw new IllegalStateException("Teleported agent must implement DistributedMobsimAgent");
		}

		// the original engine has a travel time check. The default config parameter (qsim.usingTravelTimeCheckInTeleportation)
		// is 'false' and is not settable from xml. This is usually an indicator that a feature is rarely used.
		// calculate travel time
		var travelTime = person.getExpectedTravelTime().seconds();
		var exitTime = now + travelTime;

		if (simStepMessaging.isLocal(person.getDestinationLinkId())) {
			personsTeleporting.add(new TeleportationEntry(person, exitTime));
		} else {
			simStepMessaging.collectTeleportation(person, exitTime);
		}

		return true;
	}

	@Override
	public void process(SimStepMessage stepMessage, double now) {
		for (var teleportation : stepMessage.teleportations()) {
			var exitTime = teleportation.exitTime();
			if (exitTime < now) {
				throw new IllegalStateException("Teleportation message was received too late. Exit time is supposed to be" +
					exitTime + " but simulation time is already at: " + now + ". This might happen, if partitions " +
					"diverge in simulation time. We don't really have a solution to this problem yet. However, this" +
					" error might be an indicator, that the speed of the teleported leg is too fast.");
			}

			DistributedMobsimAgent agent = qsimCompatibility.agentFromMessage(teleportation.type(), teleportation.agent());
			personsTeleporting.add(new TeleportationEntry(agent, exitTime));
		}
	}

	@Override
	public void doSimStep(double now) {

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

	private boolean firstPersonReady(double now) {
		return !personsTeleporting.isEmpty() && personsTeleporting.peek().exitTime() <= now;
	}

	private record TeleportationEntry(DistributedMobsimAgent person, double exitTime) {
	}
}
