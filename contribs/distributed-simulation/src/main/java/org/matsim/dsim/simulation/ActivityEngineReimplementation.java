package org.matsim.dsim.simulation;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.SimEngine;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.dsim.messages.SimStepMessage;

import java.util.*;

@Deprecated
@Log4j2
public class ActivityEngineReimplementation implements SimEngine {

	@Getter
	private final Queue<SimPersonEntry> agentsAtActivities = new PriorityQueue<>(new PersonEntryComparator());
	private final Collection<SimPerson> finishedAgents = new ArrayList<>();
	private final TimeInterpretation timeInterpretation;
	private final EventsManager em;

	// This has to be settable if engines are supposed to be passed to the
	// simulation and the simulation wires up a callback later.
	@Setter
	private InternalInterface internalInterface;

	public ActivityEngineReimplementation(Collection<SimPerson> persons, TimeInterpretation timeInterpretation, EventsManager em) {
		this.timeInterpretation = timeInterpretation;
		for (SimPerson person : persons) {
			var endTime = timeInterpretation.getActivityEndTime(person.getCurrentActivity(), 0);
			agentsAtActivities.add(new SimPersonEntry(endTime, person));
		}
		this.em = em;
	}

	@Override
	public void accept(MobsimAgent person, double now) {

//		Activity act = person.getCurrentActivity();
//		double endTime = timeInterpretation.getActivityEndTime(act, now);
//
//		var actStartEvent = new ActivityStartEvent(now,
//			person.getId(),
//			act.getLinkId(),
//			act.getFacilityId(),
//			act.getType(),
//			act.getCoord()
//		);
//
//		em.processEvent(actStartEvent);
//
//		if (Double.isInfinite(endTime)) {
//			finishedAgents.add(person);
//		} else if (endTime <= now) {
//			var actEndEvent = new ActivityEndEvent(
//				now,
//				person.getId(),
//				act.getLinkId(),
//				act.getFacilityId(),
//				act.getType(),
//				act.getCoord()
//			);
//			em.processEvent(actEndEvent);
////			nextStateHandler.accept(person, now);
//		} else {
//			agentsAtActivities.add(new SimPersonEntry(endTime, person));
//		}
	}

	@Override
	public void process(SimStepMessage stepMessage, double now) {
		// don't do anything. We ar not expecting any messages
	}

	@Override
	public void doSimStep(double now) {

		while (firstPersonReady(now)) {
			var entry = agentsAtActivities.poll();
			var person = entry.person();
			var act = person.getCurrentActivity();

			var actEndEvent = new ActivityEndEvent(
				now,
				person.getId(),
				act.getLinkId(),
				act.getFacilityId(),
				act.getType(),
				act.getCoord()
			);
			em.processEvent(actEndEvent);
//			nextStateHandler.accept(entry.person(), now);
		}
	}

	private boolean firstPersonReady(double now) {
		return !agentsAtActivities.isEmpty() && agentsAtActivities.peek().endTime() <= now;
	}

	private record SimPersonEntry(double endTime, SimPerson person) {
	}

	/**
	 * Have a custom comparator for Persons here. If two persons in the queue have the same exit time, we determine the order in the queue using their
	 * IDs as a secondary ordering attribute. This is necessary to avoid inconsistencies between varying numbers of processes.
	 */
	private static class PersonEntryComparator implements Comparator<SimPersonEntry> {

		private final Comparator<SimPersonEntry> endTimeComparator = Comparator.comparingDouble(SimPersonEntry::endTime);

		@Override
		public int compare(SimPersonEntry o1, SimPersonEntry o2) {
			var endTimeResult = endTimeComparator.compare(o1, o2);
			if (endTimeResult == 0) {
				return o1.person.getId().compareTo(o2.person.getId());
			} else {
				return endTimeResult;
			}
		}
	}
}
