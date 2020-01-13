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
import org.matsim.contrib.ev.infrastructure.Charger;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

/**
 * @author Michal Maciejewski (michalm)
 */
public class VariableSpeedCharging implements ChargingPower {//TODO upgrade to BatteryCharging

	public static class Point {
		private final double relativeSoc;
		private final double relativePower;

		public Point(double relativeSoc, double relativeSpeed) {
			Preconditions.checkArgument(relativeSoc >= 0 && relativeSoc <= 1, "Relative SOC must be in [0,1]");
			//XXX To avoid infinite charging simulation (e.g. at SOC==0 or close to 1.0)
			Preconditions.checkArgument(relativeSpeed > 0, "Relative speed must be positive");
			this.relativeSoc = relativeSoc;
			this.relativePower = relativeSpeed;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("relativeSoc", relativeSoc)
					.add("relativePower", relativePower)
					.toString();
		}
	}

	public static VariableSpeedCharging createForTesla(ElectricVehicle electricVehicle) {
		Point pointA = new Point(0, 0.75);// 0% => 0.75 C
		Point pointB = new Point(0.15, 1.5);// 15% => 1.5 C
		Point pointC = new Point(0.5, 1.5);// 50% => 1.5 C
		Point pointD = new Point(1.0, 0.05);// 100% => 0.05 C
		return new VariableSpeedCharging(electricVehicle, pointA, pointB, pointC, pointD);
	}

	public static VariableSpeedCharging createForNissanLeaf(ElectricVehicle electricVehicle) {
		Point pointA = new Point(0, 0.75);// 0% => 0.75 C
		Point pointB = new Point(0.1, 1.75);// 10% => 1.75 C
		Point pointC = new Point(0.6, 1.75);// 60% => 1.75 C
		Point pointD = new Point(1.0, 0.05);// 100% => 0.05 C
		return new VariableSpeedCharging(electricVehicle, pointA, pointB, pointC, pointD);
	}

	private final ElectricVehicle electricVehicle;
	private final Point pointA;
	private final Point pointB;
	private final Point pointC;
	private final Point pointD;

	public VariableSpeedCharging(ElectricVehicle electricVehicle, Point pointA, Point pointB, Point pointC,
			Point pointD) {
		//checks whether the A-B-C-D profile
		Preconditions.checkArgument(pointA.relativeSoc == 0, "PointA.relativeSoc must be 0");
		Preconditions.checkArgument(pointD.relativeSoc == 1, "PointB.relativeSoc must be 1");
		Preconditions.checkArgument(pointB.relativeSoc != 0, "PointB.relativeSoc must be greater than 0");
		Preconditions.checkArgument(pointC.relativeSoc != 1, "PointB.relativeSoc must be less than 1");
		Preconditions.checkArgument(pointB.relativeSoc < pointC.relativeSoc,
				"PointB.relativeSoc must be less than PointC.relativeSoc");
		Preconditions.checkArgument(pointA.relativePower <= pointB.relativePower,
				"PointA.relativePower must not be greater than PointB.relativePower");
		Preconditions.checkArgument(pointD.relativePower <= pointC.relativePower,
				"PointD.relativePower must not be greater than PointC.relativePower");

		this.electricVehicle = electricVehicle;
		this.pointA = pointA;
		this.pointB = pointB;
		this.pointC = pointC;
		this.pointD = pointD;
	}

	@Override
	public double calcChargingPower(Charger charger) {
		Battery b = electricVehicle.getBattery();
		double relativeSoc = b.getSoc() / b.getCapacity();
		double c = b.getCapacity() / 3600.;

		if (relativeSoc <= pointB.relativeSoc) {
			return Math.min(charger.getPlugPower(), c * approxRelativePower(relativeSoc, pointA, pointB));
		} else if (relativeSoc <= pointC.relativeSoc) {
			return Math.min(charger.getPlugPower(), c * approxRelativePower(relativeSoc, pointB, pointC));
		} else {
			return Math.min(charger.getPlugPower(), c * approxRelativePower(relativeSoc, pointC, pointD));
		}
	}

	private double approxRelativePower(double relativeSoc, Point point0, Point point1) {
		double a = (relativeSoc - point0.relativeSoc) / (point1.relativeSoc - point0.relativeSoc);
		return point0.relativePower + a * (point1.relativePower - point0.relativePower);
	}

	//TODO convert to: calcChargingTime(Charger charger, double energy)
	public double calcRemainingTimeToCharge(Charger charger) {
		Battery b = electricVehicle.getBattery();
		double relativeSoc = b.getSoc() / b.getCapacity();
		double c = b.getCapacity() / 3600.;
		double relativeChargerPower = charger.getPlugPower() / c;

		final Point adjustedPointA;
		final Point adjustedPointB;
		if (pointA.relativePower >= relativeChargerPower) {
			adjustedPointA = new Point(0, relativeChargerPower);
			adjustedPointB = new Point(pointB.relativeSoc, relativeChargerPower);
		} else {
			adjustedPointA = pointA;
			adjustedPointB = adjustPointIfSlowerCharging(relativeChargerPower, pointA, pointB);
		}

		final Point adjustedPointD;
		final Point adjustedPointC;
		if (pointD.relativePower >= relativeChargerPower) {//rather unlikely
			adjustedPointD = new Point(1, relativeChargerPower);
			adjustedPointC = new Point(pointC.relativeSoc, relativeChargerPower);
		} else {
			adjustedPointD = pointD;
			adjustedPointC = adjustPointIfSlowerCharging(relativeChargerPower, pointD, pointC);
		}

		if (relativeSoc <= adjustedPointB.relativeSoc) {
			return approximateRemainingChargeTime(relativeSoc, adjustedPointA, adjustedPointB)//
					+ approxChargeTime(adjustedPointB, adjustedPointC)//
					+ approxChargeTime(adjustedPointC, adjustedPointD);
		} else if (relativeSoc <= adjustedPointC.relativeSoc) {
			return approximateRemainingChargeTime(relativeSoc, adjustedPointB, adjustedPointC)//
					+ approxChargeTime(adjustedPointC, adjustedPointD);
		} else {
			return approximateRemainingChargeTime(relativeSoc, adjustedPointC, adjustedPointD);
		}
	}

	private Point adjustPointIfSlowerCharging(double relativeChargerPower, Point lowerPoint, Point higherPoint) {
		if (relativeChargerPower >= higherPoint.relativePower) {
			return higherPoint;
		}

		double a = (relativeChargerPower - lowerPoint.relativePower) / (higherPoint.relativePower
				- lowerPoint.relativePower);
		double relativeSoc = lowerPoint.relativeSoc + a * (higherPoint.relativeSoc - lowerPoint.relativeSoc);
		return new Point(relativeSoc, relativeChargerPower);
	}

	private double approximateRemainingChargeTime(double relativeSoc, Point point0, Point point1) {
		Point currentPoint = new Point(relativeSoc, approxRelativePower(relativeSoc, point0, point1));
		return approxChargeTime(currentPoint, point1);
	}

	private double approxChargeTime(Point point0, Point point1) {
		return 3600. * (point1.relativeSoc - point0.relativeSoc) / (point1.relativePower + point0.relativePower);
	}
}
