package playground.wrashid.artemis.smartCharging;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.MathLib;
import playground.wrashid.lib.obj.SortableMapObject;
import playground.wrashid.lib.obj.StringMatrix;
import playground.wrashid.lib.tools.txtConfig.TxtConfig;

public class SmartCharger {

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
		
		StringMatrix chargingLog = GeneralLib.readStringMatrix(config.getParameterValue("dumbChargingLogWithCorrectParkingIds"), "\t");
		
		for (int i=1;i<chargingLog.getNumberOfRows();i++){
			int j=indexOfParkingTimesOfAgent.get(chargingLog.getString(i, 1));
			
			while (!MathLib.equals(parkingTimes.getDouble(j, 1),chargingLog.getDouble(i, 2),1.0)){
				j++;
			}
			
			if (!chargingLog.getString(i, 1).equalsIgnoreCase(parkingTimes.getString(j, 0))){
				DebugLib.stopSystemAndReportInconsistency("match not found for agent:" + chargingLog.getString(i, 1));
			}
			
			// don't try smart charging, charging required is bigger than parking duration
			if (MathLib.equals(parkingTimes.getDouble(j, 2),chargingLog.getDouble(i, 3),1.0)){
				
				
				// TODO: log here just dumb charging.
				continue;
			}
			
			double parkingArrivalTime=parkingTimes.getDouble(j, 1);
			double parkingDepartureTime=parkingTimes.getDouble(j, 2);
			double chargingDuration=GeneralLib.getIntervalDuration(chargingLog.getDouble(i, 2),chargingLog.getDouble(i, 3));
			double chargingPower = (chargingLog.getDouble(i, 5)-chargingLog.getDouble(i, 4))/chargingDuration;
			LinkedList<ChargingTime> chargingTimes = ChargingTime.get15MinChargingBins(parkingArrivalTime, parkingDepartureTime);
			LinkedList<ChargingTime> randomChargingTimes = SmartCharger.getRandomChargingTimes(chargingTimes, chargingDuration);
			LinkedList<ChargingTime> sortedChargingTimes = ChargingTime.sortChargingTimes(randomChargingTimes);
			
			for (ChargingTime chargingTime:sortedChargingTimes){
				System.out.println(chargingLog.getString(i, 1) + " -> " + chargingTime.getStartChargingTime() + "; " + GeneralLib.projectTimeWithin24Hours(chargingTime.getEndChargingTime()));
			}
		}
		
		
				
	}
	
	public static LinkedList<ChargingTime> getRandomChargingTimes(LinkedList<ChargingTime> chargingTimes, double totalChargingTimeNeeded){
		LinkedList<ChargingTime> result=new LinkedList<ChargingTime>();
		
		Random rand=new Random();
		PriorityQueue<SortableMapObject<ChargingTime>> priorityQueue = new PriorityQueue<SortableMapObject<ChargingTime>>();
		
		for (ChargingTime chargingTime:chargingTimes){
			priorityQueue.add(new SortableMapObject<ChargingTime>(chargingTime, rand.nextDouble()));
		}
		
		
		
		while (!MathLib.equals(totalChargingTimeNeeded,0.0,0.1)){
			ChargingTime chargingTime=null;
			try{
				chargingTime = priorityQueue.poll().getKey();
			} catch (Exception e) {
				DebugLib.emptyFunctionForSettingBreakPoint();
			}
			if (chargingTime.getDuration()<totalChargingTimeNeeded){
				totalChargingTimeNeeded-=chargingTime.getDuration();
				result.add(chargingTime);
			} else {
				ChargingTime tmpChargingTime = new ChargingTime(chargingTime.getStartChargingTime(), chargingTime.getStartChargingTime()+totalChargingTimeNeeded);
				totalChargingTimeNeeded-=tmpChargingTime.getDuration();
				result.add(tmpChargingTime);
				
				if (!MathLib.equals(totalChargingTimeNeeded,0.0,0.1)){
					DebugLib.stopSystemAndReportInconsistency();
				}
			}
			
		}
		
		
		return result;
	}
	
}
