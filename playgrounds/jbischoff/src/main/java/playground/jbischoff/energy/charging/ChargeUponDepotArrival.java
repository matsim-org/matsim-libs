package playground.jbischoff.energy.charging;

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.transEnergySim.vehicles.api.BatteryElectricVehicle;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;


public class ChargeUponDepotArrival implements AgentArrivalEventHandler,
		AgentDepartureEventHandler, ShutdownListener {

	
	private List<Id> depotLocations;
	private HashMap<Id, Vehicle> vehicles;
	private HashMap<Id, Double> arrivalTimes;
	private final double MINIMUMCHARGETIME = 120.;
	private final double POWERINKW = 62.5;
	private static final Logger log = Logger.getLogger(ChargeUponDepotArrival.class);

	public ChargeUponDepotArrival(HashMap<Id, Vehicle> vehicles){
		this.vehicles=vehicles;
		this.arrivalTimes = new HashMap<Id, Double>();
		
	}
	
	@Override
	public void reset(int iteration) {
		this.arrivalTimes.clear();
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (!isMonitoredVehicle(event.getPersonId())) return;
		if (!isBatteryElectricVehicle(event.getPersonId())) return;
		if (!isAtDepotLocation(event.getLinkId())) return;
		
		
		if (!this.arrivalTimes.containsKey(event.getPersonId())) return;
		//assumption: Charging before first activity does not take place as cars are assumed to be charged in the morning, this goes in Line with the BatteryElectricVehicleImpl
		
		double chargeTime =event.getTime()- this.arrivalTimes.get(event.getPersonId())-MINIMUMCHARGETIME;
		if (chargeTime<= 0) return;
		
		double chargeWatt = chargeTime * POWERINKW * 1000;
		
		if (((BatteryElectricVehicle)this.vehicles.get(event.getPersonId())).getSocInJoules() + chargeWatt >= ((BatteryElectricVehicle)this.vehicles.get(event.getPersonId())).getUsableBatteryCapacityInJoules())
		{
			chargeWatt = ((BatteryElectricVehicle)this.vehicles.get(event.getPersonId())).getUsableBatteryCapacityInJoules() -((BatteryElectricVehicle)this.vehicles.get(event.getPersonId())).getSocInJoules() - 1; 
			
		}
		((BatteryElectricVehicle)this.vehicles.get(event.getPersonId())).chargeBattery(chargeWatt);
		
		
		log.info("Charged v"+event.getPersonId()+" with" + (chargeWatt/1000.) +" kW");
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (!isMonitoredVehicle(event.getPersonId())) return;
		if (!isBatteryElectricVehicle(event.getPersonId())) return;
		if (!isAtDepotLocation(event.getLinkId())) return;
		
		this.arrivalTimes.put(event.getPersonId(),event.getTime());
		
	}
	
	public void setDepotLocations(List<Id> depots){
		this.depotLocations=depots;
	}
	
	private boolean isMonitoredVehicle(Id agentId){
		return (this.vehicles.containsKey(agentId));
	}
	private boolean isAtDepotLocation(Id linkId){
		return (this.depotLocations.contains(linkId));
	}
	private boolean isBatteryElectricVehicle(Id agentId){
		return (this.vehicles.get(agentId) instanceof BatteryElectricVehicle);
	}
	

}
