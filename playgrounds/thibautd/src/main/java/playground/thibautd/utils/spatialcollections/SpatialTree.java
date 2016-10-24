/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.utils.spatialcollections;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * @author thibautd
 */
public interface SpatialTree<C, T> {
	int size();

	T getAny();

	Collection<T> getAll();

	void add( Collection<T> toAdd );

	boolean remove( T value );

	boolean contains( T value );

	T getClosest( C coord );

	T getClosest(
			C coord,
			Predicate<T> predicate );
}
