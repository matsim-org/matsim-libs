package playground.wrashid.artemis.smartCharging;

import java.util.LinkedList;
import java.util.PriorityQueue;

import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.SortableMapObject;


public class ChargingTime {

	private double startChargingTime;
	private double endChargingTime;

	public double getStartChargingTime() {
		return startChargingTime;
	}

	public void setStartChargingTime(double startChargingTime) {
		this.startChargingTime = startChargingTime;
	}

	public ChargingTime(double startChargingTime, double endChargingTime) {
		super();
		this.startChargingTime = startChargingTime;
		this.endChargingTime = endChargingTime;
	}

	public double getDuration() {
		return GeneralLib.getIntervalDuration(startChargingTime, endChargingTime);
	}

	private static int getBinIndex(double time) {
		return Math.round((float) Math.floor(time / 900));
	}

	public double getEndChargingTime() {
		return endChargingTime;
	}

	public static LinkedList<ChargingTime> get15MinChargingBins(double startParkingTime, double endParkingTime) {
		LinkedList<ChargingTime> chargingTimes = new LinkedList<ChargingTime>();
		
		startParkingTime=GeneralLib.projectTimeWithin24Hours(startParkingTime);
		
		if (endParkingTime<startParkingTime){
			endParkingTime+=3600*24;
		}
		
		int indexStartParking = getBinIndex(startParkingTime);
		int indexEndParking = getBinIndex(endParkingTime);

		if (indexStartParking == indexEndParking) {
			chargingTimes.add(new ChargingTime(startParkingTime, endParkingTime));
		} else if (indexEndParking > indexStartParking) {
			// first slot
			chargingTimes.add(new ChargingTime(startParkingTime, 900 * (indexStartParking + 1)));

			for (int i = indexStartParking + 1; i < indexEndParking; i++) {
				chargingTimes.add(new ChargingTime(i * 900, 900 * (i + 1)));
			}

			// last slot
			chargingTimes.add(new ChargingTime(indexEndParking * 900, endParkingTime));

		} else {
			DebugLib.stopSystemAndReportInconsistency();
		}

		return chargingTimes;
	}
	
	public static LinkedList<ChargingTime> sortChargingTimes(LinkedList<ChargingTime> chargingTimes){
		LinkedList<ChargingTime> result=new LinkedList<ChargingTime>();
		
		PriorityQueue<SortableMapObject<ChargingTime>> priorityQueue = new PriorityQueue<SortableMapObject<ChargingTime>>();
	
		for (ChargingTime chargingTime:chargingTimes){
			priorityQueue.add(new SortableMapObject<ChargingTime>(chargingTime, chargingTime.startChargingTime));
		}
		
		while (priorityQueue.size()>0){
			result.add(priorityQueue.poll().getKey());
		}
		
		return result;
	}

}
