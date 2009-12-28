package playground.wrashid.PSF.energy.charging.optimizedCharging;

import java.util.LinkedList;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.wrashid.PSF.energy.consumption.EnergyConsumption;
import playground.wrashid.PSF.energy.consumption.LinkEnergyConsumptionLog;
import playground.wrashid.PSF.parking.ParkLog;
import playground.wrashid.PSF.parking.ParkingTimes;
import playground.wrashid.PSF.energy.charging.ChargeLog;
import playground.wrashid.PSF.energy.charging.ChargingTimes;
import playground.wrashid.PSF.energy.charging.EnergyChargingPriceInfo;

public class EnergyBalance {

	private static final Logger log = Logger.getLogger(EnergyBalance.class);

	// for each parking log element we have the energy consumption, preceding it
	// stored under the same index value in the lists.
	LinkedList<ParkLog> parkingTimes = new LinkedList<ParkLog>();
	LinkedList<Double> energyConsumption = new LinkedList<Double>(); // in
	// [J]
	LinkedList<Double> maxChargableEnergy = new LinkedList<Double>(); // in

	// [J]

	ChargingTimes chargingTimes;
	// private double minEnergyToCharge;
	private double batteryCapacity;

	/**
	 * The last parking (index), which has been added to the priority queue for
	 * charging price
	 */
	private int lastChargingPriceParkingIndex = 0;

	public EnergyBalance(ParkingTimes parkTimes, EnergyConsumption energyConsumption, double batteryCapacity,
			ChargingTimes chargingTimes) {

		this.chargingTimes = chargingTimes;
		// this.minEnergyToCharge=minEnergyToCharge;
		this.batteryCapacity = batteryCapacity;

		// prepare parking times
		parkingTimes = (LinkedList<ParkLog>) parkTimes.getParkingTimes().clone();
		// add the last parking event of the day to the queue
		parkingTimes.add(new ParkLog(parkTimes.getCarLastTimeParkedActivity(), parkTimes.getLastParkingArrivalTime(), parkTimes
				.getFirstParkingDepartTime()));

		int maxIndex = parkingTimes.size();

		double sumEnergyConsumption;
		LinkedList<LinkEnergyConsumptionLog> energyConsumptionLog = (LinkedList<LinkEnergyConsumptionLog>) energyConsumption
				.getLinkEnergyConsumption().clone();
		LinkEnergyConsumptionLog tempEnergyConsumptionLog = null;
		/*
		 * - we compare the starting parking time, because if we would use the
		 * endParkingTime, then we would run into a problem. with the last
		 * parking of the day, because its end time is shorter than of all
		 * parkings
		 */

		for (int i = 0; i < maxIndex; i++) {
			sumEnergyConsumption = 0;
			while (energyConsumptionLog.peek() != null
					&& energyConsumptionLog.peek().getEnterTime() < parkingTimes.get(i).getStartParkingTime()) {
				tempEnergyConsumptionLog = energyConsumptionLog.poll();
				sumEnergyConsumption += tempEnergyConsumptionLog.getEnergyConsumption();
			}

			this.energyConsumption.add(sumEnergyConsumption);
		}

		/*
		 * update the maxChargableEnergy(i) means, how much energy can be
		 * charged at maximum at parking with index i (because of the battery
		 * contstraint.
		 */

		// initialize the first element
		maxChargableEnergy.add(this.energyConsumption.get(0));

		for (int i = 1; i < maxIndex; i++) {
			maxChargableEnergy.add(maxChargableEnergy.get(i - 1) + this.energyConsumption.get(i));
		}

		//checkMaxChargableEnergyInvariant();
	}

	private void checkMaxChargableEnergyInvariant() {
		// assert: it is not possible that the driver requires less energy
		// anywhere than before.
		for (int i = 1; i < maxChargableEnergy.size(); i++) {
			if (maxChargableEnergy.get(i) < maxChargableEnergy.get(i - 1)) {
				log.error("this is not possible, because of causality");
				System.exit(0);
			}
		}
	}

	/*
	 * This method adds the parking prices of the specified parking (index) to
	 * the priority list.
	 */
	private PriorityQueue<FacilityChargingPrice> addNewParkingChargingPrices(int parkingIndex,
			PriorityQueue<FacilityChargingPrice> chargingPrice) {

		boolean isLastParking = parkingTimes.size() - 1 == parkingIndex ? true : false;

		int offSet = 10000;
		if (!isLastParking) {
			// find out the slots which need to be put in the priority queue

			int minTimeSlotNumber = Math.round(parkingIndex * offSet + (float) parkingTimes.get(parkingIndex).getStartParkingTime()
					/ 900);
			int maxTimeSlotNumber = Math.round(parkingIndex * offSet + (float) parkingTimes.get(parkingIndex).getEndParkingTime()
					/ 900);

			addChargingPriceToPriorityQueue(chargingPrice, minTimeSlotNumber, maxTimeSlotNumber, parkingIndex, parkingTimes.get(
					parkingIndex).getStartParkingTime());
		} else {
			// if we need to handle the last (night parking)
			int offSetOfEarlyNightHours = 100000000;

			// create time/price slots for morning parking part after 0:00
			int minTimeSlotNumber = Math.round(offSetOfEarlyNightHours);
			int maxTimeSlotNumber = Math.round(offSetOfEarlyNightHours + (float) parkingTimes.get(parkingIndex).getEndParkingTime()
					/ 900);

			addChargingPriceToPriorityQueue(chargingPrice, minTimeSlotNumber, maxTimeSlotNumber, parkingIndex, 0);

			if (parkingTimes.get(parkingIndex).getStartParkingTime() < 86400) {
				// only handle the first part of the last parking, if the day is
				// less than 24 hours long

				minTimeSlotNumber = Math.round(parkingIndex * offSet + (float) parkingTimes.get(parkingIndex).getStartParkingTime()
						/ 900);
				maxTimeSlotNumber = Math.round(parkingIndex * offSet + (float) 86399 / 900);
				// this is just one second before mid night to get the right end
				// slot

				addChargingPriceToPriorityQueue(chargingPrice, minTimeSlotNumber, maxTimeSlotNumber, parkingIndex, parkingTimes
						.get(parkingIndex).getStartParkingTime());
			}
		}

		return chargingPrice;
	}

	private void addChargingPriceToPriorityQueue(PriorityQueue<FacilityChargingPrice> chargingPrice, int minTimeSlotNumber,
			int maxTimeSlotNumber, int parkingIndex, double startTime) {
		double tempPrice;
		for (int j = 0; j < maxTimeSlotNumber - minTimeSlotNumber; j++) {
			double time = Math.floor(startTime / 900) * 900 + j * 900;
			tempPrice = EnergyChargingPriceInfo.getEnergyPrice(time, parkingTimes.get(parkingIndex).getActivity().getLinkId());

			FacilityChargingPrice tempFacilityChPrice = new FacilityChargingPrice(tempPrice, minTimeSlotNumber + j, parkingIndex,
					time, parkingTimes.get(parkingIndex).getActivity().getFacilityId(), parkingTimes.get(parkingIndex)
							.getStartParkingTime(), parkingTimes.get(parkingIndex).getEndParkingTime(), parkingTimes.get(
							parkingIndex).getActivity().getLinkId());
			chargingPrice.add(tempFacilityChPrice);
		}
	}

	/**
	 * Add the parking prices to the priority list as far as needed (only if the
	 * car would run out of energy).
	 * 
	 * @return
	 */
	private PriorityQueue<FacilityChargingPrice> updateChargingPrice(PriorityQueue<FacilityChargingPrice> chargingPrice) {
		while (lastChargingPriceParkingIndex < maxChargableEnergy.size() && getMinimumEnergyThatNeedsToBeCharged() <= 0) {
			chargingPrice = addNewParkingChargingPrices(lastChargingPriceParkingIndex, chargingPrice);

			lastChargingPriceParkingIndex++;
		}
		return chargingPrice;
	}

	/*
	 * - positive value: the energy, that must be charged, so that the car does
	 * not run out of gasoline on its way - negative value: only little energy
	 * is needed for driving, therefore the next parking station could be
	 * reached without problems
	 */
	private double getMinimumEnergyThatNeedsToBeCharged() {

		if (lastChargingPriceParkingIndex == maxChargableEnergy.size()) {
			// if last parking reached, then we must recharge the car fully
			return maxChargableEnergy.get(lastChargingPriceParkingIndex - 1);
		} else {
			// find out how much we would run out of electricity, if we would go
			// to the next parking
			return maxChargableEnergy.get(lastChargingPriceParkingIndex) - batteryCapacity;
		}
	}

	/**
	 * TODO: Test also, if the first parking is already so far, that we would
	 * run out of electricity, if we can handle this case properly...
	 * 
	 * @param agentEnergyConsumption
	 * 
	 * @return
	 */

	// assuming, there is enough electricity in the car for driving the whole
	// day
	// TODO: for more general case
	// TODO: need to take min Energy into consideration!!!
	public ChargingTimes getChargingTimes(EnergyConsumption agentEnergyConsumption) {
		
		// this should only be called once (return immediatly if already
		// populated)

		int indexOfLastParking = parkingTimes.size() - 1;

		if (chargingTimes.getChargingTimes().size() > 0) {
			return chargingTimes;
		}

		// initialize chargingPrice

		PriorityQueue<FacilityChargingPrice> chargingPrice = updateChargingPrice(new PriorityQueue<FacilityChargingPrice>());

		// at home the car must have reached 'minEnergyLevelToCharge'
		// or the priority queue must be empty but this should be reported,
		// because it means, the car could not charge fully
		// see below
		while (maxChargableEnergy.get(indexOfLastParking) > 0) {
			FacilityChargingPrice bestEnergyPrice = chargingPrice.poll();

			// only for debugging
			// assert the following (a bit expensive operation)
			// if
			// (chargingTimes.getTotalEnergyCharged()>agentEnergyConsumption.getTotalEnergyConsumption()){
			// System.out.println(chargingTimes.getTotalEnergyCharged() + " - "
			// + agentEnergyConsumption.getTotalEnergyConsumption());
			// System.out.println();
			// }

			// System.out.println(chargingTimes.getTotalEnergyCharged() + " - "
			// + agentEnergyConsumption.getTotalEnergyConsumption());

			if (bestEnergyPrice == null) {
				// TODO: report, that the current vehicle could not reach the
				// destination, because the electricity was finished
				// there should be some input here to the utility function
				// (gasoline or if el. vehicle, bad utility => switch mode)
				log.info("if this is an electric vehicle, then it has run out of power");
				break;
			}
			
			// get the parking index, where this car will charge
			int parkingIndex = bestEnergyPrice.getEnergyBalanceParkingIndex();

			// the maximum energy, that can be charged at a parking (because we
			// can only charge as much as the car has driven previously)
			double maximumEnergyThatCanBeCharged = maxChargableEnergy.get(parkingIndex);

			// the minimum energy that needs to be charged, either to reach the
			// next parking or
			// to fillup the electricity at the end of the day
			double minimumEnergyThatNeedsToBeCharged = getMinimumEnergyThatNeedsToBeCharged();

			// skip the charging slot, if no charging at the current parking is
			// needed
			// TODO: this can be made more efficient later (perhaps)
			if (maxChargableEnergy.get(parkingIndex) == 0) {
				continue;
			}

			double energyCharged = bestEnergyPrice
					.getEnergyCharge(minimumEnergyThatNeedsToBeCharged, maximumEnergyThatCanBeCharged);

			// set energyCharged to maximumEnergyThatNeedsToBeCharged, if they
			// are very close, to counter
			// rounding errors.
			double estimatedAmountOfEnergyCharged = Math.min(minimumEnergyThatNeedsToBeCharged, maximumEnergyThatCanBeCharged);
			int precision = 100000;
			if (Math.abs(estimatedAmountOfEnergyCharged * precision - energyCharged * precision) <= 1) {
				energyCharged = estimatedAmountOfEnergyCharged;
			}

			ChargeLog chargeLog = bestEnergyPrice.getChargeLog(minimumEnergyThatNeedsToBeCharged, maximumEnergyThatCanBeCharged);

			chargingTimes.addChargeLog(chargeLog);
			// chargeLog.print();
			
			updateMaxChargableEnergy(parkingIndex, energyCharged);
			checkMaxChargableEnergyInvariant();

			/*
			 * We want to make sure, that unused parts of a slot can later be
			 * used.
			 * 
			 * This means, if getEndTimeOfCharge < getEndTimeOfSlot then we
			 * should put this FacilityChargingPrice again into the queue and
			 * update it. But we should not add such FacilityChargingPrice into
			 * the queue, which are at the end of the parking and their first
			 * part has been used and the rest cannot be used.
			 */
			
			double endTimeOfCharge = bestEnergyPrice.getEndTimeOfCharge(minimumEnergyThatNeedsToBeCharged,
					maximumEnergyThatCanBeCharged);

			if (endTimeOfCharge < bestEnergyPrice.getEndTimeOfSlot()
					&& bestEnergyPrice.getSlotStartTime() != bestEnergyPrice.getEndParkingTime()) {
				bestEnergyPrice.setSlotStartTime(endTimeOfCharge);
				chargingPrice.add(bestEnergyPrice);
			}

			// if possible, more parkings should be taken into consideration
			updateChargingPrice(chargingPrice);
			
		}

		// update the state of charge...
		chargingTimes.updateSOCs(agentEnergyConsumption);

		return chargingTimes;
	}

	private void updateMaxChargableEnergy(int indexOfUsedParkingToCharge, double amountOfEnergy) {
		// from all the charging possibilities coming in future can be deduced the charging value ('amount of energy')
		for (int i = indexOfUsedParkingToCharge; i < maxChargableEnergy.size(); i++) {
			double oldValue = maxChargableEnergy.get(i);
			double newValue = oldValue - amountOfEnergy;

			maxChargableEnergy.remove(i);
			maxChargableEnergy.add(i, newValue);
		}
		
		
		// trim the energy charging amount of all previous parking as of the current parking, where the charging is happening
		double currentParkingMaxChargableEnergy=maxChargableEnergy.get(indexOfUsedParkingToCharge);
		for (int i = 0; i < indexOfUsedParkingToCharge; i++) {
			double oldValue = maxChargableEnergy.get(i);
			if (oldValue>currentParkingMaxChargableEnergy){
				double newValue = currentParkingMaxChargableEnergy;
				maxChargableEnergy.remove(i);
				maxChargableEnergy.add(i, newValue);
			}
		}
		
	}

}
