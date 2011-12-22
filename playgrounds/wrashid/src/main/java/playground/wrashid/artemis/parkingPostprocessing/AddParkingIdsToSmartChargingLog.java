package playground.wrashid.artemis.parkingPostprocessing;

import java.util.HashMap;


import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.StringMatrix;
import playground.wrashid.lib.tools.txtConfig.TxtConfig;

// NOTE: this class was not used for the experiments (deadend: => see smartCharging.SmartCharger).
public class AddParkingIdsToSmartChargingLog {

	private static TxtConfig config;
	
	public static void main(String[] args) {
		config = new TxtConfig(args[0]);
		
		StringMatrix parkingTimes = GeneralLib.readStringMatrix(config.getParameterValue("parkingTimesFileWithCorrectParkingIds"), "\t");
		
		HashMap<String, Integer> indexOfParkingTimesOfAgent=new HashMap<String, Integer>();
		
		for (int i=1;i<parkingTimes.getNumberOfRows();i++){
			String agentId = parkingTimes.getString(i, 0);
			if (!indexOfParkingTimesOfAgent.containsKey(agentId)){
				indexOfParkingTimesOfAgent.put(agentId, i);
			}
		}
		
		StringMatrix chargingLog = GeneralLib.readStringMatrix(config.getParameterValue("inputChargingLog"), "\t");
		
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
