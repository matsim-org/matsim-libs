/* *********************************************************************** *
 * project: org.matsim.*
 * LruCache.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

/**
 * @author thibautd
 */
public class SoftCache<K,V> {
	private static final Logger log =
		Logger.getLogger(SoftCache.class);

	private final Cloner<V> cloner;

	private final ConcurrentMap<K, SoftEntry> softRefsMap = new ConcurrentHashMap<K, SoftEntry>();
	private final ReferenceQueue<V> queue = new ReferenceQueue<V>();

	public SoftCache() {
		this( new Cloner<V>() {
				@Override
				public V clone(V cloned) {
					return cloned;
				}
			} );
	}

	public SoftCache(
			final Cloner<V> cloner ) {
		this.cloner = cloner;
	}

	private void processQueue() {
		int c = 0;
		for ( SoftEntry e = (SoftEntry) queue.poll();
				e != null;
				e = (SoftEntry) queue.poll() ) {
			c++;
			softRefsMap.remove( e.key );
		}

		if ( c > 0 && log.isTraceEnabled() ) {
			log.trace( this+": processed "+c+" GC'd references" );
		}
	}

	public V get( final K key ) {
		processQueue();

		final SoftEntry sr = softRefsMap.get( key );
		if ( sr == null ) return null;

		final V value = sr.get();

		if ( value == null ) {
			// it seems the GC was triggered while we were having fun here...
			processQueue();
			return null;
		}

		return cloner.clone( value );
	}

	public void put( final K key , final V value ) {
		processQueue();

		final V clone = cloner.clone( value );
		// one could use putIfAbsent to add a "lazilly computing" object,
		// to avoid computing twice the same value
		softRefsMap.put( key , new SoftEntry( key , clone ) );
		processQueue();
	}

	private class SoftEntry extends SoftReference<V> {
		private final K key;

		public SoftEntry(
				final K key,
				final V value) {
			super( value, queue );
			this.key = key;
		}
	}


	public static interface Cloner<T> {
		public T clone( T cloned );
	}
}

