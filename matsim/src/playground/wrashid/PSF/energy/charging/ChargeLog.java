package playground.wrashid.PSF.energy.charging;

import org.matsim.api.basic.v01.Id;

public class ChargeLog {

	private Id linkId;

	private double startChargingTime;
	private double endChargingTime;

	public Id getLinkId() {
		return linkId;
	}

	public double getStartChargingTime() {
		return startChargingTime;
	}

	public double getEndChargingTime() {
		return endChargingTime;
	}

	public ChargeLog(Id linkId, double startChargingTime, double endChargingTime) {
		super();
		this.linkId = linkId;
		this.startChargingTime = startChargingTime;
		this.endChargingTime = endChargingTime;
	}


	
	
	
	public void print(){
		System.out.println("startChargingTime: " + startChargingTime + ", endChargingTime: " + endChargingTime + ", linkId: " + linkId);
	}
	
}
