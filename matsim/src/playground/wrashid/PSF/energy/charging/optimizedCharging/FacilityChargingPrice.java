package playground.wrashid.PSF.energy.charging.optimizedCharging;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.mobsim.jdeqsim.Message;

import playground.wrashid.PSF.energy.charging.ChargeLog;
import playground.wrashid.PSF.parking.ParkingInfo;

public class FacilityChargingPrice implements Comparable<FacilityChargingPrice> {

	private double price; // in utils per [J]
	private int timeSlotNumber;
	private int energyBalanceParkingIndex;
	private double slotStartTime; // for finding out the bin / charge log
	private Id facilityId;
	private double endParkingTime;

	/*
	 * the time starts with the first car leg of the person. When the car is
	 * parked, determine all the time slots, for the duration of the parking and
	 * give them a number based on 15 minute bins (0: staringTimeOfFirstLeg, 1:
	 * staringTimeOfFirstLeg+15,...) => all times below the
	 * staringTimeOfFirstLeg have a slot number bigger than 1000 (starting from
	 * 00:00)
	 * 
	 */

	// TODO: we need the right constructor here...
	// (startingTimeOfFirstLeg,price, timeSlotNumber,...
	// TODO: probably some other back pointer needed here...
	// private EnergyBalance energyBalance;
	public int getEnergyBalanceParkingIndex() {
		return energyBalanceParkingIndex;
	}

	public FacilityChargingPrice(double price, int timeSlotNumber, int energyBalanceParkingIndex, double slotStartTime,
			Id facilityId, double endParkingTime) {
		super();
		this.price = price;
		this.timeSlotNumber = timeSlotNumber;
		this.energyBalanceParkingIndex = energyBalanceParkingIndex;
		this.slotStartTime = slotStartTime;
		this.facilityId = facilityId;
		this.endParkingTime = endParkingTime;
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

	public ChargeLog getChargeLog(double maxChargableEnergy) {
		double startChargingTime = slotStartTime;

		return new ChargeLog(facilityId, startChargingTime, getEndTimeOfCharge(maxChargableEnergy));
	}

	// how much energy will be charged through this slot and facility
	public double getEnergyCharge(double maxChargableEnergy) {
		return ParkingInfo.getParkingElectricityPower(facilityId) * (getEndTimeOfCharge(maxChargableEnergy) - slotStartTime);
	}

	/*
	 * find out the end time of the charge
	 */
	private double getEndTimeOfCharge(double maxChargableEnergy) {

		// how much time would be needed to charge the car fully at the current
		// parking
		double durationNeededForMaxCharging = maxChargableEnergy / ParkingInfo.getParkingElectricityPower(facilityId);

		// by default, we use the whole slot for charging
		double endTimeOfCharge = slotStartTime + 900;

		// we cannot charge longer than we are parked there
		if (endParkingTime < endTimeOfCharge) {
			endTimeOfCharge = endParkingTime;
		}

		// still, we do not need the whole slot for charging (car needs less
		// energy)
		if (endParkingTime > slotStartTime + durationNeededForMaxCharging) {
			endTimeOfCharge = slotStartTime + durationNeededForMaxCharging;
		}

		return endTimeOfCharge;
	}

}
