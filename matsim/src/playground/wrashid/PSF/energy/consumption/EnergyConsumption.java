package playground.wrashid.PSF.energy.consumption;

import java.util.LinkedList;

import playground.wrashid.PSF.parking.ParkLog;

public class EnergyConsumption {

	private LinkedList<LinkEnergyConsumptionLog> linkEnergyConsumption=new LinkedList<LinkEnergyConsumptionLog>();
	private double tempEnteranceTimeOfLastLink=0;
	
	public void addEnergyConsumptionLog(LinkEnergyConsumptionLog linkEnergyConsumption){
		this.linkEnergyConsumption.add(linkEnergyConsumption);
	}

	public double getTempEnteranceTimeOfLastLink() {
		return tempEnteranceTimeOfLastLink;
	}

	public void setTempEnteranceTimeOfLastLink(double tempEnteranceTimeOfLastLink) {
		this.tempEnteranceTimeOfLastLink = tempEnteranceTimeOfLastLink;
	}

	public LinkedList<LinkEnergyConsumptionLog> getLinkEnergyConsumption() {
		return linkEnergyConsumption;
	}
	
	/*
	 * get total of all link energy consumptions
	 */
	public double getTotalEnergyConsumption(){
		double totalEnergyConsumption=0;
		for (int i=0;i<linkEnergyConsumption.size();i++){
			totalEnergyConsumption+=linkEnergyConsumption.get(i).getEnergyConsumption();
		}
		return totalEnergyConsumption;
	}
	
	
}
