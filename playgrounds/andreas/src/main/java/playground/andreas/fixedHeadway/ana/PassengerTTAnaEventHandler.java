package playground.andreas.fixedHeadway.ana;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.utils.misc.Time;

public class PassengerTTAnaEventHandler implements PTEventHandler, AgentDepartureEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {
	
	private final static Logger log = Logger.getLogger(PassengerTTAnaEventHandler.class);


	HashMap<Id, AgentCountBox> departedMap = new HashMap<Id, AgentCountBox>();
	HashMap<Id, AgentCountBox> enteredMap = new HashMap<Id, AgentCountBox>();
	List<AgentCountBox> completeAgents = new LinkedList<AgentCountBox>();
	
	HashMap<Id, String> debug = new HashMap<Id, String>();


	private String vehId;
	private double startTime;
	private double stopTime;
	private int numberOfAdditionalTripsPerformed = 0;
	
	public PassengerTTAnaEventHandler(String vehId, double startTime, double stopTime){
		this.vehId = vehId;
		this.startTime = startTime;
		this.stopTime = stopTime;
	}
	
	public PassengerTTAnaEventHandler(String vehId, String startTime, String stopTime){
		this(vehId, Time.parseTime(startTime), Time.parseTime(stopTime));
	}
	
	public List<AgentCountBox> getAgents(){
		return this.completeAgents;
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if(event.getLegMode().equals(TransportMode.pt)){
			AgentCountBox acb = new AgentCountBox(event.getPersonId());
			acb.waitingTime = event.getTime();
			this.departedMap.put(event.getPersonId(), acb);
		}		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(event.getVehicleId().toString().equalsIgnoreCase(this.vehId)){
			if(event.getTime() >= this.startTime && event.getTime() < this.stopTime){
				AgentCountBox acb = this.departedMap.get(event.getPersonId());
				acb.waitingTime = event.getTime() - acb.waitingTime;
				acb.travelTimeInVehicle = event.getTime();
				this.enteredMap.put(event.getPersonId(), acb);
				this.departedMap.remove(event.getPersonId());
			}
		}
		
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(event.getVehicleId().toString().equalsIgnoreCase(this.vehId)){
			if(event.getTime() >= this.startTime && event.getTime() < this.stopTime){
				AgentCountBox acb = this.enteredMap.get(event.getPersonId());
				acb.travelTimeInVehicle = event.getTime() - acb.travelTimeInVehicle;
				this.completeAgents.add(acb);
				this.enteredMap.remove(event.getPersonId());
				
				if(this.debug.containsKey(event.getPersonId())){
//					log.warn("Agent " + event.getPersonId() + " counted twice");
					this.numberOfAdditionalTripsPerformed++;
				} else {
					this.debug.put(event.getPersonId(), "");
				}
			}
		}
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub		
	}
	
	class AgentCountBox{
		Id agentId;
		double waitingTime;
		double travelTimeInVehicle;
		
		public AgentCountBox(Id personId){
			this.agentId = personId;
		}
		
		public double getWaitingTime(){
			return this.waitingTime;
		}
		
		public double getTravelTimeInVehicle(){
			return this.travelTimeInVehicle;
		}
		
		public double getTotalTravelTime(){
			return this.waitingTime + this.travelTimeInVehicle;
		}
	}

	@Override
	public String printResults() {
		
		double averageWaitingTime = 0.0;
		double averageTravelTime = 0.0;
		
		double sumWaitingTime = 0.0;
		double sumTravelTime = 0.0;
		
		int numberOfAgentsCountedSoFar = 0;
		
		for (AgentCountBox agentCountBox : this.completeAgents) {
			averageWaitingTime = (numberOfAgentsCountedSoFar * averageWaitingTime + agentCountBox.getWaitingTime()) / (numberOfAgentsCountedSoFar + 1);
			averageTravelTime = (numberOfAgentsCountedSoFar * averageTravelTime + agentCountBox.getTravelTimeInVehicle()) / (numberOfAgentsCountedSoFar + 1);
			
			sumWaitingTime += agentCountBox.getWaitingTime();
			sumTravelTime += agentCountBox.getTravelTimeInVehicle();
			
			numberOfAgentsCountedSoFar++;
		}
		
		log.info("Veh " + this.vehId + " from " + Time.writeTime(this.startTime) + " to " + Time.writeTime(this.stopTime) + " counted " + numberOfAgentsCountedSoFar 
				+ " agents who (in average) had to wait for " + ((int) averageWaitingTime) + "s and travelled " 
				+ ((int) averageTravelTime) + "s - sumWaiting " + ((int) sumWaitingTime) + "s, sumTT " + ((int) sumTravelTime) + "s, " + this.numberOfAdditionalTripsPerformed + " agents drive a bus stop ahead resulting in additional trips");
		
		return this.vehId + ", " + Time.writeTime(this.startTime) + ", " + Time.writeTime(this.stopTime) + ", " + numberOfAgentsCountedSoFar + ", " 
		+ ((int) averageWaitingTime) + ", " + ((int) averageTravelTime) + ", " + ((int) sumWaitingTime) + ", " + ((int) sumTravelTime) + ", " + this.numberOfAdditionalTripsPerformed;
		
	}
	
}
