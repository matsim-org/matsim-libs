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

import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class holding static calculation methods related to state (updates).
 */
public final class RailsimCalc {

	private RailsimCalc() {
	}

	/**
	 * Calculate traveled distance given initial speed and constant acceleration.
	 */
	static double calcTraveledDist(double speed, double elapsedTime, double acceleration) {
		return speed * elapsedTime + (elapsedTime * elapsedTime * acceleration / 2);
	}

	/**
	 * Inverse of {@link #calcTraveledDist(double, double, double)}, solves for distance, returns needed time.
	 */
	static double solveTraveledDist(double speed, double dist, double acceleration) {
		if (acceleration == 0)
			return dist / speed;

		return (Math.sqrt(2 * acceleration * dist + speed * speed) - speed) / acceleration;
	}

	/**
	 * Calculate time needed to advance distance {@code dist}. Depending on acceleration and max speed.
	 * If dist can never be reached, will return time needed to stop.
	 */
	static double calcRequiredTime(TrainState state, double dist) {

		if (FuzzyUtils.equals(dist, 0))
			return 0;

		if (state.acceleration == 0)
			return state.speed == 0 ? Double.POSITIVE_INFINITY : dist / state.speed;

		if (state.acceleration > 0) {

			double accelTime = (state.targetSpeed - state.speed) / state.acceleration;

			double d = calcTraveledDist(state.speed, accelTime, state.acceleration);

			// The required distance is reached during acceleration
			if (d > dist) {
				return solveTraveledDist(state.speed, dist, state.acceleration);

			} else
				// Time for accel plus remaining dist at max speed
				return accelTime + (dist - d) / state.targetSpeed;

		} else {

			double decelTime = -(state.speed - state.targetSpeed) / state.acceleration;

			// max distance that can be reached
			double max = calcTraveledDist(state.speed, decelTime, state.acceleration);

			if (FuzzyUtils.equals(dist, max)) {
				return decelTime;
			} else if (dist <= max) {
				return solveTraveledDist(state.speed, dist, state.acceleration);
			} else
				return decelTime;
		}
	}

	/**
	 * Calculate the maximum speed that can be reached under the condition that speed must be reduced to {@code allowedSpeed}
	 * again after traveled {@code dist}.
	 */
	static SpeedTarget calcTargetSpeed(double dist, double acceleration, double deceleration,
									   double currentSpeed, double allowedSpeed, double finalSpeed) {

		// Calculation is simplified if target is the same
		if (FuzzyUtils.equals(allowedSpeed, finalSpeed)) {
			return new SpeedTarget(finalSpeed, Double.POSITIVE_INFINITY);
		}

		// Distance could be zero, speeds must be already equal then
		if (FuzzyUtils.equals(dist, 0)) {
			assert FuzzyUtils.equals(currentSpeed, finalSpeed) : "Current speed must be equal to allowed speed";
			return new SpeedTarget(finalSpeed, 0);
		}

		double timeDecel = (allowedSpeed - finalSpeed) / deceleration;
		double distDecel = calcTraveledDist(allowedSpeed, timeDecel, -deceleration);

		// No further acceleration needed
		if (FuzzyUtils.equals(currentSpeed, allowedSpeed)) {
			double decelDist = dist - distDecel;

			// Start to stop now
			if (FuzzyUtils.equals(decelDist, 0)) {
				return new SpeedTarget(finalSpeed, 0);
			}

			// Decelerate later
			return new SpeedTarget(allowedSpeed, decelDist);
		}


//		assert FuzzyUtils.greaterEqualThan(allowedSpeed, currentSpeed) : "Current speed must be lower than allowed";
		assert FuzzyUtils.greaterEqualThan(allowedSpeed, finalSpeed) : "Final speed must be smaller than target";

		double timeAccel = (allowedSpeed - currentSpeed) / acceleration;
		double distAccel = calcTraveledDist(currentSpeed, timeAccel, acceleration);

		// there is enough distance to accelerate to the target speed
		if (FuzzyUtils.lessThan(distAccel + distDecel, dist)) {
			return new SpeedTarget(allowedSpeed, dist - distDecel);
		}

		double nom = 2 * acceleration * deceleration * dist
			+ acceleration * finalSpeed * finalSpeed
			+ deceleration * currentSpeed * currentSpeed;

		double v = Math.sqrt(nom / (acceleration + deceleration));

		timeDecel = (v - finalSpeed) / deceleration;
		distDecel = calcTraveledDist(v, timeDecel, -deceleration);

		return new SpeedTarget(v, dist - distDecel);
	}


	/**
	 * Calculate the deceleration needed to arrive at {@code targetSpeed} exactly after {@code dist}.
	 *
	 * @return negative acceleration, always a negative number.
	 */
	static double calcTargetDecel(double dist, double targetSpeed, double currentSpeed) {
		return -(currentSpeed * currentSpeed - targetSpeed * targetSpeed) / (2 * dist);
	}

	/**
	 * Calculate the maximum speed that can be achieved if trains want to stop after dist.
	 */
	static double calcTargetSpeedForStop(double dist, double acceleration, double deceleration, double currentSpeed) {

		double nom = 2 * acceleration * deceleration * dist
			+ deceleration * currentSpeed * currentSpeed;

		return Math.sqrt(nom / (acceleration + deceleration));
	}

	/**
	 * Calculate the minimum distance that needs to be reserved for the train, such that it can stop safely.
	 */
	static double calcReservationDistance(TrainState state, RailLink currentLink) {

		double assumedSpeed = calcPossibleMaxSpeed(state);

		// stop at end of link
		if (state.isStop(currentLink.getLinkId()))
			return currentLink.length - state.headPosition;

		double stopTime = assumedSpeed / state.train.deceleration();

		// safety distance
		double safety = RailsimCalc.calcTraveledDist(assumedSpeed, stopTime, -state.train.deceleration());

		double distToNextStop = currentLink.length - state.headPosition;
		int idx = state.routeIdx;

		while (idx < state.route.size()) {
			RailLink nextLink = state.route.get(idx++);
			distToNextStop += nextLink.length;

			if (state.isStop(nextLink.getLinkId()))
				break;
		}

		return Math.min(safety, distToNextStop);
	}

	/**
	 * Calculate the projected driven distance, based on current position and state.
	 */
	public static double projectedDistance(double time,  TrainPosition position) {

		if (!(position instanceof TrainState state))
			throw new IllegalArgumentException("Position must be a TrainState.");

		double elapsed = time - state.timestamp;

		if (elapsed == 0)
			return 0;

		double accelTime = (state.targetSpeed - state.speed) / state.acceleration;

		double dist;
		if (state.acceleration == 0) {
			dist = state.speed * elapsed;

		} else if (accelTime < elapsed) {
			// Travelled distance under constant acceleration
			dist = RailsimCalc.calcTraveledDist(state.speed, accelTime, state.acceleration);

			// Remaining time at constant speed
			if (state.acceleration > 0)
				dist += RailsimCalc.calcTraveledDist(state.targetSpeed, elapsed - accelTime, 0);

		} else {
			// Acceleration was constant the whole time
			dist = RailsimCalc.calcTraveledDist(state.speed, elapsed, state.acceleration);
		}

		return dist;
	}

	/**
	 * Links that need to be blocked or otherwise stop needs to be initiated.
	 * This method is only valid for fixed block resources.
	 */
	public static List<RailLink> calcLinksToBlock(TrainPosition position, RailLink currentLink, double reserveDist) {

		List<RailLink> result = new ArrayList<>();

		// Assume current distance left on link is already reserved (only for fixed block)
		double dist = currentLink.length - position.getHeadPosition();

		int idx = position.getRouteIndex();

		// This function always needs to provide more reserve distance than requested (except when it will stop)
		while (FuzzyUtils.lessEqualThan(dist, reserveDist) && idx < position.getRouteSize()) {
			RailLink nextLink = position.getRoute(idx++);
			dist += nextLink.length;

			result.add(nextLink);

			// Don't block beyond stop
			if (position.isStop(nextLink.getLinkId()))
				break;
		}

		return result;
	}

	/**
	 * Calculate distance to the next stop.
	 */
	public static double calcDistToNextStop(TrainPosition position, RailLink currentLink) {
		double dist = currentLink.length - position.getHeadPosition();

		int idx = position.getRouteIndex();
		while (idx < position.getRouteSize()) {
			RailLink nextLink = position.getRoute(idx++);
			dist += nextLink.length;

			if (position.isStop(nextLink.getLinkId()))
				break;
		}

		return dist;
	}

	/**
	 * Maximum speed of the next upcoming links.
	 */
	static double calcPossibleMaxSpeed(TrainState state) {

		double safety = RailsimCalc.calcTraveledDist(state.train.maxVelocity(), state.train.maxVelocity() / state.train.deceleration(), -state.train.deceleration());
		double maxSpeed = state.allowedMaxSpeed;

		double dist = 0;
		for (int i = 0; i < state.route.size() && dist < safety; i++) {
			RailLink link = state.route.get(i);
			maxSpeed = Math.max(maxSpeed, link.getAllowedFreespeed(state.driver));

			dist += link.length;
		}

		return maxSpeed;
	}

	record SpeedTarget(double targetSpeed, double decelDist) implements Comparable<SpeedTarget> {

		@Override
		public int compareTo(SpeedTarget o) {
			return Double.compare(decelDist, o.decelDist);
		}
	}

}
