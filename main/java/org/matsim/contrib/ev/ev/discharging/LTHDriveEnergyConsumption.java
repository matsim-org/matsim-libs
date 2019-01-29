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

package org.matsim.contrib.ev.ev.discharging;/*
 * created by jbischoff, 23.08.2018
 */

import com.google.common.primitives.Doubles;
import com.google.inject.OutOfScopeException;
import org.apache.commons.math3.analysis.interpolation.PiecewiseBicubicSplineInterpolatingFunction;
import org.apache.commons.math3.analysis.interpolation.PiecewiseBicubicSplineInterpolator;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.ev.EvUnits;
import org.matsim.core.gbl.Gbl;
import org.matsim.vehicles.VehicleType;

public class LTHDriveEnergyConsumption implements DriveEnergyConsumption {

	private PiecewiseBicubicSplineInterpolator splineInterpolater = new PiecewiseBicubicSplineInterpolator();
	private PiecewiseBicubicSplineInterpolatingFunction function;

	private final double minSpeed;
	private final double maxSpeed;
	private final double minSlope;
	private final double maxSlope;

	private boolean hasWarnedMaxSpeed = false;
	private boolean hasWarnedMinSpeed = false;

	private final Id<VehicleType> vehicleTypeId;
	private final boolean crashIfOutOfBoundValue;

	public LTHDriveEnergyConsumption(double[] speeds, double[] slopes, double[][] consumptionPerSpeedAndSlope,
			Id<VehicleType> vehicleTypeId, boolean crashIfOutOfBoundValue) {
		this.function = splineInterpolater.interpolate(speeds, slopes, consumptionPerSpeedAndSlope);
		this.minSpeed = Doubles.min(speeds);
		this.maxSpeed = Doubles.max(speeds);
		this.minSlope = Doubles.min(slopes);
		this.maxSlope = Doubles.max(slopes);
		this.vehicleTypeId = vehicleTypeId;
		this.crashIfOutOfBoundValue = crashIfOutOfBoundValue;
	}

	@Override
	public double calcEnergyConsumption(Link link, double travelTime) {
		double length = link.getLength();
		double speed = length / travelTime;

		if (speed > maxSpeed) {
			if (crashIfOutOfBoundValue) {
				throw new OutOfScopeException("Speed not covered" + speed);
			} else {
				if (!hasWarnedMaxSpeed) {
					Logger.getLogger(getClass())
							.warn("Assuming maxSpeed, as Speed not covered by consumption data " + speed);
					Logger.getLogger(getClass()).warn(Gbl.ONLYONCE);
					hasWarnedMaxSpeed = true;
				}
				speed = maxSpeed;
			}
		}

		if (speed < minSpeed) {
			if (crashIfOutOfBoundValue) {
				throw new OutOfScopeException("Speed not covered" + speed);
			} else {
				if (!hasWarnedMinSpeed) {
					Logger.getLogger(getClass())
							.warn("Assuming minSpeed, as Speed not covered by consumption data " + speed);
					Logger.getLogger(getClass()).warn(Gbl.ONLYONCE);
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
		for (int i = 0; i < linkslopes.length; i++) {
			double currentSlope = checkSlope(linkslopes[i]);
			double currentEnergyuse = function.value(speed, currentSlope);
			consumption += currentEnergyuse * slopeSegmentTravelDistance;
		}
		return EvUnits.kWh_to_J(consumption);
	}

	private double checkSlope(double currentSlope) {
		if (currentSlope < minSlope) {
			if (crashIfOutOfBoundValue) {
				throw new OutOfScopeException("Slope not covered" + currentSlope);
			} else {
				Logger.getLogger(getClass())
						.warn("Assuming minSlope, as Slope not covered by consumption data" + currentSlope);
				currentSlope = minSlope;
			}
		} else if (currentSlope > maxSlope) {
			if (crashIfOutOfBoundValue) {
				throw new OutOfScopeException("Slope not covered" + currentSlope);
			} else {
				Logger.getLogger(getClass())
						.warn("Assuming maxSlope, as Slope not covered by consumption data" + currentSlope);
				currentSlope = maxSlope;
			}
		}
		return currentSlope;
	}
}
