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

	private final Map<Id<Person>, OutsideNetworkTaskDataEntry> startedStayTasksMap = new HashMap<>();
	private final Map<Id<Person>, OutsideNetworkTaskDataEntry> startedStopTasksMap = new HashMap<>();
	private final List<OutsideNetworkTaskDataEntry> stayTaskDataEntries = new ArrayList<>();
	private final List<OutsideNetworkTaskDataEntry> stopTaskDataEntries = new ArrayList<>();
	private int stayTaskCounter = 0;
	private int stopTaskCounter = 0;

	static class OutsideNetworkTaskDataEntry {
		private final Id<Link> linkId;
		private final double startTime;
		private final Id<Person> personId;
		private final String taskId;
		private double endTime;

		public OutsideNetworkTaskDataEntry(String taskId, Id<Person> personId, double startTime, Id<Link> linkId) {
			this.taskId = taskId;
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

		public String getTaskId() {
			return taskId;
		}

		public Id<Link> getLinkId() {
			return linkId;
		}

	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().equals("DrtStay")) {
			Id<Person> personId = event.getPersonId();
			OutsideNetworkTaskDataEntry stayTaskDataEntry = new OutsideNetworkTaskDataEntry("stayTask_" + stayTaskCounter,
					personId, event.getTime(), event.getLinkId());
			startedStayTasksMap.put(personId, stayTaskDataEntry);
			stayTaskCounter += 1;
		}

		if (event.getActType().equals("DrtBusStop")){
			Id<Person> personId = event.getPersonId();
			OutsideNetworkTaskDataEntry stopTaskDataEntry = new OutsideNetworkTaskDataEntry("stopTask_" + stopTaskCounter,
					personId, event.getTime(), event.getLinkId());
			startedStopTasksMap.put(personId, stopTaskDataEntry);
			stopTaskCounter += 1;
		}

	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals("DrtStay")) {
			Id<Person> personId = event.getPersonId();
			OutsideNetworkTaskDataEntry stayTaskDataEntry = startedStayTasksMap.get(personId);
			stayTaskDataEntry.setEndTime(event.getTime());
			stayTaskDataEntries.add(stayTaskDataEntry);
			startedStayTasksMap.remove(personId);
		}

		if (event.getActType().equals("DrtBusStop")) {
			Id<Person> personId = event.getPersonId();
			OutsideNetworkTaskDataEntry stopTaskDataEntry = startedStopTasksMap.get(personId);
			stopTaskDataEntry.setEndTime(event.getTime());
			stopTaskDataEntries.add(stopTaskDataEntry);
			startedStopTasksMap.remove(personId);
		}

	}

	@Override
	public void reset(int iteration) {
		stayTaskCounter = 0;
		stayTaskDataEntries.clear();
		stopTaskDataEntries.clear();
		startedStayTasksMap.clear();
		startedStopTasksMap.clear();
	}

	public List<OutsideNetworkTaskDataEntry> getStayTaskDataEntries() {
		return stayTaskDataEntries;
	}

	public List<OutsideNetworkTaskDataEntry> getStopTaskDataEntries(){
		return stopTaskDataEntries;
	}

	public Map<Id<Person>, OutsideNetworkTaskDataEntry> getStartedStayTasksMap() {
		return startedStayTasksMap;
	}
	public Map<Id<Person>, OutsideNetworkTaskDataEntry> getStartedStopTasksMap() { return startedStopTasksMap; }

}
