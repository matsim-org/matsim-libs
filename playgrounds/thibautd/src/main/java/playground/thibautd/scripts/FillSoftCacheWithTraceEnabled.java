/* *********************************************************************** *
 * project: org.matsim.*
 * FillSoftCacheWithTraceEnabled.java
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
package playground.thibautd.scripts;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.Counter;
import playground.thibautd.utils.SoftCache;

import java.util.Random;

/**
 * For testing. It seems the JVM is smart enough to clean only part of
 * the soft references when it needs space.
 * This blog post gives an idea of which are kept:
 * http://jeremymanson.blogspot.ch/2009/07/how-hotspot-decides-to-clear_07.html
 * (which indicates that is actually uses some kind of LRU heuristic,
 * defined as a minimum time to keep a reference after it is last accessed.
 * This time depend of the free heap space: the bigger the space, the longer the
 * time)
 * @author thibautd
 */
public class FillSoftCacheWithTraceEnabled {
	public static void main(final String[] args) {
		Logger.getLogger( SoftCache.class ).setLevel( Level.TRACE );
		final SoftCache<Object, byte[]> cache = new SoftCache<Object, byte[]>();
		final Random random = new Random();
		final Counter counter = new Counter( "add object # " );
		while ( true ) {
			counter.incCounter();
			final byte[] arr = new byte[ 10000 ];
			random.nextBytes( arr );
			cache.put( new Object() , arr );
		}
	}
}

