package playground.wrashid.PHV.Utility;

import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;

import org.matsim.basic.v01.Id;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.AgentUtilityEvent;
import org.matsim.events.Events;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.events.handler.ActStartEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;

import playground.wrashid.PDES.util.ComparableEvent;


//TODO: write tests for this class

public class ElectricCostHandler implements LinkLeaveEventHandler,ActStartEventHandler,ActEndEventHandler {

	private static HashMap<Id,EnergyApplicatonSpecificState> energyLevel =new HashMap<Id,EnergyApplicatonSpecificState>();
	private final double fullEnergyLevel=36000000; // in [J] (=10 kWh)
	private NetworkLayer network=null;
	private EnergyConsumptionSamples energyConsumptionSamples=null;
	private Events events=null;
	
	public ElectricCostHandler(NetworkLayer network,EnergyConsumptionSamples energyConsumptionSamples,Events events){
		this.network=network;
		this.energyConsumptionSamples=energyConsumptionSamples;
		this.events=events;
	}
	
	
	public void handleEvent(LinkLeaveEvent event) {
		// initialize the energyLevel at the beginning to full energy
		if (!energyLevel.containsKey(event.agent.getId())){
			energyLevel.put(event.agent.getId(), new EnergyApplicatonSpecificState());
		}
		
		// updated consumed energy for link
		EnergyApplicatonSpecificState state=energyLevel.get(event.agent.getId());
		state.energyLevel-=getEnergyConsumption(event.link);
	}
	
	private double getEnergyConsumption(Link link){
		double freeSpeed=link.getFreespeed(network.getCapacityPeriod());
		double linkLength=link.getLength();
		return energyConsumptionSamples.getInterpolatedEnergyConsumption(freeSpeed,linkLength);
	}
	

	public void reset(int iteration) {
		energyLevel.clear();
	}
	
	private class EnergyApplicatonSpecificState{
		public double energyLevel=0; // in J
		public double startTimeOfLastAct=0; // in sec (offset midnight = 0sec)
	}

	public void handleEvent(ActStartEvent event) {
		// initialize the energyLevel at the beginning to full energy
		if (!energyLevel.containsKey(event.agent.getId())){
			energyLevel.put(event.agent.getId(), new EnergyApplicatonSpecificState());
		}
		
		// set start time of act
		EnergyApplicatonSpecificState state=energyLevel.get(event.agent.getId());
		state.startTimeOfLastAct =event.time;
	}

	// precondition: energyLevel for agent exists
	public void handleEvent(ActEndEvent event) {
		assert(energyLevel.containsKey(event.agent.getId()));
		
		// update energyLevel (how much the car loaded during the parking) and also put the cost
		// for the charging into the bill (utility function) of the agent
		
		EnergyApplicatonSpecificState state=energyLevel.get(event.agent.getId());
		
		// assumption is, the agent starts immediately charging, until the energyLevel is full
		// TODO: read chargingPower and costPerJule from config file
		double chargingPower=3500; //  in J/s (=Watt) 
		double costPerJule=1; // in "util" per Jule
		
		double activityTime=event.time - state.startTimeOfLastAct; // in seconds
		double energyCharged=chargingPower*activityTime; // in J
		
		
		// adjust energyCharged (if could charge more than full battery)
		if (state.energyLevel+energyCharged>fullEnergyLevel){
			energyCharged=fullEnergyLevel-state.energyLevel;
		}
		
		double costOfEnergy=energyCharged*costPerJule; // in "util"
		
		events.processEvent(new AgentUtilityEvent(event.time,event.agent,costOfEnergy));
				
		state.energyLevel+=energyCharged;
	}

	
	
	
	
	
	
	


}
