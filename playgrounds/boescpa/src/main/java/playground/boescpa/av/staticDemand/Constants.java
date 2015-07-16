/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.boescpa.av.staticDemand;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class Constants {

	private static double BEELINE_FACTOR_STREET = 1.6;
	private static double AV_SPEED = 9.72; // [m/s] (= 35km/h)
	private static int LEVEL_OF_SERVICE = 300; // [s] (= 5min)
	private static int BOARDING_TIME = 60; // [s] (= 1min)
	private static int UNBOARDING_TIME = 60; // [s] (= 1min)

	public static double getBeelineFactorStreet() {
		return BEELINE_FACTOR_STREET;
	}

	private static void setBeelineFactorStreet(double beelineFactor) {
		BEELINE_FACTOR_STREET = beelineFactor;
	}

	public static double getAvSpeed() {
		return AV_SPEED;
	}

	private static void setAvSpeed(double avSpeed) {
		AV_SPEED = avSpeed;
	}

	public static int getLevelOfService() {
		return LEVEL_OF_SERVICE;
	}

	private static void setLevelOfService(int levelOfService) {
		LEVEL_OF_SERVICE = levelOfService;
	}

	public static int getBoardingTime() {
		return BOARDING_TIME;
	}

	public static void setBoardingTime(int boardingTime) {
		BOARDING_TIME = boardingTime;
	}

	public static int getUnboardingTime() {
		return UNBOARDING_TIME;
	}

	public static void setUnboardingTime(int unboardingTime) {
		UNBOARDING_TIME = unboardingTime;
	}

	/**
	 * Calculates the maximum search radius given a desired level of service.
	 *
	 * @return Maximum search radius [m].
	 */
	public static double getSearchRadius() {
		return ((AV_SPEED * LEVEL_OF_SERVICE) / BEELINE_FACTOR_STREET);
	}
}
