/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.thibautd.initialdemandgeneration.socnetgensimulated.framework;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

class ThreadGroup {
	private final static Logger log = Logger.getLogger( ThreadGroup.class );

	final List<Thread> threads = new ArrayList< >();
	final List<Throwable> exceptions = new ArrayList< >();
	final Thread.UncaughtExceptionHandler exceptionHandler =
			( t, e ) -> {
				log.error( "exception in thread "+t.getName() , e );
				exceptions.add( e );
			};

	public void add( final Runnable r ) {
		final Thread t = new Thread( r );
		t.setUncaughtExceptionHandler( exceptionHandler );
		threads.add( t );
	}

	public void run() {
		for ( Thread t : threads ) t.start();
		try {
			for ( Thread t : threads ) t.join();
		}
		catch ( InterruptedException e ) {
			throw new RuntimeException( e );
		}

		if ( !exceptions.isEmpty() ) throw new RuntimeException( "got "+exceptions.size()+" exceptions while running threads" );
	}
}
