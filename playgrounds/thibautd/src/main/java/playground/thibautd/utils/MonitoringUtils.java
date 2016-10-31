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
package playground.thibautd.utils;

import com.sun.management.GarbageCollectionNotificationInfo;
import org.apache.log4j.Logger;

import javax.management.NotificationBroadcaster;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import static com.sun.management.GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION;
import static org.osgeo.proj4j.parser.Proj4Keyword.b;

/**
 * @author thibautd
 */
public class MonitoringUtils {
	private static final Logger log = Logger.getLogger( MonitoringUtils.class );

	public static void setMemoryLoggingOnGC() {
		// based on http://www.fasterj.com/articles/gcnotifs.shtml
		//get all the GarbageCollectorMXBeans - there's one for each heap generation
		//so probably two - the old generation and young generation
		List<GarbageCollectorMXBean> gcbeans = ManagementFactory.getGarbageCollectorMXBeans();

		//Install a notification handler for each bean
		for ( GarbageCollectorMXBean gcbean : gcbeans ) {
			NotificationBroadcaster emitter = (NotificationBroadcaster) gcbean;

			//Add the listener
			emitter.addNotificationListener( ( notification, handback ) -> {
				//we only handle GARBAGE_COLLECTION_NOTIFICATION notifications here
				if ( !notification.getType().equals( GARBAGE_COLLECTION_NOTIFICATION ) ) {
					return;
				}
				if ( !log.isTraceEnabled() ) return;

				//get the information associated with this notification
				GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from( (CompositeData) notification.getUserData() );

				//get all the info and pretty print it
				long duration = info.getGcInfo().getDuration();
				String gctype = info.getGcAction();
				if ( "end of minor GC".equals( gctype ) ) {
					gctype = "Young Gen GC";
				}
				else if ( "end of major GC".equals( gctype ) ) {
					gctype = "Old Gen GC";
				}
				log.debug( gctype + ": - " + info.getGcInfo().getId() + " " + info.getGcName() + " (from " + info.getGcCause() + ") " + duration + " milliseconds; start-end times " + info.getGcInfo().getStartTime() + "-" + info.getGcInfo().getEndTime() );
				log.debug("GcInfo MemoryUsageAfterGc: " + info.getGcInfo().getMemoryUsageAfterGc());
			}, null, null );
		}
	}

	public AutoCloseable notifyPeakUsageOnClose( final LongConsumer callback ) {
		List<GarbageCollectorMXBean> gcbeans = ManagementFactory.getGarbageCollectorMXBeans();

		final AtomicLong peak = new AtomicLong( -1 );

		final NotificationListener notificationListener = ( notification, handback ) -> {
			//we only handle GARBAGE_COLLECTION_NOTIFICATION notifications here
			if ( !notification.getType().equals( GARBAGE_COLLECTION_NOTIFICATION ) ) {
				return;
			}
			//get the information associated with this notification
			GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from( (CompositeData) notification.getUserData() );

			final long totalUsedBytes =
					info.getGcInfo().getMemoryUsageAfterGc().values().stream()
						.mapToLong( MemoryUsage::getUsed )
						.sum();

			peak.getAndUpdate( p -> totalUsedBytes > p ? totalUsedBytes : p );
		};

		//Install a notification handler for each bean
		for ( GarbageCollectorMXBean gcbean : gcbeans ) {
			NotificationBroadcaster emitter = (NotificationBroadcaster) gcbean;
			emitter.addNotificationListener( notificationListener, null, null );
		}

		return () -> {
			callback.accept( peak.get() );
			for ( GarbageCollectorMXBean gcbean : gcbeans ) {
				NotificationBroadcaster emitter = (NotificationBroadcaster) gcbean;
				emitter.removeNotificationListener( notificationListener );
			}
		};
	}
}
