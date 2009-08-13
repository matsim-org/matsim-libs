package playground.wrashid.PSF.energy.charging;

import org.matsim.api.basic.v01.Id;

public class ChargeLog {

	private Id facilityId;

	private double startChargingTime;
	private double endChargingTime;

	public Id getFacilityId() {
		return facilityId;
	}

	public double getStartChargingTime() {
		return startChargingTime;
	}

	public double getEndChargingTime() {
		return endChargingTime;
	}

	public ChargeLog(Id facilityId, double startChargingTime, double endChargingTime) {
		super();
		this.facilityId = facilityId;
		this.startChargingTime = startChargingTime;
		this.endChargingTime = endChargingTime;
	}


	
	
	
	public void print(){
		System.out.println("startChargingTime: " + startChargingTime + ", endChargingTime: " + endChargingTime + ", facilityId: " + facilityId);
	}
	
}
