package playground.wrashid.PSF.energy.consumption;

import org.matsim.api.basic.v01.Id;

public class LinkEnergyConsumptionLog {
	
	Id linkId;
	double enterTime=0;
	double leaveTime=0;
	double energyConsumption=0;
	public LinkEnergyConsumptionLog(Id linkId, double enterTime, double leaveTime, double energyConsumption) {
		super();
		this.linkId = linkId;
		this.enterTime = enterTime;
		this.leaveTime = leaveTime;
		this.energyConsumption = energyConsumption;
	}

	
}
