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
import org.matsim.events.handler.AgentUtilityEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;

import playground.wrashid.PDES.util.ComparableEvent;


//TODO: write tests for this class

public class ElectricCostHandler implements LinkLeaveEventHandler,ActStartEventHandler,ActEndEventHandler,AgentUtilityEventHandler {

	private HashMap<Id,EnergyApplicatonSpecificState> energyLevel =new HashMap<Id,EnergyApplicatonSpecificState>();
	private final double fullEnergyLevel=259000; // in [J]
	// default: 36000000  (=10 kWh)
	private final double penaltyForRunningOutOfElectricEnergy=-100000000;
	private MobSimController controler=null;
	private EnergyConsumptionSamples energyConsumptionSamples=null;
	private Events events=null;
	
	// application specific
	private double averageTimeSpentAtWork=0;
	private double averageTimeSpentAtShop=0;
	
	
	public ElectricCostHandler(MobSimController controler,EnergyConsumptionSamples energyConsumptionSamples,Events events){
		this.controler=controler;
		this.energyConsumptionSamples=energyConsumptionSamples;
		this.events=events;
	}
	
	
	public void handleEvent(LinkLeaveEvent event) {
		// initialize the energyLevel at the beginning to full energy
		if (!energyLevel.containsKey(event.agent.getId())){
			energyLevel.put(event.agent.getId(), new EnergyApplicatonSpecificState(fullEnergyLevel));
		}
		
		// updated consumed energy for link
		EnergyApplicatonSpecificState state=energyLevel.get(event.agent.getId());
		state.energyLevel-=getEnergyConsumption(event.link);
		
		// if energy level is below zero: give huge penalty to agent
		if (state.energyLevel<=0){
			events.processEvent(new AgentUtilityEvent(event.time,event.agent,penaltyForRunningOutOfElectricEnergy));
		}
	}
	
	private double getEnergyConsumption(Link link){
		double freeSpeed=link.getFreespeed(controler.getNetwork().getCapacityPeriod());
		double linkLength=link.getLength();
		return energyConsumptionSamples.getInterpolatedEnergyConsumption(freeSpeed,linkLength);
	}
	

	public void reset(int iteration) {
		System.out.println("averageTimeSpentAtWork:" + averageTimeSpentAtWork/energyLevel.size());
		System.out.println("averageTimeSpentAtShop:" + averageTimeSpentAtShop/energyLevel.size());
		
		// reset variables
		averageTimeSpentAtWork=0;
		averageTimeSpentAtShop=0;
		energyLevel.clear();
	}
	
	private class EnergyApplicatonSpecificState{
		
		EnergyApplicatonSpecificState(double energyLevel){
			this.energyLevel=energyLevel;
		} 
		
		public double energyLevel=0; // in J
		public double startTimeOfLastAct=0; // in sec (offset midnight = 0sec)
	}

	public void handleEvent(ActStartEvent event) {
		// initialize the energyLevel at the beginning to full energy
		if (!energyLevel.containsKey(event.agent.getId())){
			energyLevel.put(event.agent.getId(), new EnergyApplicatonSpecificState(fullEnergyLevel));
		}
		
		// set start time of act
		EnergyApplicatonSpecificState state=energyLevel.get(event.agent.getId());
		state.startTimeOfLastAct =event.time;
	}

	public void handleEvent(ActEndEvent event) {
		// initialize the energyLevel at the beginning to full energy
		if (!energyLevel.containsKey(event.agent.getId())){
			energyLevel.put(event.agent.getId(), new EnergyApplicatonSpecificState(fullEnergyLevel));
		}
		
		// update energyLevel (how much the car loaded during the parking) and also put the cost
		// for the charging into the bill (utility function) of the agent
		
		EnergyApplicatonSpecificState state=energyLevel.get(event.agent.getId());
		
		// assumption is, the agent starts immediately charging, until the energyLevel is full
		// TODO: read chargingPower and costPerJule from config file
		double chargingPower=3500; //  in J/s (=Watt) 
		//default: 3.5KW => 3500 W
		double costPerJuleAtWork=-1; // in "util"/Euro per Jule
		// 0.03 Euro / kWh = 3600000J => 8.3333333333333333333333333333333e-10
		double costPerJuleAtShop=-1;
		
		
		double activityTime=event.time - state.startTimeOfLastAct; // in seconds
		double energyCharged=chargingPower*activityTime; // in J
		
		
		// adjust energyCharged (if could charge more than full battery)
		if (state.energyLevel+energyCharged>fullEnergyLevel){
			energyCharged=fullEnergyLevel-state.energyLevel;
			//System.out.println(energyCharged/chargingPower);
		}


		double costOfEnergy=0.0; // in "util"/Euro
		if (event.linkId.equalsIgnoreCase("100")){
			
			// work1
			costOfEnergy=energyCharged*costPerJuleAtWork;
			//System.out.println(activityTime);
			averageTimeSpentAtWork+=activityTime;
		} else if (event.linkId.equalsIgnoreCase("107")){
			// work2
			costOfEnergy=energyCharged*costPerJuleAtShop;
			averageTimeSpentAtShop+=activityTime;
			//System.out.println("noooo:"+costOfEnergy);
		}
		
		
		events.processEvent(new AgentUtilityEvent(event.time,event.agent,costOfEnergy));
				
		state.energyLevel+=energyCharged;
	}


	public void handleEvent(AgentUtilityEvent event) {
		//System.out.println("util:"+event.amount);
	}

	
	
	
	
	
	
	


}
