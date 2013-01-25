/* *********************************************************************** *
 * project: org.matsim.*
 * PooledIdFactory.java
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
package playground.thibautd.utils;

import java.lang.ref.WeakReference;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * A simple object to avoid multiplying identical instances of immutable objects.
 * It uses a WeakHashMap internally, so that no unecessary instances are remembered.
 * <br>
 * Do not use with mutable objects!
 *
 * @author thibautd
 */
public class ObjectPool<T extends Object> {
	private final Map<T, WeakReference<T>> pool = new WeakHashMap<T, WeakReference<T>>();

	public T getPooledInstance( final T object ) {
		WeakReference<T> pooled = pool.get( object );

		if (pooled == null) {
			pooled = new WeakReference<T>( object );
			pool.put( object , pooled );
		}

		return pooled.get();
	}
}

