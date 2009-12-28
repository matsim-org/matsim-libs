package playground.wrashid.PSF.energy.consumption;

import org.matsim.api.core.v01.Id;

public class LinkEnergyConsumptionLog {

	private Id linkId;
	private double enterTime = 0;
	private double leaveTime = 0;
	private double energyConsumption = 0;

	public LinkEnergyConsumptionLog(Id linkId, double enterTime, double leaveTime, double energyConsumption) {
		super();
		this.linkId = linkId;
		this.enterTime = enterTime;
		this.leaveTime = leaveTime;
		this.energyConsumption = energyConsumption;
	}

	public Id getLinkId() {
		return linkId;
	}

	public double getEnterTime() {
		return enterTime;
	}

	public double getLeaveTime() {
		return leaveTime;
	}

	public double getEnergyConsumption() {
		return energyConsumption;
	}

}
