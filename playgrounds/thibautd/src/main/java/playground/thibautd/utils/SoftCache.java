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

	private final QueueCleaner<K, V> cleaner = new QueueCleaner<K, V>();

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
		final Thread t = new Thread( cleaner , this+"_queueCleaner" );
		t.setDaemon( true );
		t.start();
	}

	public V get( final K key ) {
		final SoftEntry<K, V> sr = cleaner.softRefsMap.get( key );
		if ( sr == null ) return null;

		final V value = sr.get();

		if ( value == null ) {
			// it seems the GC was triggered while we were having fun here...
			return null;
		}

		return cloner.clone( value );
	}

	public void put( final K key , final V value ) {
		final V clone = cloner.clone( value );
		// one could use putIfAbsent to add a "lazilly computing" object,
		// to avoid computing twice the same value
		cleaner.softRefsMap.put( key , new SoftEntry<K, V>( cleaner.queue, key , clone ) );
	}

	@Override
	public void finalize() {
		cleaner.run = false;
	}

	private static class SoftEntry<K, V> extends SoftReference<V> {
		private final K key;

		public SoftEntry(
				final ReferenceQueue<V> queue,
				final K key,
				final V value) {
			super( value, queue );
			this.key = key;
		}
	}


	public static interface Cloner<T> {
		public T clone( T cloned );
	}

	private static class QueueCleaner<K, V> implements Runnable {
		private final ReferenceQueue<V> queue = new ReferenceQueue<V>();
		private final ConcurrentMap<K, SoftEntry<K, V>> softRefsMap = new ConcurrentHashMap<K, SoftEntry<K, V>>();
		private boolean run = true;

		@Override
		public void run() {
			int last = 0;
			int c = 0;
			while (run) {
				try {
					final SoftEntry<K, V> e = (SoftEntry<K, V>) queue.remove( 5000L );
					if ( e != null ) {
						c++;
						last++;
						softRefsMap.remove( e.key );
					}
					else if ( last > 0 && log.isTraceEnabled() ) {
						log.trace( this+": processed "+last+" GC'd references" );
						last = 0;
					}
				}
				catch (InterruptedException e) {
					log.error( e );
				}
			}

			if ( c > 0 && log.isTraceEnabled() ) {
				log.trace( this+": processed "+c+" GC'd references in total" );
			}
		}
	}
}

