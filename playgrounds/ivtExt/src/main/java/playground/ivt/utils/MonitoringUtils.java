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
package playground.ivt.utils;

import com.sun.management.GarbageCollectionNotificationInfo;
import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import javax.management.NotificationBroadcaster;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleConsumer;
import java.util.function.LongConsumer;

import static com.sun.management.GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION;

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

	public static AutoCloseable writeGCFigure( final String path ) throws IOException {
		final BufferedWriter writer = IOUtils.getBufferedWriter( path );

		writer.write( "time\twhen\tcause\ttotalOverhead\tduration\tcommited\tused\tmax" );

		final long startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
		final AtomicLong totalGcTime = new AtomicLong( 0 );

		final NotificationListener notificationListener = ( notification, handback ) -> {
			//we only handle GARBAGE_COLLECTION_NOTIFICATION notifications here
			if ( !notification.getType().equals( GARBAGE_COLLECTION_NOTIFICATION ) ) {
				return;
			}
			//get the information associated with this notification
			GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from( (CompositeData) notification.getUserData() );

			final long totalAllocatedBeforeBytes =
					info.getGcInfo().getMemoryUsageBeforeGc().values().stream()
						.mapToLong( MemoryUsage::getCommitted )
						.sum();

			final long totalAllocatedAfterBytes =
					info.getGcInfo().getMemoryUsageAfterGc().values().stream()
						.mapToLong( MemoryUsage::getCommitted )
						.sum();

			final long totalUsedBeforeBytes =
					info.getGcInfo().getMemoryUsageBeforeGc().values().stream()
						.mapToLong( MemoryUsage::getUsed )
						.sum();

			final long totalUsedAfterBytes =
					info.getGcInfo().getMemoryUsageAfterGc().values().stream()
						.mapToLong( MemoryUsage::getUsed )
						.sum();

			final long totalMaxBeforeBytes =
					info.getGcInfo().getMemoryUsageBeforeGc().values().stream()
						.mapToLong( MemoryUsage::getMax )
						.sum();

			final long totalMaxAfterBytes =
					info.getGcInfo().getMemoryUsageAfterGc().values().stream()
							.mapToLong( MemoryUsage::getMax )
							.sum();

			totalGcTime.addAndGet( info.getGcInfo().getDuration() );

			final double totalOverhead = ((double) totalGcTime.get()) /
					(info.getGcInfo().getEndTime() - startTime);

			try {
				writer.newLine();
				writer.write( info.getGcInfo().getStartTime()+"\tbefore\t"+
						info.getGcCause()+"\t"+
						totalOverhead+"\t"+
						info.getGcInfo().getDuration()+"\t"+
						totalAllocatedBeforeBytes+"\t" +
						totalUsedBeforeBytes+"\t"+
						totalMaxBeforeBytes );
				writer.newLine();
				writer.write( info.getGcInfo().getEndTime()+"\tafter\t"+
						info.getGcCause()+"\t"+
						totalOverhead+"\t"+
						info.getGcInfo().getDuration()+"\t"+
						totalAllocatedAfterBytes+"\t" +
						totalUsedAfterBytes+"\t"+
						totalMaxAfterBytes );
				writer.flush();
			}
			catch ( IOException e ) {
				throw new UncheckedIOException( e );
			}
		};

		//Install a notification handler for each bean
		final List<GarbageCollectorMXBean> gcbeans = ManagementFactory.getGarbageCollectorMXBeans();
		for ( GarbageCollectorMXBean gcbean : gcbeans ) {
			NotificationBroadcaster emitter = (NotificationBroadcaster) gcbean;
			emitter.addNotificationListener( notificationListener, null, null );
		}

		return () -> {
			for ( GarbageCollectorMXBean gcbean : gcbeans ) {
				NotificationBroadcaster emitter = (NotificationBroadcaster) gcbean;
				emitter.removeNotificationListener( notificationListener );
			}
			writer.close();
		};
	}

	public static AutoCloseable monitorAndLogOnClose() {
		Gbl.printBuildInfo();
		Gbl.printSystemInfo();

		return closeable(
				() -> log.info( "Summary Garbage Collector Statistics:" ),
				notifyPeakUsageOnClose( p -> log.info( "Peak used memory after GC: "+formatMemory( p ) ) ),
				notifyPeakCommitedOnClose( p -> log.info( "Peak commited memory after GC: "+formatMemory( p ) ) ),
				notifyGCOverheadOnClose( o -> log.info( "GC Time Overhead: "+o+"%" ) ) );
	}

	private static AutoCloseable closeable( AutoCloseable... cs ) {
		return () -> {
			for ( AutoCloseable c : cs ) c.close();
		};
	}

	private static String formatMemory( final long p ) {
		return ((p/1048576)+1)+"MiB ("+((p / 1073741824) + 1)+"GiB)";
	}

	public static void monitorAndLogAtEnd( final Call call ) {
		try ( AutoCloseable monitorAndLogOnClose = monitorAndLogOnClose() ) {
			call.call();
		}
		catch ( RuntimeException | Error e ) {
			throw e;
		}
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	public static AutoCloseable notifyPeakUsageOnClose( final LongConsumer callback ) {
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

	public static AutoCloseable notifyPeakCommitedOnClose( final LongConsumer callback ) {
		List<GarbageCollectorMXBean> gcbeans = ManagementFactory.getGarbageCollectorMXBeans();

		final AtomicLong peak = new AtomicLong( -1 );

		final NotificationListener notificationListener = ( notification, handback ) -> {
			//we only handle GARBAGE_COLLECTION_NOTIFICATION notifications here
			if ( !notification.getType().equals( GARBAGE_COLLECTION_NOTIFICATION ) ) {
				return;
			}
			//get the information associated with this notification
			GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from( (CompositeData) notification.getUserData() );

			final long totalAllocatedBytes =
					info.getGcInfo().getMemoryUsageAfterGc().values().stream()
						.mapToLong( MemoryUsage::getCommitted )
						.sum();

			peak.getAndUpdate( p -> totalAllocatedBytes > p ? totalAllocatedBytes : p );
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

	public static AutoCloseable notifyGCOverheadOnClose( final DoubleConsumer callback ) {
		List<GarbageCollectorMXBean> gcbeans = ManagementFactory.getGarbageCollectorMXBeans();

		final long startTime = System.currentTimeMillis();
		final AtomicLong totalGcTime = new AtomicLong( 0 );

		final NotificationListener notificationListener = ( notification, handback ) -> {
			//we only handle GARBAGE_COLLECTION_NOTIFICATION notifications here
			if ( !notification.getType().equals( GARBAGE_COLLECTION_NOTIFICATION ) ) {
				return;
			}
			//get the information associated with this notification
			GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from( (CompositeData) notification.getUserData() );

			totalGcTime.addAndGet( info.getGcInfo().getDuration() );
		};

		//Install a notification handler for each bean
		for ( GarbageCollectorMXBean gcbean : gcbeans ) {
			NotificationBroadcaster emitter = (NotificationBroadcaster) gcbean;
			emitter.addNotificationListener( notificationListener, null, null );
		}

		return () -> {
			final long endTime = System.currentTimeMillis();
			callback.accept( totalGcTime.get() * 100d / (endTime - startTime) );
			for ( GarbageCollectorMXBean gcbean : gcbeans ) {
				NotificationBroadcaster emitter = (NotificationBroadcaster) gcbean;
				emitter.removeNotificationListener( notificationListener );
			}
		};
	}

	public static void listenBytesUsageOnGC( final LongConsumer callback ) {
		List<GarbageCollectorMXBean> gcbeans = ManagementFactory.getGarbageCollectorMXBeans();

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

			callback.accept( totalUsedBytes );
		};

		//Install a notification handler for each bean
		for ( GarbageCollectorMXBean gcbean : gcbeans ) {
			NotificationBroadcaster emitter = (NotificationBroadcaster) gcbean;
			emitter.addNotificationListener( notificationListener, null, null );
		}
	}

	@FunctionalInterface
	public interface Call { void call(); }
}
