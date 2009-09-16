package playground.wrashid.PSF.energy.consumption;

import java.util.LinkedList;

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

	/**
	 * The list of energy consumption is ordered by time...
	 * @return
	 */
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
