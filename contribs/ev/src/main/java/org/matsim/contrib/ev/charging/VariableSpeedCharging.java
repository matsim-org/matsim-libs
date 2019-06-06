/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (pointC) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.ev.charging;

import org.matsim.contrib.ev.fleet.Battery;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

/**
 * @author Michal Maciejewski (michalm)
 */
public class VariableSpeedCharging implements ChargingStrategy {
	public static class Point {
		private final double relativeSoc;
		private final double relativePower;

		public Point(double relativeSoc, double relativePower) {
			this.relativeSoc = relativeSoc;
			this.relativePower = relativePower;
		}
	}

	public static VariableSpeedCharging createStrategyForTesla(double chargingPower, double maxRelativeSoc) {
		Point pointA = new Point(0, 0.75);// 0% => 0.75 C
		Point pointB = new Point(0.15, 1.5);// 15% => 1.5 C
		Point pointC = new Point(0.5, 1.5);// 50% => 1.5 C
		Point pointD = new Point(1.0, 0.05);// 100% => 0.05 C
		return new VariableSpeedCharging(chargingPower, maxRelativeSoc, pointA, pointB, pointC, pointD);
	}

	public static VariableSpeedCharging createStrategyForNissanLeaf(double chargingPower, double maxRelativeSoc) {
		Point pointA = new Point(0, 0.75);// 0% => 0.75 C
		Point pointB = new Point(0.1, 1.75);// 10% => 1.75 C
		Point pointC = new Point(0.6, 1.75);// 60% => 1.75 C
		Point pointD = new Point(1.0, 0.05);// 100% => 0.05 C
		return new VariableSpeedCharging(chargingPower, maxRelativeSoc, pointA, pointB, pointC, pointD);
	}

	private final double chargingPower;
	private final double maxRelativeSoc;

	private final Point pointA;
	private final Point pointB;
	private final Point pointC;
	private final Point pointD;

	//XXX To avoid infinite charging simulation at 0 or close to 1.0 ensure:
	// 1. pointD.relativePower > 0.0
	// 2. pointA.relativePower > 0.0
	public VariableSpeedCharging(double chargingPower, double maxRelativeSoc, Point pointA, Point pointB, Point pointC,
			Point pointD) {
		if (chargingPower <= 0) {
			throw new IllegalArgumentException("chargingPower must be positive");
		}
		if (maxRelativeSoc <= 0 || maxRelativeSoc > 1) {
			throw new IllegalArgumentException("maxRelativeSoc must be in (0,1]");
		}

		this.pointA = pointA;
		this.pointB = pointB;
		this.pointC = pointC;
		this.pointD = pointD;

		this.chargingPower = chargingPower;
		this.maxRelativeSoc = maxRelativeSoc;
	}

	@Override
	public double calcChargingPower(ElectricVehicle ev) {
		Battery b = ev.getBattery();
		double relativeSoc = b.getSoc() / b.getCapacity();
		double c = b.getCapacity() / 3600.;

		Point adjustedPointB = adjustPointIfSlowerCharging(c, pointA, pointB);
		Point adjustedPointC = adjustPointIfSlowerCharging(c, pointD, pointC);

		if (relativeSoc <= adjustedPointB.relativeSoc) {
			return c * approxRelativePower(relativeSoc, pointA, adjustedPointB);
		} else if (relativeSoc <= adjustedPointC.relativeSoc) {
			return c * approxRelativePower(relativeSoc, adjustedPointB, adjustedPointC);
		} else {
			return c * approxRelativePower(relativeSoc, adjustedPointC, pointD);
		}
	}

	private double approxRelativePower(double relativeSoc, Point point0, Point point1) {
		double a = (relativeSoc - point0.relativeSoc) / (point1.relativeSoc - point0.relativeSoc);
		return point0.relativePower + a * (point1.relativePower - point0.relativePower);
	}

	@Override
	public double calcRemainingEnergyToCharge(ElectricVehicle ev) {
		Battery b = ev.getBattery();
		return maxRelativeSoc * b.getCapacity() - b.getSoc();
	}

	@Override
	public double calcRemainingTimeToCharge(ElectricVehicle ev) {
		Battery b = ev.getBattery();
		double relativeSoc = b.getSoc() / b.getCapacity();
		double c = b.getCapacity() / 3600.;

		Point adjustedPointB = adjustPointIfSlowerCharging(c, pointA, pointB);
		Point adjustedPointC = adjustPointIfSlowerCharging(c, pointD, pointC);

		if (relativeSoc <= adjustedPointB.relativeSoc) {
			return approximateRemainingChargeTime(relativeSoc, pointA, adjustedPointB)//
					+ approxChargeTime(adjustedPointB, adjustedPointC)//
					+ approxChargeTime(adjustedPointC, pointD);
		} else if (relativeSoc <= adjustedPointC.relativeSoc) {
			return approximateRemainingChargeTime(relativeSoc, adjustedPointB, adjustedPointC)//
					+ approxChargeTime(adjustedPointC, pointD);
		} else {
			return approximateRemainingChargeTime(relativeSoc, adjustedPointC, pointD);
		}
	}

	private Point adjustPointIfSlowerCharging(double c, Point lowerPoint, Point higherPoint) {
		if (chargingPower >= c * higherPoint.relativePower) {
			return higherPoint;
		}

		double relativeChargingPower = chargingPower / c;
		double a = (relativeChargingPower - lowerPoint.relativePower) / (higherPoint.relativePower
				- lowerPoint.relativePower);
		double relativeSoc = lowerPoint.relativeSoc + a * (higherPoint.relativeSoc - lowerPoint.relativeSoc);
		return new Point(relativeSoc, relativeChargingPower);
	}

	private double approximateRemainingChargeTime(double relativeSoc, Point point0, Point point1) {
		Point currentPoint = new Point(relativeSoc, approxRelativePower(relativeSoc, point0, point1));
		return approxChargeTime(currentPoint, point1);
	}

	private double approxChargeTime(Point point0, Point point1) {
		return 3600. * (point1.relativeSoc - point0.relativeSoc) / (point1.relativePower + point0.relativePower);
	}
}
