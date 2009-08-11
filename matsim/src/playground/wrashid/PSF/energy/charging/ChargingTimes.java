package playground.wrashid.PSF.energy.charging;

import java.util.LinkedList;

import org.matsim.api.basic.v01.Id;

import playground.wrashid.PSF.parking.ParkLog;

public class ChargingTimes {
	
	private LinkedList<ChargeLog> chargingTimes=new LinkedList<ChargeLog>();
	private double initialStateOfCharge=0;
	private double finalStateOfCharge=0; // needed e.g. for scoring
	
	public ChargingTimes(double initialStateOfCharge) {
		super();
		this.initialStateOfCharge = initialStateOfCharge;
	}
	
	public void addChargeLog(ChargeLog chargeLog){
		chargingTimes.add(chargeLog);
	}

	public double getInitialStateOfCharge() {
		return initialStateOfCharge;
	}

	public void setInitialStateOfCharge(double initialStateOfCharge) {
		this.initialStateOfCharge = initialStateOfCharge;
	}

	public double getFinalStateOfCharge() {
		return finalStateOfCharge;
	}

	public void setFinalStateOfCharge(double finalStateOfCharge) {
		this.finalStateOfCharge = finalStateOfCharge;
	}

	public LinkedList<ChargeLog> getChargingTimes() {
		return chargingTimes;
	}
	
}
