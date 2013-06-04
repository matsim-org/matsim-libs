package playground.jbischoff.taxi.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;

public class TravelDistanceTimeEvaluator implements LinkLeaveEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private Map<Id,Double> taxiTravelDistance;
	private Map<Id,Double> taxiTravelDistancesWithPassenger;
	private Network network;
	private Map<Id,Double> lastDepartureWithPassenger;
	private Map<Id,Double> taxiTravelDurationwithPassenger;
	private List<Id> isOccupied;
	private Map<Id,Double> lastDeparture;
	private Map<Id,Double> taxiTravelDuration;

	
	
	public TravelDistanceTimeEvaluator(Network network) {
		this.taxiTravelDistance= new HashMap<Id,Double>();
		this.taxiTravelDistancesWithPassenger= new HashMap<Id,Double>();
		this.taxiTravelDurationwithPassenger= new HashMap<Id,Double>();
		this.lastDepartureWithPassenger= new HashMap<Id,Double>();
		this.lastDeparture = new HashMap<Id,Double>();
		this.taxiTravelDuration = new HashMap<Id,Double>();
		this.isOccupied = new ArrayList();
		this.network = network;
	}

	public void addAgent(Id agentId){
		this.taxiTravelDistance.put(agentId, 0.0);
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (!isMonitoredVehicle(event.getPersonId())) return;
		double distance = this.taxiTravelDistance.get(event.getPersonId());
		distance = distance + this.network.getLinks().get(event.getLinkId()).getLength();
		this.taxiTravelDistance.put(event.getPersonId(), distance);
		if (this.isOccupied.contains(event.getPersonId())) {
			double distanceWithPax = 0.;
			if (this.taxiTravelDistancesWithPassenger.containsKey(event.getPersonId())) distanceWithPax = this.taxiTravelDistancesWithPassenger.get(event.getPersonId());
			distanceWithPax = distanceWithPax + this.network.getLinks().get(event.getLinkId()).getLength();
			this.taxiTravelDistancesWithPassenger.put(event.getPersonId(), distanceWithPax);
			
		}
	}

	private boolean isMonitoredVehicle(Id agentId){
		return (this.taxiTravelDistance.containsKey(agentId));
	}
	
	public void printTravelDistanceStatistics(){
		System.out.println("Agent ID\tdistanceTravelled\tdistanceTravelledWithPax\tOccupanceOverDistance\tTravelTime\tTravelTimeWithPax\tOccupanceOverTime");
		for (Entry<Id,Double> e: this.taxiTravelDistance.entrySet()){
			double relativeOccupanceDist = this.taxiTravelDistancesWithPassenger.get(e.getKey()) /e.getValue();
			double relativeOccpanceTime = this.taxiTravelDurationwithPassenger.get(e.getKey()) / this.taxiTravelDuration.get(e.getKey());
			System.out.println(e.getKey()+"\t"+(e.getValue()/1000)+"\t"+(this.taxiTravelDistancesWithPassenger.get(e.getKey())/1000)+"\t"+relativeOccupanceDist+"\t"+this.taxiTravelDuration.get(e.getKey())+"\t"+this.taxiTravelDurationwithPassenger.get(e.getKey())+"\t"+relativeOccpanceTime);
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		// TODO Auto-generated method stub
		if (isMonitoredVehicle(event.getPersonId())) handleTaxiDriverLeavesEvent(event);
		if (event.getPersonId().equals(event.getVehicleId())) return;
		double travelTimeWithPax = event.getTime() - this.lastDepartureWithPassenger.get(event.getVehicleId());
		double totalTravelTimeWithPax = 0.;
		if (this.taxiTravelDurationwithPassenger.containsKey(event.getVehicleId())) totalTravelTimeWithPax = this.taxiTravelDurationwithPassenger.get(event.getVehicleId());
		totalTravelTimeWithPax = totalTravelTimeWithPax + travelTimeWithPax;
		this.taxiTravelDurationwithPassenger.put(event.getVehicleId(),totalTravelTimeWithPax);
		this.lastDepartureWithPassenger.remove(event.getVehicleId());
		this.isOccupied.remove(event.getVehicleId());
	}


	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (isMonitoredVehicle(event.getPersonId())) handleTaxiDriverEntersEvent(event);

		if (event.getPersonId().equals(event.getVehicleId())) return;
//		System.out.println(event.getVehicleId() + "dep");
		this.lastDepartureWithPassenger.put(event.getVehicleId(), event.getTime());
		this.isOccupied.add(event.getVehicleId());
	}

	private void handleTaxiDriverLeavesEvent(PersonLeavesVehicleEvent event) {
		double travelTime = event.getTime() - this.lastDeparture.get(event.getPersonId());
		double totalTravelTime = 0.;
		if (this.taxiTravelDuration.containsKey(event.getPersonId())) totalTravelTime = this.taxiTravelDuration.get(event.getPersonId());
		totalTravelTime = totalTravelTime + travelTime;
		this.taxiTravelDuration.put(event.getPersonId(), totalTravelTime);
	
		this.lastDeparture.remove(event.getPersonId());
	}
	
	private void handleTaxiDriverEntersEvent(PersonEntersVehicleEvent event) {
		this.lastDeparture.put(event.getPersonId(), event.getTime());		
	}
}
