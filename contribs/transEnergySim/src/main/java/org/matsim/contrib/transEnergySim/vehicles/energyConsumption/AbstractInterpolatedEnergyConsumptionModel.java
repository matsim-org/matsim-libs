/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.transEnergySim.vehicles.energyConsumption;

import java.util.Iterator;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.parkingchoice.lib.DebugLib;

public abstract class AbstractInterpolatedEnergyConsumptionModel implements EnergyConsumptionModel{

	protected PriorityQueue<EnergyConsumption> queue = new PriorityQueue<EnergyConsumption>();
	private EnergyConsumption zeroSpeedConsumption = new EnergyConsumption(0, 0);
	
	/**
	 * 
	 * Gives the interpolated energy consumption. Speed must be inside the
	 * original interval (borders included).
	 * 
	 * @param consumptionA
	 * @param consumptionB
	 * @param speed
	 * @return
	 */
	protected static double getInterpolatedEnergyConsumption(EnergyConsumption consumptionA, EnergyConsumption consumptionB,
			double speed) {
		EnergyConsumption smallerSpeedEC;
		EnergyConsumption biggerSpeedEC;

		if (consumptionA.getSpeed() < consumptionB.getSpeed()) {
			smallerSpeedEC = consumptionA;
			biggerSpeedEC = consumptionB;
		} else if (consumptionA.getSpeed() < consumptionB.getSpeed()) {
			smallerSpeedEC = consumptionB;
			biggerSpeedEC = consumptionA;
		} else {
			return (consumptionA.getEnergyConsumption() + consumptionB.getEnergyConsumption()) / 2;
		}

		if (speed < smallerSpeedEC.getSpeed() || speed > biggerSpeedEC.getSpeed()) {
			DebugLib.stopSystemAndReportInconsistency("input speed is not inside given interval");
		}

		if (speed == smallerSpeedEC.getSpeed()) {
			return smallerSpeedEC.getEnergyConsumption();
		}

		if (speed == biggerSpeedEC.getSpeed()) {
			return biggerSpeedEC.getEnergyConsumption();
		}

		double differenceSpeed = biggerSpeedEC.getSpeed() - smallerSpeedEC.getSpeed();
		double differenceEnergyConsumption = biggerSpeedEC.getEnergyConsumption() - smallerSpeedEC.getEnergyConsumption();

		double interpolationFactor = differenceEnergyConsumption / differenceSpeed;

		double result = smallerSpeedEC.getEnergyConsumption() + interpolationFactor * (speed - smallerSpeedEC.getSpeed());
		return result;
	}
	
	/**
	 * @param speedInMetersPerSecond
	 * @param distanceInMeters
	 * @return
	 */
	protected double getInterpolatedEnergyConsumption(double speedInMetersPerSecond, double distanceInMeters) {
		Iterator<EnergyConsumption> iter = queue.iterator();

		EnergyConsumption currentAverageConsumption = iter.next();

		if (ifSmallerThanMimimumSpeed(speedInMetersPerSecond, currentAverageConsumption)) {
			return currentAverageConsumption.getEnergyConsumption() * distanceInMeters;
		}
		EnergyConsumption previousConsumption = null;

		while (currentAverageConsumption.getSpeed() < speedInMetersPerSecond && iter.hasNext()) {
			previousConsumption = currentAverageConsumption;
			currentAverageConsumption = iter.next();
		}

		if (ifHigherThanMaxSpeed(speedInMetersPerSecond, currentAverageConsumption)) {
			return currentAverageConsumption.getEnergyConsumption() * distanceInMeters;
		} else {
			return getInterpolatedEnergyConsumption(previousConsumption, currentAverageConsumption, speedInMetersPerSecond)
					* distanceInMeters;
		}
	}
	
	private boolean ifHigherThanMaxSpeed(double speedInMetersPerSecond, EnergyConsumption currentAverageConsumption) {
		return currentAverageConsumption.getSpeed() < speedInMetersPerSecond;
	}

	private boolean ifSmallerThanMimimumSpeed(double speedInMetersPerSecond, EnergyConsumption currentAverageConsumption) {
		return currentAverageConsumption.getSpeed() >= speedInMetersPerSecond;
	}
	
	@Override
	public double getEnergyConsumptionForLinkInJoule(Link link, double averageSpeedDriven) {
		return getEnergyConsumptionForLinkInJoule(link.getLength(), -1, averageSpeedDriven);
	}

	@Override
	public double getEnergyConsumptionForLinkInJoule(double drivenDistanceInMeters, double maxSpeedOnLink, double averageSpeedDriven) {
		return getInterpolatedEnergyConsumption(averageSpeedDriven, drivenDistanceInMeters);
	}
}
