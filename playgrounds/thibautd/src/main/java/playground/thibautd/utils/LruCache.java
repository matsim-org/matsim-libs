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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * @author thibautd
 */
public class LruCache<K,V> {
	private static final Logger log =
		Logger.getLogger(LruCache.class);

	private static final long SIZE_LOG_PERIOD = 1000;
	private long addCount = 0;

	private final Cloner<V> cloner;

	/**
	 * The cache works this way:
	 * - it stores at least cacheSize elements in an LRU cache 
	 * - mappings are also remembered in a map of softreferences.
	 *
	 * The idea is the following: the "real" cache is the softrefs map,
	 * which keeps objects as long as their key is in the lru; the lru
	 * is here to prevent the most recently used elements to be garbage collected.
	 *
	 * So this is a LRU which keeps elements as long as the garbage collector is
	 * happy with that --- the meaning of "the garbage collector being happy with
	 * that" being JVM dependent...
	 */
	private final Map<K, V> lru;
	private final Map<K, SoftEntry> softRefsMap = new HashMap<K, SoftEntry>();
	private final ReferenceQueue<V> queue = new ReferenceQueue<V>();

	public LruCache() {
		this( 100 );
	}

	public LruCache(
			final int cacheSize) {
		this( new Cloner<V>() {
				@Override
				public V clone(V cloned) {
					return cloned;
				}
			},
			cacheSize );
	}

	public LruCache(
			final Cloner<V> cloner,
			final int cacheSize) {
		this.lru =
			new LinkedHashMap<K, V>( (int) (1.6 * cacheSize) , 0.75f , true ) {
				private static final long serialVersionUID = 1L;
				@Override
				protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
					return size() >= cacheSize;
				}
			};
		this.cloner = cloner;
	}

	private void processQueue() {
		int c = 0;
		for ( SoftEntry e = (SoftEntry) queue.poll();
				e != null;
				e = (SoftEntry) queue.poll() ) {
			c++;
			assert !lru.containsKey( e.key );
			softRefsMap.remove( e.key );
		}

		if ( c > 0 && log.isTraceEnabled() ) {
			log.trace( this+": processed "+c+" GC'd references" );
		}
	}

	public V get( final K key ) {
		processQueue();

		// first get element from lru, to generate an access
		final V t = lru.get( key );
		if ( t != null ) return cloner.clone( t );

		// was not in the LRU: check if it is in the soft references
		final SoftEntry sr = softRefsMap.get( key );
		if ( sr == null ) return null;

		final V value = sr.get();

		if ( value == null ) {
			// it seems the GC was triggered while we were having fun here...
			processQueue();
		}

		return cloner.clone( value );
	}

	public void put( final K key , final V value ) {
		processQueue();

		if ( addCount++ % SIZE_LOG_PERIOD == 0 && log.isTraceEnabled() ) {
			log.trace( "size of cache (with "+lru.size()+" in lru): "+softRefsMap.size() );
		}

		final V clone = cloner.clone( value );
		lru.put( key , clone );
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

