package playground.tschlenther.lanes;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LaneEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LaneLeaveEventHandler;
import org.matsim.lanes.data.v20.Lane;

class LaneTestEventHandler implements LaneEnterEventHandler,
		LaneLeaveEventHandler, Wait2LinkEventHandler , LinkEnterEventHandler,/*  LinkLeaveEventHandler,
		PersonDepartureEventHandler,*/ PersonArrivalEventHandler {

	Map<Id<Person>,Double> departureTimes = new HashMap<Id<Person>, Double>();
	Map<Id<Person>,Double> travelTimes = new HashMap<Id<Person>, Double>();
	Map<Id<Person>,Double> olLaneEnterTimes = new HashMap<Id<Person>, Double>();
	Map<Id<Person>,Double> middleLaneEnterTimes = new HashMap<Id<Person>, Double>();
	Map<Id<Person>,Double> bottomLaneEnterTimes = new HashMap<Id<Person>, Double>();
	Map<Id<Person>,Double> topLaneEnterTimes = new HashMap<Id<Person>, Double>();
	Map<Id<Person>,Double> olLaneLeaveTimes = new HashMap<Id<Person>, Double>();
	Map<Id<Person>,Double> bottomLaneLeaveTimes = new HashMap<Id<Person>, Double>();
	Map<Id<Person>,Double> middleLaneLeaveTimes = new HashMap<Id<Person>, Double>();
	Map<Id<Person>,Double> topLaneLeaveTimes = new HashMap<Id<Person>, Double>();
	Map<Id<Person>,Double> L3EnterTimes = new HashMap<Id<Person>,Double>();
	
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}
	
	/*@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(this.departureTimes.containsKey(event.getDriverId())){
			throw new IllegalStateException(
					"A person has a second departure event");
		}
		this.departureTimes.put(event.getDriverId(), event.getTime());
	}
	*/

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if(!this.departureTimes.containsKey(event.getPersonId())){
			throw new IllegalStateException(
					"A person arrives without departure");
		}
		double traveltime = event.getTime() - this.departureTimes.get(event.getPersonId());
		this.travelTimes.put(event.getPersonId(), traveltime);
	}
	 
	
	@Override
	public void handleEvent(LaneEnterEvent event) {
		if(event.getLaneId().equals(Id.create("2.ol", Lane.class))){
			if(this.olLaneEnterTimes.containsKey(event.getPersonId())){
				throw new IllegalStateException(
						"A person enters olLane twice");
			}
			this.olLaneEnterTimes.put(event.getPersonId(), event.getTime());
		}
		else if(event.getLaneId().equals(Id.create("2.1", Lane.class))){
			if(this.topLaneEnterTimes.containsKey(event.getPersonId())){
				throw new IllegalStateException(
						"A person enters topLane twice");
			}
			this.topLaneEnterTimes.put(event.getPersonId(), event.getTime());
		}
		else if(event.getLaneId().equals(Id.create("2.2", Lane.class))){
			if(this.middleLaneEnterTimes.containsKey(event.getPersonId())){
				throw new IllegalStateException(
						"A person enters midLane twice");
			}
			this.middleLaneEnterTimes.put(event.getPersonId(), event.getTime());
		}
		else if(event.getLaneId().equals(Id.create("2.3", Lane.class))){
			if(this.bottomLaneEnterTimes.containsKey(event.getPersonId())){
				throw new IllegalStateException(
						"A person enters bottomLane twice");
			}
			this.bottomLaneEnterTimes.put(event.getPersonId(), event.getTime());
		}	
	}



	@Override
	public void handleEvent(LaneLeaveEvent event) {
		if(event.getLaneId().equals(Id.create("2.ol", Lane.class))){
			if(this.olLaneLeaveTimes.containsKey(event.getPersonId())){
				throw new IllegalStateException(
						"A person leaves olLane twice");
			}
			this.olLaneLeaveTimes.put(event.getPersonId(), event.getTime());
		}
		else if(event.getLaneId().equals(Id.create("2.1", Lane.class))){
			if(this.topLaneLeaveTimes.containsKey(event.getPersonId())){
				throw new IllegalStateException(
						"A person leavestopLane twice");
			}
			this.topLaneLeaveTimes.put(event.getPersonId(), event.getTime());
		}
		else if(event.getLaneId().equals(Id.create("2.2", Lane.class))){
			if(this.middleLaneLeaveTimes.containsKey(event.getPersonId())){
				throw new IllegalStateException(
						"A person Leave middleLane twice");
			}
			this.middleLaneLeaveTimes.put(event.getPersonId(), event.getTime());
		}
		else if(event.getLaneId().equals(Id.create("2.3", Lane.class))){
			if(this.bottomLaneLeaveTimes.containsKey(event.getPersonId())){
				throw new IllegalStateException(
						"A person Leave bottomLane twice");
			}
			this.bottomLaneLeaveTimes.put(event.getPersonId(), event.getTime());
		}
	}
	
	
	void printAllTravelTimes(){
		TreeMap<Id<Person>,Double> map = new TreeMap<Id<Person>, Double>(new LanesTestComparator(this.travelTimes));
		map.putAll(this.travelTimes);
		Double x = 0.0;
		System.out.println("-------------------------------\n TravelTimes:");
		for(Id<Person> id : map.keySet()){
			String string = "Agent" + id + " travels " + map.get(id) + "seconds";
			System.out.println(string);	
			x += map.get(id);
		}
		System.out.println("Durchschnitt : " + x/map.size() + " seconds");
		printAllOlLaneEnterTimes();
		printAllTopLaneEnterTimes();
		printAllMiddleLaneEnterTimes();
		printAllBottomLaneEnterTimes();
		printAllL3EnterTimes();
	}
	
	private void printAllL3EnterTimes() {
		TreeMap<Id<Person>,Double> map = new TreeMap<Id<Person>, Double>(new LanesTestComparator(this.L3EnterTimes));
		map.putAll(this.L3EnterTimes);
		System.out.println("-------------------------------\n L3 EnterTimes:");
		for(Id<Person> id : map.keySet()){
			String string = "" + map.get(id) + "\t Agent " + id;
			System.out.println(string);			
		}
	}

	void printAllOlLaneEnterTimes(){
		TreeMap<Id<Person>,Double> map = new TreeMap<Id<Person>, Double>(new LanesTestComparator(this.olLaneEnterTimes));
		map.putAll(this.olLaneEnterTimes);
		System.out.println("-------------------------------\n olLaneEnterTimes: \t Anzahl Fahrer: " + map.size());
		for(Id<Person> id : map.keySet()){
			String string = "" + map.get(id) + "\t Agent " + id;
			System.out.println(string);
		}

	}

	void printAllMiddleLaneEnterTimes(){
		TreeMap<Id<Person>,Double> map = new TreeMap<Id<Person>, Double>(new LanesTestComparator(this.middleLaneEnterTimes));
		map.putAll(this.middleLaneEnterTimes);
		System.out.println("-------------------------------\n MiddleLaneEnterTimes: \t Anzahl Fahrer: " + map.size());
		for(Id<Person> id : map.keySet()){
			String string = "" + map.get(id) + "\t Agent " + id;
			System.out.println(string);			
		}
	}
	
	void printAllBottomLaneEnterTimes(){
		TreeMap<Id<Person>,Double> map = new TreeMap<Id<Person>, Double>(new LanesTestComparator(this.bottomLaneEnterTimes));
		map.putAll(this.bottomLaneEnterTimes);
		System.out.println("-------------------------------\n BottomLaneEnterTimes: \t Anzahl Fahrer: " + map.size());
		for(Id<Person> id : map.keySet()){
			String string = "" + map.get(id) + "\t Agent " + id;
			System.out.println(string);			
		}
	}
	
	void printAllTopLaneEnterTimes(){
		TreeMap<Id<Person>,Double> map = new TreeMap<Id<Person>, Double>(new LanesTestComparator(this.topLaneEnterTimes));
		map.putAll(this.topLaneEnterTimes);
		System.out.println("-------------------------------\n topLaneEnterTimes: \t Anzahl Fahrer: " + map.size());
		for(Id<Person> id : map.keySet()){
			String string = "" + map.get(id) + "\t Agent " + id;
			System.out.println(string);			
		}
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if(event.getLinkId().equals(Id.createLinkId("L1"))){
			if(this.departureTimes.containsKey(event.getPersonId())){
				throw new IllegalStateException(
						"A person enters Link L1 twice");
			}
			this.departureTimes.put(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(event.getLinkId().equals(Id.createLinkId("L3"))){
			this.L3EnterTimes.put(event.getDriverId(),event.getTime());
		}
	}
		

}
