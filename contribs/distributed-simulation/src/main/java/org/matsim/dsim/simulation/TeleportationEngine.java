package org.matsim.dsim.simulation;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.NextStateHandler;
import org.matsim.api.SimEngine;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.DistributedMobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.DistributedMobsimVehicle;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.dsim.messages.SimStepMessage;
import org.matsim.dsim.messages.Teleportation;


import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

@RequiredArgsConstructor
@Log4j2
public class TeleportationEngine implements SimEngine {

	private final EventsManager em;
	private final Queue<TeleportationEntry> personsTeleporting = new PriorityQueue<>(Comparator.comparingDouble(TeleportationEntry::exitTime));
	private final SimStepMessaging simStepMessaging;

	@Setter
	private InternalInterface internalInterface;

	TeleportationEngine(EventsManager em, SimStepMessaging simStepMessaging, Config config) {
		this.simStepMessaging = simStepMessaging;
		this.em = em;
	}

	@Override
	public void accept(DistributedMobsimAgent person, double now) {

		// the original engine has a travel time check. The default config parameter (qsim.usingTravelTimeCheckInTeleportation)
		// is 'false' and is not settable from xml. This is usually an indicator that a feature is rarely used.
		// calculate travel time
		var travelTime = person.getExpectedTravelTime().seconds();
		double exitTime = now + travelTime;
		//person.advanceRoute(SimPerson.Advance.Last);

		if (simStepMessaging.isLocal(person.getDestinationLinkId())) {
			personsTeleporting.add(new TeleportationEntry(person, exitTime));
		} else {
			simStepMessaging.collectTeleportation(person, exitTime);
		}
	}

	@Override
	public void process(SimStepMessage stepMessage, double now) {
		for (Teleportation teleportation : stepMessage.getTeleportationMsgs()) {
			var exitTime = teleportation.getExitTime();
			if (exitTime < now) {
				throw new IllegalStateException("Teleportation message was received too late. Exit time is supposed to be" +
					exitTime + " but simulation time is already at: " + now + ". This might happen, if partitions " +
					"diverge in simulation time. We don't really have a solution to this problem yet. However, this" +
					" error might be an indicator, that the speed of the teleported leg is too fast.");
			}

			//var person = new SimPerson(teleportation.getPerson());
			log.warn("converting message to DistributedMobsimAgent not yet implemented");
			//personsTeleporting.add(new TeleportationEntry(person, exitTime));
		}
	}

	@Override
	public void doSimStep(double now) {

		while (firstPersonReady(now)) {
			TeleportationEntry entry = personsTeleporting.remove();
			var person = entry.person();
			var mode = person.getMode();
			var endLink = person.getCurrentLinkId();
			var distance = person.getExpectedTravelDistance();
			em.processEvent(new TeleportationArrivalEvent(now, person.getId(), distance, mode));
			em.processEvent(new PersonArrivalEvent(now, person.getId(), endLink, mode));

			// firstPersonReady tests that we certainly have an entry no need for null testing
			internalInterface.arrangeNextAgentState(person);
		}
	}

	private boolean firstPersonReady(double now) {
		return !personsTeleporting.isEmpty() && personsTeleporting.peek().exitTime() <= now;
	}

	private record TeleportationEntry(DistributedMobsimAgent person, double exitTime) {
	}
}
