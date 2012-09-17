/* *********************************************************************** *
 * project: org.matsim.*
 * EmptyStageActivityTypes.java
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
 * A {@link StageActivityTypes} that identifies no activity as a "stage activity".
 * To use for modes for which no activities are generated.
 * @author thibautd
 */
public final class EmptyStageActivityTypes implements StageActivityTypes {
	/**
	 * The only instance.
	 */
	public static final EmptyStageActivityTypes INSTANCE = new EmptyStageActivityTypes();

	private EmptyStageActivityTypes() {}
	@Override
	public boolean isStageActivity(final String activityType) {
		return false;
	}
}

