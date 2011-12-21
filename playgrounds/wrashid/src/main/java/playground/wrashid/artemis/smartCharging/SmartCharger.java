package playground.wrashid.artemis.smartCharging;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.MathLib;
import playground.wrashid.lib.obj.SortableMapObject;

public class SmartCharger {

	
	
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
