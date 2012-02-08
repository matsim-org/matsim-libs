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
package playground.thibautd.router;

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

	public void addActivityTypes(final StageActivityTypes checker) {
		checkers.add( checker );
	}

	public boolean removeActivityTypes(final StageActivityTypes checker) {
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

