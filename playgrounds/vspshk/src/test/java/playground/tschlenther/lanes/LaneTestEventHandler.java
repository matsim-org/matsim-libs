package playground.tschlenther.lanes;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LaneEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LaneLeaveEventHandler;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.vehicles.Vehicle;

class LaneTestEventHandler implements LaneEnterEventHandler, LaneLeaveEventHandler, VehicleEntersTrafficEventHandler, LinkEnterEventHandler, VehicleLeavesTrafficEventHandler {

	Map<Id<Vehicle>,Double> departureTimes = new HashMap<>();
	Map<Id<Vehicle>,Double> travelTimes = new HashMap<>();
	Map<Id<Vehicle>,Double> olLaneEnterTimes = new HashMap<>();
	Map<Id<Vehicle>,Double> middleLaneEnterTimes = new HashMap<>();
	Map<Id<Vehicle>,Double> bottomLaneEnterTimes = new HashMap<>();
	Map<Id<Vehicle>,Double> topLaneEnterTimes = new HashMap<>();
	Map<Id<Vehicle>,Double> olLaneLeaveTimes = new HashMap<>();
	Map<Id<Vehicle>,Double> bottomLaneLeaveTimes = new HashMap<>();
	Map<Id<Vehicle>,Double> middleLaneLeaveTimes = new HashMap<>();
	Map<Id<Vehicle>,Double> topLaneLeaveTimes = new HashMap<>();
	Map<Id<Vehicle>,Double> L3EnterTimes = new HashMap<>();
	
	
	@Override
	public void reset(int iteration) {
	}	 
	
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if(event.getLinkId().equals(Id.createLinkId("L1"))){
			if(this.departureTimes.containsKey(event.getVehicleId())){
				throw new IllegalStateException(
						"A vehicle enters Link L1 twice");
			}
			this.departureTimes.put(event.getVehicleId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(event.getLinkId().equals(Id.createLinkId("L3"))){
			this.L3EnterTimes.put(event.getVehicleId(),event.getTime());
		}
	}

	@Override
	public void handleEvent(LaneEnterEvent event) {
		if(event.getLaneId().equals(Id.create("2.ol", Lane.class))){
			if(this.olLaneEnterTimes.containsKey(event.getVehicleId())){
				throw new IllegalStateException(
						"A vehicle enters olLane twice");
			}
			this.olLaneEnterTimes.put(event.getVehicleId(), event.getTime());
		}
		else if(event.getLaneId().equals(Id.create("2.1", Lane.class))){
			if(this.topLaneEnterTimes.containsKey(event.getVehicleId())){
				throw new IllegalStateException(
						"A vehicle enters topLane twice");
			}
			this.topLaneEnterTimes.put(event.getVehicleId(), event.getTime());
		}
		else if(event.getLaneId().equals(Id.create("2.2", Lane.class))){
			if(this.middleLaneEnterTimes.containsKey(event.getVehicleId())){
				throw new IllegalStateException(
						"A vehicle enters midLane twice");
			}
			this.middleLaneEnterTimes.put(event.getVehicleId(), event.getTime());
		}
		else if(event.getLaneId().equals(Id.create("2.3", Lane.class))){
			if(this.bottomLaneEnterTimes.containsKey(event.getVehicleId())){
				throw new IllegalStateException(
						"A vehicle enters bottomLane twice");
			}
			this.bottomLaneEnterTimes.put(event.getVehicleId(), event.getTime());
		}	
	}



	@Override
	public void handleEvent(LaneLeaveEvent event) {
		if(event.getLaneId().equals(Id.create("2.ol", Lane.class))){
			if(this.olLaneLeaveTimes.containsKey(event.getVehicleId())){
				throw new IllegalStateException(
						"A vehicle leaves olLane twice");
			}
			this.olLaneLeaveTimes.put(event.getVehicleId(), event.getTime());
		}
		else if(event.getLaneId().equals(Id.create("2.1", Lane.class))){
			if(this.topLaneLeaveTimes.containsKey(event.getVehicleId())){
				throw new IllegalStateException(
						"A vehicle leavestopLane twice");
			}
			this.topLaneLeaveTimes.put(event.getVehicleId(), event.getTime());
		}
		else if(event.getLaneId().equals(Id.create("2.2", Lane.class))){
			if(this.middleLaneLeaveTimes.containsKey(event.getVehicleId())){
				throw new IllegalStateException(
						"A vehicle Leave middleLane twice");
			}
			this.middleLaneLeaveTimes.put(event.getVehicleId(), event.getTime());
		}
		else if(event.getLaneId().equals(Id.create("2.3", Lane.class))){
			if(this.bottomLaneLeaveTimes.containsKey(event.getVehicleId())){
				throw new IllegalStateException(
						"A vehicle Leave bottomLane twice");
			}
			this.bottomLaneLeaveTimes.put(event.getVehicleId(), event.getTime());
		}
	}
	
	
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if(!this.departureTimes.containsKey(event.getVehicleId())){
			throw new IllegalStateException(
					"A vehicle leaves traffic without entering it before.");
		}
		double traveltime = event.getTime() - this.departureTimes.get(event.getVehicleId());
		this.travelTimes.put(event.getVehicleId(), traveltime);
	}

	void printAllTravelTimes(){
		TreeMap<Id<Vehicle>,Double> map = new TreeMap<>(new LanesTestComparator(this.travelTimes));
		map.putAll(this.travelTimes);
		Double x = 0.0;
		System.out.println("-------------------------------\n TravelTimes:");
		for(Id<Vehicle> id : map.keySet()){
			String string = "Vehicle" + id + " travels " + map.get(id) + "seconds";
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
		TreeMap<Id<Vehicle>,Double> map = new TreeMap<>(new LanesTestComparator(this.L3EnterTimes));
		map.putAll(this.L3EnterTimes);
		System.out.println("-------------------------------\n L3 EnterTimes:");
		for(Id<Vehicle> id : map.keySet()){
			String string = "" + map.get(id) + "\t Vehicle " + id;
			System.out.println(string);			
		}
	}

	void printAllOlLaneEnterTimes(){
		TreeMap<Id<Vehicle>,Double> map = new TreeMap<>(new LanesTestComparator(this.olLaneEnterTimes));
		map.putAll(this.olLaneEnterTimes);
		System.out.println("-------------------------------\n olLaneEnterTimes: \t Number of vehicles: " + map.size());
		for(Id<Vehicle> id : map.keySet()){
			String string = "" + map.get(id) + "\t Vehicle " + id;
			System.out.println(string);
		}

	}

	void printAllMiddleLaneEnterTimes(){
		TreeMap<Id<Vehicle>,Double> map = new TreeMap<>(new LanesTestComparator(this.middleLaneEnterTimes));
		map.putAll(this.middleLaneEnterTimes);
		System.out.println("-------------------------------\n MiddleLaneEnterTimes: \t Number of vehicles: " + map.size());
		for(Id<Vehicle> id : map.keySet()){
			String string = "" + map.get(id) + "\t Vehicle " + id;
			System.out.println(string);			
		}
	}
	
	void printAllBottomLaneEnterTimes(){
		TreeMap<Id<Vehicle>,Double> map = new TreeMap<>(new LanesTestComparator(this.bottomLaneEnterTimes));
		map.putAll(this.bottomLaneEnterTimes);
		System.out.println("-------------------------------\n BottomLaneEnterTimes: \t Number of vehicles: " + map.size());
		for(Id<Vehicle> id : map.keySet()){
			String string = "" + map.get(id) + "\t Agent " + id;
			System.out.println(string);			
		}
	}
	
	void printAllTopLaneEnterTimes(){
		TreeMap<Id<Vehicle>,Double> map = new TreeMap<>(new LanesTestComparator(this.topLaneEnterTimes));
		map.putAll(this.topLaneEnterTimes);
		System.out.println("-------------------------------\n topLaneEnterTimes: \t Anzahl Fahrer: " + map.size());
		for(Id<Vehicle> id : map.keySet()){
			String string = "" + map.get(id) + "\t Agent " + id;
			System.out.println(string);			
		}
	}
		

}
