package playground.staheale.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;

public class TripHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityStartEventHandler, PersonStuckEventHandler {

	LinkedListValueHashMap<Id,Id> startLink;
	LinkedListValueHashMap<Id, String> mode;
	LinkedListValueHashMap<Id, String> purpose;
	LinkedListValueHashMap<Id, Double> startTime;
	LinkedListValueHashMap<Id,Id> endLink;
	LinkedListValueHashMap<Id, Double> endTime;

	List<Id> ptTripList;
	HashMap<Id, Id> ptStageEndLinkId;
	HashMap<Id, Double> ptStageEndTime;
	List<Id> transitWalkList;
	List<Id> endInitialisedList;

	public LinkedListValueHashMap<Id, Id> getStartLink() {
		return startLink;
	}

	public LinkedListValueHashMap<Id, String> getMode() {
		return mode;
	}
	
	public LinkedListValueHashMap<Id, String> getPurpose() {
		return purpose;
	}

	public LinkedListValueHashMap<Id, Double> getStartTime() {
		return startTime;
	}

	public LinkedListValueHashMap<Id, Id> getEndLink() {
		return endLink;
	}

	public LinkedListValueHashMap<Id, Double> getEndTime() {
		return endTime;
	}

	@Override
	public void reset(int iteration) {
		startLink = new LinkedListValueHashMap<Id, Id>();
		mode = new LinkedListValueHashMap<Id, String>();
		purpose = new LinkedListValueHashMap<Id, String>();
		startTime = new LinkedListValueHashMap<Id, Double>();
		endLink = new LinkedListValueHashMap<Id, Id>();
		endTime = new LinkedListValueHashMap<Id, Double>();
		ptStageEndLinkId = new HashMap<Id, Id>();
		ptStageEndTime = new HashMap<Id, Double>();
		ptTripList = new ArrayList<Id>();
		transitWalkList = new ArrayList<Id>();
		endInitialisedList = new ArrayList<Id>();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		//store endLink and endTime if it's a pt stage
		if (event.getLegMode().equals("pt") || event.getLegMode().equals("transit_walk")) {
			ptStageEndLinkId.put(event.getPersonId(), event.getLinkId());
			ptStageEndTime.put(event.getPersonId(), event.getTime());
		}
		//add endLink and endTime if it's not pt stage
		else {
			endLink.put(event.getPersonId(), event.getLinkId());
			endTime.put(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// handle pt trips
		if (event.getLegMode().equals("pt")) {
			// do not add a transit walk trip if it's only a stage
			if (!ptTripList.contains(event.getPersonId())) {
				startLink.put(event.getPersonId(), event.getLinkId());
				mode.put(event.getPersonId(), event.getLegMode());
				startTime.put(event.getPersonId(), event.getTime());
				// set endLink and endTime to null (in case an agent enters a pt vehicle and the pt vehicle is stuck in the end)
				if (!endInitialisedList.contains(event.getPersonId())) {
					endLink.put(event.getPersonId(), null);
					endTime.put(event.getPersonId(), null);
					purpose.put(event.getPersonId(), null);
					endInitialisedList.add(event.getPersonId());
				}
				
			}
			else {
				//replace transit walk mode with pt
				if (!mode.get(event.getPersonId()).getLast().equals("pt")) {
					mode.get(event.getPersonId()).set((mode.get(event.getPersonId()).size()-1), "pt");
				}
			}


		}
		// handle transit walk trips
		else if (event.getLegMode().equals("transit_walk")) {
			if (!transitWalkList.contains(event.getPersonId())) {
				transitWalkList.add(event.getPersonId());
			}
			// do not add a transit walk trip if it's only a stage
			if (!ptTripList.contains(event.getPersonId())) {
				startLink.put(event.getPersonId(), event.getLinkId());
				mode.put(event.getPersonId(), event.getLegMode());
				startTime.put(event.getPersonId(), event.getTime());
				// set endLink and endTime to null (in case an agent enters a pt vehicle and the pt vehicle is stuck in the end)
				if (!endInitialisedList.contains(event.getPersonId())) {
					endLink.put(event.getPersonId(), null);
					endTime.put(event.getPersonId(), null);
					purpose.put(event.getPersonId(), null);
					endInitialisedList.add(event.getPersonId());
				}
			}
		}
		else {
			startLink.put(event.getPersonId(), event.getLinkId());
			mode.put(event.getPersonId(), event.getLegMode());
			startTime.put(event.getPersonId(), event.getTime());
		}

	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().equals("pt interaction")) {
			// add person to pt trip list
			if (!ptTripList.contains(event.getPersonId())) {
				ptTripList.add(event.getPersonId());
				if (transitWalkList.contains(event.getPersonId())) {
					transitWalkList.remove(event.getPersonId());
				}
			}
		}
		else {
			if (ptTripList.contains(event.getPersonId()) || transitWalkList.contains(event.getPersonId())) {
				if (endInitialisedList.contains(event.getPersonId())) {
					endLink.get(event.getPersonId()).removeLast();
					endTime.get(event.getPersonId()).removeLast();
					purpose.get(event.getPersonId()).removeLast();
				}
				endLink.put(event.getPersonId(), ptStageEndLinkId.get(event.getPersonId()));
				endTime.put(event.getPersonId(), ptStageEndTime.get(event.getPersonId()));
				ptTripList.remove(event.getPersonId());
				transitWalkList.remove(event.getPersonId());
			}
			if (endInitialisedList.contains(event.getPersonId())) {
				endInitialisedList.remove(event.getPersonId());
			}
			purpose.put(event.getPersonId(), event.getActType());
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		endLink.put(event.getPersonId(), event.getLinkId());
		endTime.put(event.getPersonId(), event.getTime());
		purpose.put(event.getPersonId(), "stuck");
	}
}
