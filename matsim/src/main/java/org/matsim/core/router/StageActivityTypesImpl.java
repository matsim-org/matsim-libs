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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Default implementation of a {@link StageActivityTypes}, based on a list of types.
 *
 * @author thibautd
 */
public class StageActivityTypesImpl implements StageActivityTypes {
	// use a sorted set, so that two checkers returning the same result
	// have equal internal collections.
	private final SortedSet<String> types = new TreeSet<String>();

	/**
	 * Initializes an instance with a single type
	 */
	public StageActivityTypesImpl(final String type) {
		this( Collections.singleton( type ) );
	}

	/**
	 * Initialises an instance with a given list of types
	 * @param types a Collection containing the types to consider as stage types.
	 */
	public StageActivityTypesImpl(final Collection<String> types) {
		this.types.addAll( types );
	}

	public StageActivityTypesImpl( final String... types ) {
		this( Arrays.asList( types ) );
	}

	@Override
	public boolean isStageActivity(final String activityType) {
		if ( !activityType.isEmpty() && activityType.endsWith("interaction") ) {
			return true;
		} else {
			return types.contains( activityType );
		}
	}


	@Override
	public boolean equals(final Object other) {
		if (other != null && other.getClass().equals( this.getClass() )) {
			return types.equals( ((StageActivityTypesImpl) other).types );
		}
		return false;
	}

	@Override
	public int hashCode() {
		return types.hashCode();
	}
}

