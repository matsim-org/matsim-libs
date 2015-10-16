package patryk.utils;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.population.Person;

public class HomeActivityEnd implements ActivityEndEventHandler {

	ArrayList<Double> leaveHomeTimes = new ArrayList<>();
	ArrayList<Id<Person>> personIDs = new ArrayList<>();
	
	@Override
	public void reset(int iteration) {
		leaveHomeTimes.clear();
		personIDs.clear();
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		String actType = event.getActType();
		Double leaveHomeTime = event.getTime();
		Id<Person> linkID = event.getPersonId();
		
		if (actType.equals("home")) {
			leaveHomeTimes.add(leaveHomeTime);
			personIDs.add(linkID);
		}
	}
	
	public ArrayList<Double> getLeaveHomeTimes() {
		return leaveHomeTimes;
	}
	
	public ArrayList<Id<Person>> getPersonIDs() {
		return personIDs;
	}

}
