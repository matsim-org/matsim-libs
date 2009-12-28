package playground.wrashid.PSF.energy.charging;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.energy.charging.optimizedCharging.EnergyBalance;

public class ChargeLog implements Comparable<ChargeLog> {

	private static final Logger log = Logger.getLogger(ChargeLog.class);

	private Id linkId;

	private double startChargingTime;
	private double endChargingTime;
	private double startSOC;
	private double endSOC;

	private Id facilityId;

	public double getStartSOC() {
		return startSOC;
	}

	public double getEndSOC() {
		return endSOC;
	}

	public Id getLinkId() {
		return linkId;
	}

	public double getStartChargingTime() {
		return startChargingTime;
	}

	public double getEndChargingTime() {
		return endChargingTime;
	}

	/*
	 * TODO - refactoring: Is it possible, that we just pass in the facilityId
	 * and the the facilityId and get the linkId from there?
	 */
	public ChargeLog(Id linkId, Id facilityId, double startChargingTime, double endChargingTime) {
		super();
		this.linkId = linkId;
		this.facilityId = facilityId;
		this.startChargingTime = startChargingTime;
		this.endChargingTime = endChargingTime;
	}

	/**
	 * This method takes the start state of charge and uses the delta between
	 * startChargingTime,endChargingTime for how long the charging was done with
	 * the given chargingPower (for calculating endSOC).
	 * 
	 * This method has the following problem: This Object can be created in an
	 * inconsistent way, because this should be done over the constructor. TODO:
	 * Redesign/refactor...
	 * 
	 * @param startSOC
	 * @param chargingPower
	 */
	public void updateSOC(double startSOC) {
		this.startSOC = startSOC;
		this.endSOC = this.startSOC + getEnergyCharged();
	}

	public double getEnergyCharged() {
		return (endChargingTime - startChargingTime) * ParametersPSF.getFacilityChargingPowerMapper().getChargingPower(facilityId);
	}

	public void print() {
		System.out.println("linkId: " + linkId + ", startChargingTime: " + startChargingTime + ", endChargingTime: "
				+ endChargingTime + ", startSOC: " + startSOC + ", endSOC: " + endSOC);
	}

	/**
	 * Two ChargeLogs are equal, if their chargeStartTime is equal... => That
	 * throws an error, because this should not be possible for the same
	 * agent... => that would mean a wrong usage of this class...
	 * 
	 * @param otherChargeLog
	 * @return
	 */
	public int compareTo(ChargeLog otherChargeLog) {
		if (startChargingTime > otherChargeLog.getStartChargingTime()) {
			return 1;
		} else if (startChargingTime < otherChargeLog.getStartChargingTime()) {
			return -1;
		} else {
			log.error("A car cannot charge using the same starting time twice...");
			System.exit(-1);
			return 0;
		}
	}

}
