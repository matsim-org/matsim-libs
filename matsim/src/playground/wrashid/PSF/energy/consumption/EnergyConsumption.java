package playground.wrashid.PSF.energy.consumption;

import java.util.LinkedList;

import playground.wrashid.PSF.parking.ParkLog;

public class EnergyConsumption {

	private LinkedList<LinkEnergyConsumptionLog> legEnergyConsumption=new LinkedList<LinkEnergyConsumptionLog>();
	private double tempEnteranceTimeOfLastLink=0;
	
	public void addEnergyConsumptionLog(LinkEnergyConsumptionLog legEnergyConsumption){
		this.legEnergyConsumption.add(legEnergyConsumption);
	}

	public double getTempEnteranceTimeOfLastLink() {
		return tempEnteranceTimeOfLastLink;
	}

	public void setTempEnteranceTimeOfLastLink(double tempEnteranceTimeOfLastLink) {
		this.tempEnteranceTimeOfLastLink = tempEnteranceTimeOfLastLink;
	}

	public LinkedList<LinkEnergyConsumptionLog> getLegEnergyConsumption() {
		return legEnergyConsumption;
	}
	
	
	
}
