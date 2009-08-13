package playground.wrashid.PSF.energy.charging;

import java.util.LinkedList;

import org.matsim.api.basic.v01.Id;

import playground.wrashid.PSF.parking.ParkLog;

public class ChargingTimes {
	
	private LinkedList<ChargeLog> chargingTimes=new LinkedList<ChargeLog>();

	public void addChargeLog(ChargeLog chargeLog){
		chargingTimes.add(chargeLog);
	}

	public LinkedList<ChargeLog> getChargingTimes() {
		return chargingTimes;
	}
	
	public void print(){
		for (int i=0; i<chargingTimes.size();i++){
			chargingTimes.get(i).print();
		}
	}
}
