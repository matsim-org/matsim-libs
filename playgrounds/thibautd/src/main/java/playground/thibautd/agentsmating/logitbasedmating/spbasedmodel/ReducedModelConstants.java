/* *********************************************************************** *
 * project: org.matsim.*
 * ReducedModelConstants.java
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
package playground.thibautd.agentsmating.logitbasedmating.spbasedmodel;

/**
 * Defines some useful constants for the reduced model
 * @author thibautd
 */
public final class ReducedModelConstants {
	private ReducedModelConstants() {}

	public static final String GA_ABO = "generalAbonnement";
	public static final String HT_ABO = "halbtax";

	// model attributes names: alternative
	public static final String A_TRAVEL_TIME = "travelTime";
	public static final String A_COST = "cost";
	public static final String A_WALKING_TIME = "walkingTime";
	public static final String A_PARK_COST = "parkingCost";
	public static final String A_WAITING_TIME = "waitingTime";
	public static final String A_N_TRANSFERS = "nTransfers";

	// model attributes names: decider
	public static final String A_AGE = "age";
	public static final String A_IS_MALE = "isMale";
	public static final String A_SPEAKS_GERMAN = "speaksGerman";
	public static final String A_HAS_PT_ABO = "hasPtAbo";
	public static final String A_IS_CAR_ALWAYS_AVAIL = "carAvailability";
}

