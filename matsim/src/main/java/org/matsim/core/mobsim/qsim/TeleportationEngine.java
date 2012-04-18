package org.matsim.core.mobsim.qsim;

import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.AdditionalTeleportationDepartureEvent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.comparators.TeleportationArrivalTimeComparator;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.utils.collections.Tuple;

public class TeleportationEngine implements DepartureHandler, MobsimEngine {
	/**
	 * Includes all agents that have transportation modes unknown to the
	 * QueueSimulation (i.e. != "car") or have two activities on the same link
	 */
	private Queue<Tuple<Double, MobsimAgent>> teleportationList = new PriorityQueue<Tuple<Double, MobsimAgent>>(30, new TeleportationArrivalTimeComparator());
	private InternalInterface internalInterface;

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id linkId) {
		double arrivalTime = now + agent.getExpectedTravelTime();
		this.teleportationList.add(new Tuple<Double, MobsimAgent>(arrivalTime, agent));
		internalInterface.getMobsim().getEventsManager().processEvent(new AdditionalTeleportationDepartureEvent(now,
				agent.getId(), linkId, agent.getMode(), agent.getDestinationLinkId(),
				agent.getExpectedTravelTime()));
		return true;
	}

	@Override
	public void doSimStep(double time) {
		handleTeleportationArrivals();
	}

	private void handleTeleportationArrivals() {
		double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
		while (teleportationList.peek() != null) {
			Tuple<Double, MobsimAgent> entry = teleportationList.peek();
			if (entry.getFirst().doubleValue() <= now) {
				teleportationList.poll();
				MobsimAgent personAgent = entry.getSecond();
				personAgent.notifyTeleportToLink(personAgent.getDestinationLinkId());
				personAgent.endLegAndComputeNextState(now);
				internalInterface.arrangeNextAgentState(personAgent) ;
			} else {
				break;
			}
		}
	}

	@Override
	public void onPrepareSim() {
		
	}

	@Override
	public void afterSim() {
		double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
		for (Tuple<Double, MobsimAgent> entry : teleportationList) {
			MobsimAgent agent = entry.getSecond();
			EventsManager eventsManager = internalInterface.getMobsim().getEventsManager();
			eventsManager.processEvent(eventsManager.getFactory().createAgentStuckEvent(now, agent.getId(), agent.getDestinationLinkId(), agent.getMode()));
		}
		teleportationList.clear();
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

}