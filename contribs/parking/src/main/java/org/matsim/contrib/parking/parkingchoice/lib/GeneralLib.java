/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingchoice.lib;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

public class GeneralLib {

		/**
	 * If time is > 60*60*24 [seconds], it will be projected into next day, e.g.
	 * time=60*60*24+1=1
	 * <p>
	 * even if time is negative, it is turned into a positive time by adding
	 * number of seconds of day into it consecutively
	 *
	 * @param time
	 * @return
	 */
	public static double projectTimeWithin24Hours(double time) {
		double secondsInOneDay = 60 * 60 * 24;

		if (time == Double.NEGATIVE_INFINITY || time == Double.POSITIVE_INFINITY) {
			throw new Error("system is in inconsistent state: " +
			"time is not allowed to be minus or plus infinity");
		}

		while (time < 0) {
			time += secondsInOneDay;
		}

		if (time < secondsInOneDay) {
			return time;
		} else {
			return ((time / secondsInOneDay) - (Math.floor(time / secondsInOneDay))) * secondsInOneDay;
		}
	}

	/**
	 * 24 hour check is performed (no projection required as pre-requisite).
	 *
	 * @param startIntervalTime
	 * @param endIntervalTime
	 * @return
	 */
	public static double getIntervalDuration(double startIntervalTime, double endIntervalTime) {
		double secondsInOneDay = 60 * 60 * 24;
		startIntervalTime = projectTimeWithin24Hours(startIntervalTime);
		endIntervalTime = projectTimeWithin24Hours(endIntervalTime);

		if (startIntervalTime == endIntervalTime) {
			return 0;
		}

		if (startIntervalTime < endIntervalTime) {
			return endIntervalTime - startIntervalTime;
		} else {
			return endIntervalTime + (secondsInOneDay - startIntervalTime);
		}
	}

	/**
	 * Interval start and end are inclusive.
	 *
	 * @param startIntervalTime
	 * @param endIntervalTime
	 * @param timeToCheck
	 * @return
	 */
	public static boolean isIn24HourInterval(double startIntervalTime, double endIntervalTime, double timeToCheck) {
		errorIfNot24HourProjectedTime(startIntervalTime);
		errorIfNot24HourProjectedTime(endIntervalTime);
		errorIfNot24HourProjectedTime(timeToCheck);

		if (startIntervalTime < endIntervalTime && timeToCheck >= startIntervalTime && timeToCheck <= endIntervalTime) {
			return true;
		}

		return startIntervalTime > endIntervalTime && (timeToCheck >= startIntervalTime || timeToCheck <= endIntervalTime);
	}

	public static void errorIfNot24HourProjectedTime(double time) {
		double secondsInOneDay = 60 * 60 * 24;

		if (time >= secondsInOneDay) {
			throw new Error("time not projected within 24 hours!");
		}
	}

	public static double getDistance(Coord coord, Link link) {
		return GeneralLib.getDistance(coord, link.getCoord());
	}

	public static double getDistance(Coord coordA, Coord coordB) {
		double xDiff = coordA.getX() - coordB.getX();
		double yDiff = coordA.getY() - coordB.getY();
		return Math.sqrt(xDiff * xDiff + yDiff * yDiff);
	}

}
