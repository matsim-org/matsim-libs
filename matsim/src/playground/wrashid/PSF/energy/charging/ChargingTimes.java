package playground.wrashid.PSF.energy.charging;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.basic.v01.Id;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.parking.ParkLog;

public class ChargingTimes {

	private LinkedList<ChargeLog> chargingTimes = new LinkedList<ChargeLog>();

	public void addChargeLog(ChargeLog chargeLog) {
		chargingTimes.add(chargeLog);
	}

	public LinkedList<ChargeLog> getChargingTimes() {
		return chargingTimes;
	}

	public void print() {
		for (int i = 0; i < chargingTimes.size(); i++) {
			chargingTimes.get(i).print();
		}
	}
	
	/**
	 * TODO: replace default max battery capacity with a class, which maps max battery of vehicles on 
	 * an individual vehicle basis.
	 * TODO - refactor: this is not neat design, because inconsitencies are possible, if this method is not invoked
	 * 
	 * Appraoche: think through, if this could works????
	 * 
	 * - Attention, this cannot work, because the ChargeLogs are not ordered!
	 * - The energy consumption needs to be taken into account
	 * => this should be directly done via the constructed instead!!!!
	 * 
	 */
	/*
	public void updateSOCs(){
		double startSOC=ParametersPSF.getDefaultMaxBatteryCapacity();
		for (int i = 0; i < chargingTimes.size(); i++) {
			chargingTimes.get(i).updateSOC(startSOC);
			startSOC=chargingTimes.get(i).getEndSOC();
		}
	}
*/
	/**
	 * This writes out charging events. TODO: add columns at the end for
	 * SOC_start and SOC_end Dependencies: Of course the link ids etc. must be
	 * available for using this...
	 * 
	 * @param chargingTimes
	 * @param outputFilePath
	 */
	public static void writeChargingTimes(HashMap<Id, ChargingTimes> chargingTimes, String outputFilePath) {
		System.out.println("linkId\tagentId\tstartChargingTime\tendChargingTime");

		try {
			FileOutputStream fos = new FileOutputStream(outputFilePath);
			OutputStreamWriter chargingOutput = new OutputStreamWriter(fos, "UTF8");
			chargingOutput.write("linkId\tagentId\tstartChargingTime\tendChargingTime\n");

			for (Id personId : chargingTimes.keySet()) {
				ChargingTimes curChargingTime = chargingTimes.get(personId);

				for (ChargeLog chargeLog : curChargingTime.getChargingTimes()) {
					chargingOutput.write(chargeLog.getLinkId().toString() + "\t");
					chargingOutput.write(personId.toString() + "\t");
					chargingOutput.write(chargeLog.getStartChargingTime() + "\t");
					chargingOutput.write(chargeLog.getEndChargingTime() + "\t");
					chargingOutput.write("\n");
				}
			}
			
			chargingOutput.flush();
			chargingOutput.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
