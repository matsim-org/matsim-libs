/* *********************************************************************** *
 * project: org.matsim.*
 * CompositeStageActivityChecker.java
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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Implementation of a {@link StageActivityTypes} meant to aggregate the
 * activity checkers for all modes.
 *
 * @author thibautd
 */
public class CompositeStageActivityTypes implements StageActivityTypes {
	// set or list?
	private final Collection<StageActivityTypes> checkers = new ArrayList<StageActivityTypes>();

	// not necessary (empty varargs valid), but better for javadocs
	/**
	 * Creates an empty instance
	 */
	public CompositeStageActivityTypes() {}
	
	/**
	 * Creates an instance filled with the components passed as parameter
	 */
	public CompositeStageActivityTypes(
			final StageActivityTypes... components) {
		for ( StageActivityTypes c : components ) {
			addActivityTypes( c );
		}
	}

	/**
	 * Adds a {@link StageActivityTypes} to the delegates
	 * @param checker an instance which will be used to check whether an activity
	 * type corresponds to a stage activity
	 */
	public void addActivityTypes(final StageActivityTypes checker) {
		if (checker instanceof EmptyStageActivityTypes) {
			// avoid having to loop through lots of those,
			// which would be useless.
			return;
		}
		checkers.add( checker );
	}

	/**
	 * Removes the first registered {@link StageActivityTypes} instance
	 * considered equal to the parameter instance
	 * @param checker the instance to use to identify the instance to remove
	 * @return true if an instance was actually removed, false otherwise.
	 * Note that this will always return true for {@link EmptyStageActivityTypes}
	 * instances.
	 */
	public boolean removeActivityTypes(final StageActivityTypes checker) {
		if (checker instanceof EmptyStageActivityTypes) {
			// those checkers are not added: consider them removed.
			return true;
		}
		return checkers.remove( checker );
	}

	/**
	 * @return true if at least one of the registered checkers considers this activity
	 * type as a stage type.
	 */
	@Override
	public boolean isStageActivity(final String activityType) {
		for (StageActivityTypes checker : checkers) {
			if ( checker.isStageActivity( activityType ) ) {
				return true;
			}
		}

		return false;
	}
}

