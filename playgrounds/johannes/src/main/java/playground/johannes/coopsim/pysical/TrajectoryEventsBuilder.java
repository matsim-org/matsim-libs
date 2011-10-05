/* *********************************************************************** *
 * project: org.matsim.*
 * TrajectoryEventsBuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.johannes.coopsim.pysical;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;

/**
 * @author illenberger
 * 
 */
public class TrajectoryEventsBuilder implements AgentDepartureEventHandler, AgentArrivalEventHandler {

	private Map<Id, PersonData> personData;

	public TrajectoryEventsBuilder(Set<Person> persons) {
		/*
		 * FIXME: This actually should be a ConcurrentHashMap, however, seems to
		 * work anyway!? Because the EventsManager is synchronized!?
		 */
		personData = new HashMap<Id, TrajectoryEventsBuilder.PersonData>(persons.size());
		for (Person person : persons) {
			PersonData data = new PersonData();
			data.person = person;
			personData.put(person.getId(), data);
		}
	}

	public Set<Trajectory> trajectories() {
		Set<Trajectory> trajectories = new HashSet<Trajectory>(personData.size());
		for (PersonData data : personData.values()) {
			if (data.trajectory != null)
				trajectories.add(data.trajectory);
		}
		return trajectories;
	}

	@Override
	public void reset(int iteration) {
		for (PersonData data : personData.values()) {
			data.trajectory = null;
			data.planIndex = 0;
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		addElement(event);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		addElement(event);
	}

	private void addElement(AgentEvent event) {
		PersonData data = personData.get(event.getPersonId());
		Trajectory t = data.trajectory;
		int index = data.planIndex;
		Person person = data.person;

		if (t == null) {
			t = new Trajectory(person);
			data.trajectory = t;
		}

		Plan plan = person.getSelectedPlan();

		t.addElement(plan.getPlanElements().get(index), event.getTime());
		if (index == plan.getPlanElements().size() - 2) {
			/*
			 * This is the last element.
			 */
			t.addElement(plan.getPlanElements().get(index + 1), Math.max(86400, event.getTime() + 1)); // FIXME
																										// Without
																										// +1
																										// sec
																										// plan
																										// scores
																										// can
																										// be
																										// NaN.
																										// Probably
																										// because
																										// the
																										// last
																										// act
																										// has
																										// zero
																										// duration.
		}

		data.planIndex++;
	}

	private class PersonData {

		private Trajectory trajectory;

		private Person person;

		private int planIndex;
	}
}
