package playground.wrashid.PSF.energy.charging.optimizedCharging;

import java.util.LinkedList;
import java.util.PriorityQueue;

import playground.wrashid.PSF.energy.consumption.EnergyConsumption;
import playground.wrashid.PSF.energy.consumption.EnergyConsumptionInfo;
import playground.wrashid.PSF.energy.consumption.LinkEnergyConsumptionLog;
import playground.wrashid.PSF.parking.ParkLog;
import playground.wrashid.PSF.parking.ParkingTimes;
import playground.wrashid.PSF.energy.charging.EnergyChargingInfo;

public class EnergyBalance {

	// for each parking log element we have the energy consumption, preceding it
	// stored under the same index value in the lists.
	LinkedList<ParkLog> parkingTimes = new LinkedList<ParkLog>();
	LinkedList<Double> energyConsumption = new LinkedList<Double>(); // in
	// [J]
	LinkedList<Double> maxChargableEnergy = new LinkedList<Double>(); // in

	// [J]

	public EnergyBalance(ParkingTimes parkTimes, EnergyConsumption energyConsumption, double minEnergyToCharge,
			double batteryCapacity) {
		// prepare parking times
		parkingTimes = (LinkedList<ParkLog>) parkTimes.getParkingTimes().clone();
		// add the last parking event of the day to the queue
		parkTimes.addParkLog(new ParkLog(parkTimes.getCarLastTimeParkedFacilityId(), parkTimes.getLastParkingArrivalTime(),
				parkTimes.getFirstParkingDepartTime()));

		int maxIndex = parkingTimes.size();

		double sumEnergyConsumption;
		LinkedList<LinkEnergyConsumptionLog> energyConsumptionLog = energyConsumption.getLinkEnergyConsumption();
		LinkEnergyConsumptionLog tempEnergyConsumptionLog = null;
		/*
		 * - we compare the starting parking time, because if we would use the
		 * endParkingTime, then we would run into a problem. with the last
		 * parking of the day, because its end time is shorter than of all
		 * parkings
		 */

		for (int i = 0; i < maxIndex; i++) {
			sumEnergyConsumption = 0;
			while (energyConsumptionLog.peek().getEnterTime() < parkingTimes.get(i).getStartParkingTime()) {
				tempEnergyConsumptionLog = energyConsumptionLog.poll();
				sumEnergyConsumption += tempEnergyConsumptionLog.getEnergyConsumption();
			}

			this.energyConsumption.add(sumEnergyConsumption);
		}

		/*
		 * update the maxChargableEnergy(i) means, how much energy can be
		 * charged at maxium at parking with index i (because of the battery
		 * contstraint.
		 * 
		 */

		// initialize the first element
		maxChargableEnergy.set(0, batteryCapacity - this.energyConsumption.get(0));

		for (int i = 1; i < maxIndex; i++) {
			maxChargableEnergy.set(i, batteryCapacity - this.energyConsumption.get(i) - maxChargableEnergy.get(i - 1));
		}
	}
	
	public PriorityQueue<FacilityChargingPrice> getChargingPrice(){
		PriorityQueue<FacilityChargingPrice> chargingPrice=new PriorityQueue<FacilityChargingPrice>();
		int maxIndex = parkingTimes.size();
		double tempPrice;
		//FacilityChargingPrice tempPrice;
		
		
		// the last parking needs to be handeled specially
		int offSet=10000;
		for (int i=0;i<maxIndex-1;i++){
			int minTimeSlotNumber=Math.round(i*offSet+(float)parkingTimes.get(i).getStartParkingTime() / 900);
			int maxTimeSlotNumber=Math.round(i*offSet+(float)parkingTimes.get(i).getEndParkingTime() / 900);
			
			for (int j=minTimeSlotNumber;j<maxTimeSlotNumber;j++){
				tempPrice=EnergyChargingInfo.getEnergyPrice(parkingTimes.get(i).getStartParkingTime(), parkingTimes.get(i).getFacilityId());
			}
			
			// Continue here...
		}
		
		double time=parkingTimes.get(0).getStartParkingTime();
		// make 15 minute bins
		
		
		
	
		
		// last charging must be biggest
		// make i=1000 and add that to it...
		
		
		return chargingPrice;
	}

}
