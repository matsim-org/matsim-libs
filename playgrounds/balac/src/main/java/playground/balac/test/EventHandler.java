package playground.balac.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;

public class EventHandler implements ActivityStartEventHandler, ActivityEndEventHandler{

	ArrayList<Double> homeDuration = new ArrayList<>();
	public ArrayList<Double> getHomeDuration() {
		return homeDuration;
	}

	ArrayList<Double> workDuration = new ArrayList<>();
	public ArrayList<Double> getWorkDuration() {
		return workDuration;
	}

	public ArrayList<Double> getLeisureDuration() {
		return leisureDuration;
	}
	
	public ArrayList<Double> getShopDuration() {
		return shopDuration;
	}

	ArrayList<Double> leisureDuration = new ArrayList<>();
	ArrayList<Double> shopDuration = new ArrayList<>();

	ArrayList<Double> workEnd = new ArrayList<>();
	ArrayList<Double> leisureEnd = new ArrayList<>();
	ArrayList<Double> shopEnd = new ArrayList<>();

	Map<String,Double> workStart = new HashMap<>();
	Map<String,Double> leisureStart = new HashMap<>();
	Map<String,Double> shopStart = new HashMap<>();

	Map<String,Double> homeEnd = new HashMap<>();

	Set<String> homeFirstDone = new HashSet<>();
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {

		if (event.getActType().equals("secondary")) {
			leisureEnd.add(event.getTime());
			//if (event.getTime() - leisureStart.get(event.getPersonId().toString()) < 3000.0 && event.getTime() - leisureStart.get(event.getPersonId().toString()) > 0.0)
			//	System.out.println("");
			leisureDuration.add(event.getTime() - leisureStart.get(event.getPersonId().toString()));

		}
		
		if (event.getActType().equals("shopping")) {
			shopEnd.add(event.getTime());
			//if (event.getTime() - shopStart.get(event.getPersonId().toString()) < 3000.0 && event.getTime() - shopStart.get(event.getPersonId().toString()) > 0.0)
				//System.out.println("");
			shopDuration.add(event.getTime() - shopStart.get(event.getPersonId().toString()));

		}
		
		if (event.getActType().equals("home_2")) {
			if (!homeFirstDone.contains(event.getPersonId().toString())) {
				homeEnd.put(event.getPersonId().toString(), event.getTime());
				homeFirstDone.add(event.getPersonId().toString());
			}

			
		}
		
		if (event.getActType().equals("work")) {
			workEnd.add(event.getTime());
			
			workDuration.add(event.getTime() - workStart.get(event.getPersonId().toString()));
		}
		
		
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {		
		if (event.getActType().equals("work")) {
			
			workStart.put(event.getPersonId().toString(), event.getTime());
			
		}
		
		if (event.getActType().equals("secondary")) {
			
			leisureStart.put(event.getPersonId().toString(), event.getTime());
			
		}
		
		if (event.getActType().equals("shopping")) {
			
			shopStart.put(event.getPersonId().toString(), event.getTime());
			
		}

		if (event.getActType().equals("home_2")) {
			if (homeFirstDone.contains(event.getPersonId().toString())) {
				homeDuration.add(homeEnd.get(event.getPersonId().toString()) + 24 *3600.0 - event.getTime());
			}
			
		}
	}
	
	

	public ArrayList<Double> getWorkEnd() {
		return workEnd;
	}

	public ArrayList<Double> getLeisureEnd() {
		return leisureEnd;
	}

}
