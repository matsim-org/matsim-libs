/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.qsimengine;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RailsimCalcTest {

	@Test
	void testCalcAndSolveTraveledDist() {

		assertThat(RailsimCalc.calcTraveledDist(5, 2, 0))
			.isEqualTo(10);

		assertThat(RailsimCalc.solveTraveledDist(5, 15, 0))
			.isEqualTo(3);

		double d = RailsimCalc.calcTraveledDist(5, 3, 1);

		assertThat(d)
			.isEqualTo(19.5);

		assertThat(RailsimCalc.solveTraveledDist(5, 19.5, 1))
			.isEqualTo(3);


	}

	@Test
	void testCalcAndSolveTraveledDistNegative() {

		double d = RailsimCalc.calcTraveledDist(5, 5, -1);

		assertThat(d).isEqualTo(12.5);

		assertThat(RailsimCalc.calcTraveledDist(5, 10, -1))
			.isEqualTo(0);

		double t = RailsimCalc.solveTraveledDist(5, 12.5, -1);

		assertThat(t)
			.isEqualTo(5);

	}

	@Test
	void testMaxSpeed() {

		double dist = 1000;

		double current = 5;
		double f = 0;

		RailsimCalc.SpeedTarget res = RailsimCalc.calcTargetSpeed(dist, 0.5, 0.5,
			5, 30, 0);

		double timeDecel = (res.targetSpeed() - f) / 0.5;
		double distDecel = RailsimCalc.calcTraveledDist(res.targetSpeed(), timeDecel, -0.5);

		double timeAccel = (res.targetSpeed() - current) / 0.5;
		double distAccel = RailsimCalc.calcTraveledDist(5, timeAccel, 0.5);

		assertThat(distDecel + distAccel)
			.isCloseTo(dist, Offset.offset(0.001));

	}

	@Test
	void testCalcTargetDecel() {

		double d = RailsimCalc.calcTargetDecel(1000, 0, 10);

		assertThat(RailsimCalc.calcTraveledDist(10, -10 / d, d))
			.isCloseTo(1000, Offset.offset(0.001));

		d = RailsimCalc.calcTargetDecel(1000, 5, 10);

		assertThat(RailsimCalc.calcTraveledDist(10, -5 / d, d))
			.isCloseTo(1000, Offset.offset(0.001));

	}

	@Test
	void testCalcTargetSpeed() {

		RailsimCalc.SpeedTarget target = RailsimCalc.calcTargetSpeed(100, 0.5, 0.5, 0, 23, 0);


		double t = RailsimCalc.solveTraveledDist(0, 50, 0.5);

		// Train can not reach target speed and accelerates until 50m
		assertThat(target.decelDist())
			.isCloseTo(50, Offset.offset(0.0001));

		assertThat(RailsimCalc.calcTraveledDist(target.targetSpeed(), t, -0.5))
			.isCloseTo(50, Offset.offset(0.0001));


		target = RailsimCalc.calcTargetSpeed(200, 0.5, 0.5, 13, 13, 0);

		assertThat(target.targetSpeed())
			.isCloseTo(13, Offset.offset(0.0001));

		// assume travelling at max speed for 31m
		assertThat(target.decelDist())
			.isCloseTo(31, Offset.offset(0.0001));

		t = RailsimCalc.solveTraveledDist(13, 200 - 31, -0.5);

		// speed is 0 after decelerating rest of the distance
		assertThat(13 + t * -0.5)
			.isCloseTo(0, Offset.offset(0.001));

	}

	@Test
	void testCalcTargetSpeedForStop() {

		double v = RailsimCalc.calcTargetSpeedForStop(1000, 0.5, 0.5, 0);

		double accelTime = v / 0.5;

		double d1 = RailsimCalc.calcTraveledDist(0, accelTime, 0.5);
		double d2 = RailsimCalc.calcTraveledDist(v, accelTime, -0.5);

		assertThat(d1 + d2)
			.isCloseTo(1000, Offset.offset(0.0001));

	}

	@Test
	public void testCalcRequiredTime() {

		TrainState state = new TrainState(null, null, 0, null, null);

		state.speed = 0;
		state.acceleration = 0;

		assertThat(RailsimCalc.calcRequiredTime(state, 1000))
			.isEqualTo(Double.POSITIVE_INFINITY);

		state.speed = 10;
		state.acceleration = -1;

		// Comes to stop after 10s
		assertThat(RailsimCalc.calcRequiredTime(state, 10000))
			.isEqualTo(10);

	}
}
