package org.matsim.contrib.drt.analysis.afterSimAnalysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StayTaskRecorder implements ActivityStartEventHandler, ActivityEndEventHandler {

	private final Map<Id<Person>, StayTaskDataEntry> startedStayTasksMap = new HashMap<>();
	private final List<StayTaskDataEntry> stayTaskDataEntriesList = new ArrayList<>();
	private int counter = 0;

	static class StayTaskDataEntry {
		private final Id<Link> linkId;
		private final double startTime;
		private final Id<Person> personId;
		private final String stayTaskId;
		private double endTime;

		public StayTaskDataEntry(String stayTaskId, Id<Person> personId, double startTime, Id<Link> linkId) {
			this.stayTaskId = stayTaskId;
			this.personId = personId;
			this.startTime = startTime;
			this.linkId = linkId;
		}

		public void setEndTime(double endTime) {
			this.endTime = endTime;
		}

		public double getStartTime() {
			return startTime;
		}

		public double getEndTime() {
			return endTime;
		}

		public Id<Person> getPersonId() {
			return personId;
		}

		public String getStayTaskId() {
			return stayTaskId;
		}

		public Id<Link> getLinkId() {
			return linkId;
		}

	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().equals("DrtStay")) {
			Id<Person> personId = event.getPersonId();
			StayTaskDataEntry stayTaskDataEntry = new StayTaskDataEntry("stayTask_" + counter,
					personId, event.getTime(), event.getLinkId());
			startedStayTasksMap.put(personId, stayTaskDataEntry);
			counter += 1;
		}

	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals("DrtStay")) {
			Id<Person> personId = event.getPersonId();
			StayTaskDataEntry stayTaskDataEntry = startedStayTasksMap.get(personId);
			stayTaskDataEntry.setEndTime(event.getTime());
			stayTaskDataEntriesList.add(stayTaskDataEntry);
			startedStayTasksMap.remove(personId);
		}

	}

	@Override
	public void reset(int iteration) {
		counter = 0;
		stayTaskDataEntriesList.clear();
		startedStayTasksMap.clear();
	}

	public List<StayTaskDataEntry> getStayTaskDataEntriesList() {
		return stayTaskDataEntriesList;
	}

	public Map<Id<Person>, StayTaskDataEntry> getStartedStayTasksMap() {
		return startedStayTasksMap;
	}

}
