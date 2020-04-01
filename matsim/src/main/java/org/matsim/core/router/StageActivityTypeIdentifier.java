/* *********************************************************************** *
 * project: org.matsim.*
 * StageActivityCheckerImpl.java
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
package org.matsim.core.router;

/**
 * It is used by the {@link TripRouter} to detect trips, and can be used by
 * replanning modules as a "black list" of activities not to touch.
 *
 * @author gleich
 */
public final class StageActivityTypeIdentifier {

	public static final boolean isStageActivity(final String activityType) {
		return activityType.endsWith("interaction");
	}

}

