package playground.wrashid.PHV.Utility;

import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;

import org.matsim.basic.v01.Id;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;

import playground.wrashid.PDES.util.ComparableEvent;


//TODO: write tests for this class

public class ElectricCostHandler implements LinkLeaveEventHandler {

	private static HashMap<Id,Double> energyLevel =new HashMap<Id,Double>();
	private final double fullEnergyLevel=10; // 10 kW
	private NetworkLayer network=null;
	private EnergyConsumptionSamples energyConsumptionSamples=null;
	
	public ElectricCostHandler(NetworkLayer network,EnergyConsumptionSamples energyConsumptionSamples){
		this.network=network;
		this.energyConsumptionSamples=energyConsumptionSamples;
	}
	
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// initialize the energyLevel at the beginning to full energy
		if (!energyLevel.containsKey(event.agent.getId())){
			energyLevel.put(event.agent.getId(), fullEnergyLevel);
		}
		
		// updated consumed energy for link
		energyLevel.put(event.agent.getId(), energyLevel.get(event.agent.getId())-getEnergyConsumption(event.link));
	}
	
	private double getEnergyConsumption(Link link){
		double freeSpeed=link.getFreespeed(network.getCapacityPeriod());
		double linkLength=link.getLength();
		return energyConsumptionSamples.getInterpolatedEnergyConsumption(freeSpeed,linkLength);
	}
	

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	
	
	
	
	
	
	


}
