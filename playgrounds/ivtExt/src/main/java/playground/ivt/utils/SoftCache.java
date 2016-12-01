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
package playground.ivt.utils;

import org.apache.log4j.Logger;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * A cache that keeps entries as long as the Virtual Machine is happy
 * with it, using SoftReferences.
 * It is designed to be usable from multiple threads with minimal locking
 * using a ConcurrentHashMap internally.
 * At construction, a daemon thread is started to handle cleaning of the GC'd
 * entries, which gets stopped at most 5 seconds after the cache instance is finalized.
 * <br>
 * Note that the GC often only partially cleans the cache.
 * This blog post gives an idea of which references are kept:
 * http://jeremymanson.blogspot.ch/2009/07/how-hotspot-decides-to-clear_07.html
 * (in short, the JVM is actually uses some kind of LRU heuristic,
 * defined as a minimum time to keep a reference after it is last accessed.
 * This time depends on the free heap space: the bigger the space, the longer the
 * time)
 * <br>
 * To log some statistics about cleared references, set the log level to TRACE
 * for this class.
 *
 * @author thibautd
 */
public class SoftCache<K,V> {
	private static final Logger log =
		Logger.getLogger(SoftCache.class);

	private final Cloner<V> cloner;

	private final QueueCleaner<K, V> cleaner = new QueueCleaner<K, V>();

	private long requests = 0;
	private long hits = 0;
	private long nextTrace = 1;

	/**
	 * Initializes an instance which does not clone objects when they are added
	 * or returned. This should be used only when <tt>V</tt> objects are immutable
	 * or no reference to them is kept.
	 */
	public SoftCache() {
		this( cloned -> cloned );
	}

	/**
	 * Initializes an instance which clones objects when they are added or returned.
	 * @param cloner an object which clones cached objects before adding them to the cache
	 * and before returning them, to ensure the abscence of side effects if needed.
	 */
	public SoftCache(
			final Cloner<V> cloner ) {
		this.cloner = cloner;
		final Thread t = new Thread( cleaner , this+"_queueCleaner" );
		t.setDaemon( true );
		t.start();
	}

	public V get( final K key ) {
		requests++;
		final SoftEntry<K, V> sr = cleaner.softRefsMap.get( key );
		if ( sr == null ) return null;

		final V value = sr.get();

		if ( value == null ) {
			// it seems the GC was triggered while we were having fun here...
			return null;
		}

		hits++;

		if ( log.isTraceEnabled() && hits == nextTrace ) {
			nextTrace *= 2;
			log.trace( "hits / requests: "+hits+" / "+requests+" ("+( (100d * hits) / requests)+"%)");
		}
		return cloner.clone( value );
	}

	public void put( final K key , final V value ) {
		final V clone = cloner.clone( value );
		cleaner.softRefsMap.put( key , new SoftEntry<>( cleaner.queue, key, clone ) );
	}

	public V getOrPut( final K key , final V value ) {
		return getOrPut( key , () -> value );
	}

	public V getOrPut( final K key , final Supplier<V> supplier ) {
		V obj = get( key );

		if ( obj == null ) {
			obj = supplier.get();
			put( key , obj );
		}

		return obj;
	}

	@Override
	public void finalize() throws Throwable {
		cleaner.run = false;
		super.finalize();
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


	public interface Cloner<T> {
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
						log.trace( this+": processed "+last+" GC'd references. "+softRefsMap.size()+" entries remain in cache." );
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

