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
package playground.ivt.scripts;

import org.matsim.core.utils.misc.Counter;
import playground.ivt.utils.MonitoringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author thibautd
 */
public class RunMemoryMonitoringExample {
	public static void main( String... args ) throws Exception {
		// this sets listenners of the GC, and prints some information at the end of the try block
		try ( AutoCloseable monitor = MonitoringUtils.monitorAndLogOnClose() ) {
			runIntensive();
		}

		// a new block restarts the measurements.
		try ( AutoCloseable monitor = MonitoringUtils.monitorAndLogOnClose() ) {
			runEasyPeasy();
		}
	}

	private static void runIntensive() {
		final List<Object> objects = new ArrayList<>();
		final Counter counter = new Counter( "Do useless stuff # " );
		for ( int i=0; i < 1e3; i++ ) {
			counter.incCounter();
			final List<Object> temp = new ArrayList<>(  );
			// create lots of objects
			for ( int j=0; j < 1e5; j++ ) {
				temp.add( new Object() );
			}
			objects.add( temp.subList( 0 , 100 ) );
		}
		counter.printCounter();
		objects.clear();
	}

	private static void runEasyPeasy() {
		final List<Object> objects = new ArrayList<>();
		final Counter counter = new Counter( "Do less useless stuff # " );
		for ( int i=0; i < 1e3; i++ ) {
			counter.incCounter();
			// create just enough objects
			for ( int j=0; j < 100; j++ ) {
				objects.add( new Object() );
			}
		}
		counter.printCounter();
		objects.clear();
	}
}
