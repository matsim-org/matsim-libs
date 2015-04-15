package patryk.utils;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;

public class ActivityEnd implements ActivityStartEventHandler, ActivityEndEventHandler {
	private HashMap<Id<Person>, Double> workTime = new HashMap<>(); 
	private ArrayList<Double> workStartTimes = new ArrayList<>();
	private ArrayList<Double> workEndTimes = new ArrayList<>();
	private ArrayList<Double> workDurTimes = new ArrayList<>();

	@Override
	public void reset(int iteration) {
		workTime.clear();
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id<Person> persID = event.getPersonId();
		String actType = event.getActType();
		
		if (actType.equals(new String("work"))) {
			Double workStartTime = event.getTime();
			workTime.put(persID, workStartTime);
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		Id<Person> persID = event.getPersonId();
		String actType = event.getActType();
		
		if (actType.equals(new String("work"))) {
			Double workEndTime = event.getTime();
			Double workStartTime = workTime.get(persID);
			Double workDurTime = workEndTime - workStartTime;
			workStartTimes.add(workStartTime);
			workEndTimes.add(workEndTime);
			workDurTimes.add(workDurTime);
			workTime.remove(persID);
		}
	}

	public ArrayList<Double> getWorkStartTimes() {
		return workStartTimes;
	}
	
	public ArrayList<Double> getWorkEndTimes() {
		return workEndTimes;
	}
	
	public ArrayList<Double> getDurTimes() {
		return workDurTimes;
	}

}
