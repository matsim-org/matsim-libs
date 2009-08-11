package playground.wrashid.PSF.energy.charging.optimizedCharging;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.mobsim.jdeqsim.Message;

public class FacilityChargingPrice implements Comparable<FacilityChargingPrice> {

	private double price; // in utils per [J]
	private int timeSlotNumber;
	/*
	 * the time starts with the first car leg of the person. When the car is
	 * parked, determine all the time slots, for the duration of the parking and
	 * give them a number based on 15 minute bins (0: staringTimeOfFirstLeg, 1:
	 * staringTimeOfFirstLeg+15,...)
	 *  => all times below the staringTimeOfFirstLeg have a slot number bigger than 1000 (starting from 00:00)
	 *  
	 */

	// TODO: we need the right constructor here... (startingTimeOfFirstLeg,price, timeSlotNumber,...
	// TODO: probably some other back pointer needed here...
	//private EnergyBalance energyBalance;

	public FacilityChargingPrice(double price, int timeSlotNumber) {
		super();
		this.price = price;
		this.timeSlotNumber = timeSlotNumber;
	}

	public int compareTo(FacilityChargingPrice otherChargingPrice) {
		if (price > otherChargingPrice.getPrice()) {
			return 1;
		} else if (price < otherChargingPrice.getPrice()) {
			return -1;
		} else {
			// this does not work with cyclic time...
			return timeSlotNumber-otherChargingPrice.getTimeSlotNumber();
		}
	}

	public double getPrice() {
		return price;
	}

	public int getTimeSlotNumber() {
		return timeSlotNumber;
	}

}
