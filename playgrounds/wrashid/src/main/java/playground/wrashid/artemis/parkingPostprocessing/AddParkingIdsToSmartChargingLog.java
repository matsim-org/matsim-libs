package playground.wrashid.artemis.parkingPostprocessing;

import java.util.HashMap;

import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;


import playground.wrashid.lib.tools.txtConfig.TxtConfig;

// NOTE: this class was not used for the experiments (deadend: => see smartCharging.SmartCharger).
public class AddParkingIdsToSmartChargingLog {

	private static TxtConfig config;
	
	public static void main(String[] args) {
		config = new TxtConfig(args[0]);
		
		Matrix parkingTimes = GeneralLib.readStringMatrix(config.getParameterValue("parkingTimesFileWithCorrectParkingIds"), "\t");
		
		HashMap<String, Integer> indexOfParkingTimesOfAgent=new HashMap<String, Integer>();
		
		for (int i=1;i<parkingTimes.getNumberOfRows();i++){
			String agentId = parkingTimes.getString(i, 0);
			if (!indexOfParkingTimesOfAgent.containsKey(agentId)){
				indexOfParkingTimesOfAgent.put(agentId, i);
			}
		}
		
		Matrix chargingLog = GeneralLib.readStringMatrix(config.getParameterValue("inputChargingLog"), "\t");
		
		for (int i=1;i<chargingLog.getNumberOfRows();i++){
			String agentId = parkingTimes.getString(i, 0);
			double startChargingTime = chargingLog.getDouble(i, 2);
			
			Integer agentFirstParkingIndex = indexOfParkingTimesOfAgent.get(agentId);
			int j=agentFirstParkingIndex;
			boolean foundCorrectParking=false;
			while (parkingTimes.getString(j, 0).equalsIgnoreCase(agentId)){
				if (GeneralLib.isIn24HourInterval(parkingTimes.getDouble(j, 1), parkingTimes.getDouble(j, 2), startChargingTime)){
					
					chargingLog.replaceString(i, 0, parkingTimes.getString(j, 3));
					foundCorrectParking=true;
					break;
				}
				j++;
			}
			
			if (!foundCorrectParking){
				//DebugLib.stopSystemAndReportInconsistency();
				DebugLib.emptyFunctionForSettingBreakPoint();
			}
		}
		
		chargingLog.writeMatrix(config.getParameterValue("outputChargingLog"));
	}
	
	
}
