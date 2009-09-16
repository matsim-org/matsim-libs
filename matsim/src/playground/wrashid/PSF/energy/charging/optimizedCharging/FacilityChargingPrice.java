package playground.wrashid.PSF.energy.charging.optimizedCharging;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.mobsim.jdeqsim.Message;

import playground.wrashid.PSF.energy.charging.ChargeLog;
import playground.wrashid.PSF.parking.ParkingInfo;

public class FacilityChargingPrice implements Comparable<FacilityChargingPrice> {

	private static final Logger log = Logger.getLogger(FacilityChargingPrice.class);

	private double price; // in utils per [J]
	private int timeSlotNumber;
	private int energyBalanceParkingIndex;
	private double slotStartTime; // for finding out the bin / charge log
	private Id facilityId;
	private double endParkingTime;

	public double getEndParkingTime() {
		return endParkingTime;
	}

	private double startParkingTime;

	private double endTimeOfSlot;
	private Id linkId;

	/*
	 * the time starts with the first car leg of the person. When the car is
	 * parked, determine all the time slots, for the duration of the parking and
	 * give them a number based on 15 minute bins (0: staringTimeOfFirstLeg, 1:
	 * staringTimeOfFirstLeg+15,...) => all times below the
	 * staringTimeOfFirstLeg have a slot number bigger than 1000 (starting from
	 * 00:00)
	 */

	// TODO: we need the right constructor here...
	// (startingTimeOfFirstLeg,price, timeSlotNumber,...
	// TODO: probably some other back pointer needed here...
	// private EnergyBalance energyBalance;
	public int getEnergyBalanceParkingIndex() {
		return energyBalanceParkingIndex;
	}

	public FacilityChargingPrice(double price, int timeSlotNumber, int energyBalanceParkingIndex, double slotStartTime,
			Id facilityId, double startParkingTime, double endParkingTime, Id linkId) {
		super();
		this.price = price;
		this.timeSlotNumber = timeSlotNumber;
		this.energyBalanceParkingIndex = energyBalanceParkingIndex;
		this.slotStartTime = slotStartTime;
		this.facilityId = facilityId;
		this.endParkingTime = endParkingTime;
		this.startParkingTime = startParkingTime;
		this.linkId = linkId;
		endTimeOfSlot = slotStartTime + 900;

		// this is possible, because parking can start in the evening and stop
		// in the morning...
		if (startParkingTime > endParkingTime) {
			// log.error("startParkingTime cannot be bigger than endParkingTime!");
			// System.exit(-1);
		}
	}

	public int compareTo(FacilityChargingPrice otherChargingPrice) {
		if (price > otherChargingPrice.getPrice()) {
			return 1;
		} else if (price < otherChargingPrice.getPrice()) {
			return -1;
		} else {
			// this does not work with cyclic time...
			return timeSlotNumber - otherChargingPrice.getTimeSlotNumber();
		}
	}

	public double getPrice() {
		return price;
	}

	public int getTimeSlotNumber() {
		return timeSlotNumber;
	}

	public ChargeLog getChargeLog(double minimumEnergyThatNeedsToBeCharged, double maxChargableEnergy) {
		double startChargingTime = slotStartTime < startParkingTime ? startParkingTime : slotStartTime;
		double endChargingTime = getEndTimeOfCharge(minimumEnergyThatNeedsToBeCharged, maxChargableEnergy);

		// this is possible, because parking can start in the evening and stop
		// in the morning...
		if (startChargingTime > endChargingTime) {
			// log.error("start charging time cannot be bigger than end charging time!");
			// System.exit(-1);
		}

		return new ChargeLog(linkId, facilityId, startChargingTime, endChargingTime);
	}

	// how much energy will be charged through this slot and facility
	public double getEnergyCharge(double minimumEnergyThatNeedsToBeCharged, double maxChargableEnergy) {
		double startChargingTime = slotStartTime < startParkingTime ? startParkingTime : slotStartTime;
		double electricityPower = ParkingInfo.getParkingElectricityPower(facilityId);
		double chargingDuration = getEndTimeOfCharge(minimumEnergyThatNeedsToBeCharged, maxChargableEnergy) - startChargingTime;
		double energyCharged = electricityPower * chargingDuration;

		if (energyCharged < 0) {
			log.error("the energy is negative!");
			System.exit(-1);
		}

		return energyCharged;
	}

	/*
	 * find out the end time of the charge
	 */
	public double getEndTimeOfCharge(double minimumEnergyThatNeedsToBeCharged, double maxChargableEnergy) {

		double startChargingTime = slotStartTime < startParkingTime ? startParkingTime : slotStartTime;

		// how much time would be needed to charge the car fully at the current
		// parking

		// normally, minimum energy that needs to be charged should be smaller
		// than the maximum energy that can be charged
		// but this is not always the case: when the car reaches the last
		// parking, then the overall goal is to charge the car fully
		// till the end of the iteration. In this case
		// minimumEnergyThatNeedsToBeCharged can be bigger than
		// maxChargableEnergy
		// therefore the min of these values is should be taken
		double durationNeededForRequiredCharging = Math.min(minimumEnergyThatNeedsToBeCharged, maxChargableEnergy)
				/ ParkingInfo.getParkingElectricityPower(facilityId);

		// by default, we can charge till the end of the charging slot
		double endTimeOfCharge = endTimeOfSlot;

		// the last (home) parking - parking ending after midnight
		if (slotStartTime > endParkingTime) { 

			// the full time slot is available for charging, we do not need to
			// make the check

			// if still, we do not need the whole slot for charging (car needs
			// less
			// energy)
			if (endTimeOfCharge > startChargingTime + durationNeededForRequiredCharging) {
				endTimeOfCharge = startChargingTime + durationNeededForRequiredCharging;
			}

		} else if (slotStartTime<endParkingTime && endTimeOfSlot>startParkingTime){
			
			// there is a special case, where departing from home happens within the same slot 
			// as arriving at work. And the car owner leaves the car at work and walks later home.
			// This case should be handled in the same way, as the last home parking, but it is put
			// into a separate case for clarity. => test7OptimizedCharger covers this (config6.xml)
			if (endTimeOfCharge > startChargingTime + durationNeededForRequiredCharging) {
				endTimeOfCharge = startChargingTime + durationNeededForRequiredCharging;
			}
			
		} else {

			// all other parking during the day

			// we cannot charge longer than we are parked there
			if (endParkingTime < endTimeOfCharge) {
				endTimeOfCharge = endParkingTime;
			}

			// if still, we do not need the whole slot for charging (car needs
			// less
			// energy)
			if (endTimeOfCharge > startChargingTime + durationNeededForRequiredCharging) {
				endTimeOfCharge = startChargingTime + durationNeededForRequiredCharging;
			}
		}

		if (endTimeOfCharge - startChargingTime<0) {
			log.error("startChargingTime needs to be bigger than end charging time!");
			System.exit(-1);
		}

		return endTimeOfCharge;
	}

	public double getSlotStartTime() {
		return slotStartTime;
	}

	public double getEndTimeOfSlot() {
		return endTimeOfSlot;
	}

	public void setEndTimeOfSlot(double endTimeOfSlot) {
		this.endTimeOfSlot = endTimeOfSlot;
	}

	public void setSlotStartTime(double slotStartTime) {
		this.slotStartTime = slotStartTime;
	}

}
