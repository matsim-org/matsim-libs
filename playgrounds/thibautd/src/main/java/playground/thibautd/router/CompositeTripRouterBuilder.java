/* *********************************************************************** *
 * project: org.matsim.*
 * CompositeTripRouterBuilder.java
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
import java.util.List;

/**
 * A composite {@link TripRouterBuilder}.
 * @author thibautd
 */
public class CompositeTripRouterBuilder implements TripRouterBuilder {
	private final List<TripRouterBuilder> builders = new ArrayList<TripRouterBuilder>();


	/**
	 * Adds a builder to the list.
	 * @param builder the builder to add.
	 */
	public void addBuilder(final TripRouterBuilder builder) {
		builders.add( builder );
	}

	/**
	 * Executes all registered builders, in the order they were added.
	 */
	@Override
	public void setModeHandlers(
			final TripRouterFactory factory,
			final TripRouter tripRouter) {
		for (TripRouterBuilder builder : builders) {
			builder.setModeHandlers( factory , tripRouter );
		}
	}
}

