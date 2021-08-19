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

public class StayTaskManager implements ActivityStartEventHandler, ActivityEndEventHandler {

	private final Map<Id<Person>, StayTaskDataEntry> startedSatyTasksMap = new HashMap<>();
	private final List<StayTaskDataEntry> stayTaskDataEntriesList = new ArrayList<>();
	private int counter = 0;

	class StayTaskDataEntry {
		private Id<Link> linkId;
		private double StartTime;
		private double EndTime;
		private final Id<Person> personId;
		private final String stayTaskId;

		public StayTaskDataEntry(String stayTaskId, Id<Person> personId) {
			this.stayTaskId = stayTaskId;
			this.personId = personId;
		}

		public void setStartTime(double startTime) {
			StartTime = startTime;
		}

		public void setEndTime(double endTime) {
			EndTime = endTime;
		}

		public void setLinkId(Id<Link> linkId) {
			this.linkId = linkId;
		}

		public double getStartTime() {
			return StartTime;
		}

		public double getEndTime() {
			return EndTime;
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
			StayTaskDataEntry stayTaskDataEntry = new StayTaskDataEntry("stayTask_" + Integer.toString(counter),
					personId);
			stayTaskDataEntry.setStartTime(event.getTime());
			stayTaskDataEntry.setLinkId(event.getLinkId());
			startedSatyTasksMap.put(personId, stayTaskDataEntry);
			counter += 1;
		}

	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals("DrtStay")) {
			Id<Person> personId = event.getPersonId();
			StayTaskDataEntry stayTaskDataEntry = startedSatyTasksMap.get(personId);
			stayTaskDataEntry.setEndTime(event.getTime());
			stayTaskDataEntriesList.add(stayTaskDataEntry);
			startedSatyTasksMap.remove(personId);
		}

	}

	@Override
	public void reset(int iteration) {
		counter = 0;
		stayTaskDataEntriesList.clear();
		startedSatyTasksMap.clear();
	}

	public List<StayTaskDataEntry> getStayTaskDataEntriesList() {
		return stayTaskDataEntriesList;
	}

	public Map<Id<Person>, StayTaskDataEntry> getStartedSatyTasksMap() {
		return startedSatyTasksMap;
	}

}
