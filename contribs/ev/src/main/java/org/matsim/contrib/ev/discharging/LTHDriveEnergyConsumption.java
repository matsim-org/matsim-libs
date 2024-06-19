/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.discharging;/*
 * created by jbischoff, 23.08.2018
 */

import org.apache.commons.math3.analysis.interpolation.PiecewiseBicubicSplineInterpolatingFunction;
import org.apache.commons.math3.analysis.interpolation.PiecewiseBicubicSplineInterpolator;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.core.gbl.Gbl;

import com.google.common.primitives.Doubles;

public final class LTHDriveEnergyConsumption implements DriveEnergyConsumption {

	private final PiecewiseBicubicSplineInterpolatingFunction function;

	private final double minSpeed;
	private final double maxSpeed;
	private final double minSlope;
	private final double maxSlope;

	private static boolean hasWarnedMaxSpeed = false;
	private static boolean hasWarnedMinSpeed = false;
	private static boolean hasWarnedMinSlope = false;
	private static boolean hasWarnedMaxSlope = false;

	private final boolean crashIfOutOfBoundValue;

	public static class Factory implements DriveEnergyConsumption.Factory {

		private final boolean crashIfOutOfBoundValue;
		private final double[] speeds;
		private final double[] slopes;
		private final double[][] consumptionPerSpeedAndSlope;

		public Factory(double[] speeds, double[] slopes, double[][] consumptionPerSpeedAndSlope,
				boolean crashIfOutOfBoundValue) {
			this.speeds = speeds;
			this.slopes = slopes;
			this.consumptionPerSpeedAndSlope = consumptionPerSpeedAndSlope;
			this.crashIfOutOfBoundValue = crashIfOutOfBoundValue;

		}

		@Override
		public DriveEnergyConsumption create(ElectricVehicle electricVehicle) {
			return new LTHDriveEnergyConsumption(speeds, slopes, consumptionPerSpeedAndSlope, crashIfOutOfBoundValue);
		}
	}

	private LTHDriveEnergyConsumption(double[] speeds, double[] slopes, double[][] consumptionPerSpeedAndSlope,
			boolean crashIfOutOfBoundValue) {
		this.function = new PiecewiseBicubicSplineInterpolator().interpolate(speeds, slopes,
				consumptionPerSpeedAndSlope);
		this.minSpeed = Doubles.min(speeds);
		this.maxSpeed = Doubles.max(speeds);
		this.minSlope = Doubles.min(slopes);
		this.maxSlope = Doubles.max(slopes);
		this.crashIfOutOfBoundValue = crashIfOutOfBoundValue;
	}

	@Override
	public double calcEnergyConsumption(Link link, double travelTime, double linkEnterTime) {
		double length = link.getLength();
		double speed = length / travelTime;

		if (speed > maxSpeed) {
			if (crashIfOutOfBoundValue) {
				throw new IllegalArgumentException("Speed greater than the supported maxSpeed; speed =" + speed);
			} else {
				if (!hasWarnedMaxSpeed) {
					LogManager.getLogger(getClass())
							.warn("Assuming maxSpeed, as Speed not covered by consumption data " + speed);
					LogManager.getLogger(getClass()).warn(Gbl.ONLYONCE);
					hasWarnedMaxSpeed = true;
				}
				speed = maxSpeed;
			}
		}

		if (speed < minSpeed) {
			if (crashIfOutOfBoundValue) {
				throw new IllegalArgumentException("Speed less than the supported minSpeed; speed =" + speed);
			} else {
				if (!hasWarnedMinSpeed) {
					LogManager.getLogger(getClass())
							.warn("Assuming minSpeed, as Speed not covered by consumption data " + speed);
					LogManager.getLogger(getClass()).warn(Gbl.ONLYONCE);
					hasWarnedMinSpeed = true;
				}
				speed = minSpeed;

			}

		}

		double[] linkslopes = (double[])link.getAttributes().getAttribute("slopes");
		if (linkslopes == null) {
			linkslopes = new double[] { 0.0 };
		}
		double slopeSegmentTravelDistance = (link.getLength() / 1000.0) / (double)linkslopes.length;
		double consumption = 0;
		for (double linkslope : linkslopes) {
			double currentSlope = checkSlope(linkslope);
			double currentEnergyuse = function.value(speed, currentSlope);
			consumption += currentEnergyuse * slopeSegmentTravelDistance;
		}
		return EvUnits.kWh_to_J(consumption);
	}

	private double checkSlope(double currentSlope) {
		if (currentSlope < minSlope) {
			if (crashIfOutOfBoundValue) {
				throw new IllegalArgumentException("Slope less than the supported minSlope; slope =" + currentSlope);
			} else {
				if (!hasWarnedMinSlope) {
					LogManager.getLogger(getClass())
							.warn("Assuming minSlope, as Slope not covered by consumption data" + currentSlope);
					hasWarnedMinSlope = true;
				}
				currentSlope = minSlope;

			}
		} else if (currentSlope > maxSlope) {
			if (crashIfOutOfBoundValue) {
				throw new IllegalArgumentException("Slope greater than the supported maxSlope; slope =" + currentSlope);
			} else {
				if (!hasWarnedMaxSlope) {
					LogManager.getLogger(getClass())
							.warn("Assuming maxSlope, as Slope not covered by consumption data" + currentSlope);
					hasWarnedMaxSlope = true;
				}
				currentSlope = maxSlope;

			}
		}
		return currentSlope;
	}
}
