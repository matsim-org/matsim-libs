/* *********************************************************************** *
 * project: org.matsim.*
 * FrequentationSpotWeighter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.hitchiking.spotweights;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.thibautd.hitchiking.HitchHikingConstants;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link SpotWeighter} which weights depend on the number of passenger/drivers
 * having interacted with this spot (ie it is destination or success blind).
 *
 * It uses learning with linear forgetting to avoid to much fluctuation.
 * </b>
 * CAUTION: the weights are NOT valid during the mobsim step.
 * </b>
 * To work properly, the instance has to be registered as a StartupListener before
 * starting the run. It will than insert itself as an AgentDepartureEventHandler
 * once the EventsManager is initialized. 
 * @author thibautd
 */
public class FrequentationSpotWeighter implements SpotWeighter, PersonDepartureEventHandler, StartupListener {
	private static final Logger log =
		Logger.getLogger(FrequentationSpotWeighter.class);

	public static final String CONFIG_GROUP_NAME = "frequentationSpotWeighter";
	public static final String CONFIG_PARAM_RATE = "learningRate";
	public static final String CONFIG_PARAM_BASE_WEIGHT = "baseWeight";
	public static final String CONFIG_PARAM_WRITE_INTERVAL = "writeInterval";
	private final double binSize = 30 * 60;
	private final double learningRate;
	private final double baseWeight;
	private final int writeInterval;

	private StatsWriter statsWriter = null;

	private final Map<Id, WeightsPerTimeBin> weightsForDrivers = new ConcurrentHashMap<Id, WeightsPerTimeBin>();
	private final Map<Id, WeightsPerTimeBin> weightsForPassengers = new ConcurrentHashMap<Id, WeightsPerTimeBin>();

	private int currentIter = -1;

	public FrequentationSpotWeighter(final Config config) {
		this( getLearningRate( config ),
				getBaseWeight( config ),
				getWriteInterval( config ));
	}

	private static int getWriteInterval(final Config config) {
		String v = config == null ? null :
			config.findParam( CONFIG_GROUP_NAME , CONFIG_PARAM_WRITE_INTERVAL );
		return v == null ? 10 : Integer.parseInt( v );
	}

	private static double getBaseWeight(final Config config) {
		String v = config == null ? null :
			config.findParam( CONFIG_GROUP_NAME , CONFIG_PARAM_BASE_WEIGHT );
		return v == null ? 50 : Double.parseDouble( v );
	}

	private static double getLearningRate(Config config) {
		String v = config == null ? null :
			config.findParam( CONFIG_GROUP_NAME , CONFIG_PARAM_RATE );
		return v == null ? 0.5 : Double.parseDouble( v );
	}

	public FrequentationSpotWeighter() {
		this( null );
	}

	public FrequentationSpotWeighter(
			final double learningRate,
			final double baseWeight,
			final int writeInterval) {
		if (learningRate < 0 || learningRate > 1) {
			throw new IllegalArgumentException( "invalid learning rate "+learningRate );
		}
		this.learningRate = learningRate;
		this.baseWeight = baseWeight;
		this.writeInterval = writeInterval;
	}

	// ///////////////////////////////////////////////////////////////////////////
	// weight interface
	// ///////////////////////////////////////////////////////////////////////////
	@Override
	public double weightDriverOrigin(
			final double departureTime,
			final Id originLink,
			final Id destinationLink) {
		return baseWeight + getValue( originLink , departureTime , weightsForDrivers ).value;
	}

	@Override
	public double weightPassengerOrigin(
			final double departureTime,
			final Id originLink,
			final Id destinationLink) {
		return baseWeight + getValue( originLink , departureTime , weightsForPassengers ).value;
	}

	// ///////////////////////////////////////////////////////////////////////////
	// event handler interface
	// ///////////////////////////////////////////////////////////////////////////
	@Override
	public void reset(final int iteration) {
		log.debug( "reset called" );
		if (iteration > currentIter) {
			log.debug( "update iteration number from "+currentIter+" to "+iteration );
			currentIter = iteration;

			if (writeInterval > 0 && iteration % writeInterval == 0) {
				log.debug( "call statistics print" );
				statsWriter.writeStats( iteration );
			}

			log.debug( "update weights for drivers" );
			for (WeightsPerTimeBin ws  : weightsForDrivers.values()) {
				for (MyDouble v : ws.weights.values()) v.value *= learningRate;
			}
			log.debug( "update weights for passengers" );
			for (WeightsPerTimeBin ws : weightsForPassengers.values()) {
				for (MyDouble v : ws.weights.values()) v.value *= learningRate;
			}
		}
	}

	@Override
	public void handleEvent(final PersonDepartureEvent event) {
		final String mode = event.getLegMode();

		// XXX: what is the time of the departure event for passengers?
		if (mode.equals( HitchHikingConstants.DRIVER_MODE )) {
			getValue( event.getLinkId() , event.getTime() , weightsForPassengers ).value++;
		}
		else if (mode.equals( HitchHikingConstants.PASSENGER_MODE )) {
			getValue( event.getLinkId() , event.getTime() , weightsForDrivers ).value++;
		}
	}


	@Override
	public void notifyStartup(final StartupEvent event) {
		event.getControler().getEvents().addHandler( this );
		statsWriter = new StatsWriter( event.getControler() );
	}
	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private static class MyDouble {
		public double value = 0;
	}

	private class WeightsPerTimeBin {
		private final Map<Integer, MyDouble> weights = new ConcurrentHashMap<Integer, MyDouble>();

		public synchronized MyDouble getValue(final double time) {
			int bin = (int) (time / binSize);
			MyDouble v = weights.get( bin );

			if (v == null) {
				v = new MyDouble();
				weights.put( bin , v );
			}

			return v;
		}
	}

	private MyDouble getValue(
			final Id key,
			final double time,
			final Map<Id, WeightsPerTimeBin> map) {
		synchronized (map) {
			if (log.isTraceEnabled()) log.trace( "getting value at "+key );
			WeightsPerTimeBin val = map.get( key );

			if (val == null) {
				if (log.isTraceEnabled()) log.trace( "creating value at "+key );
				val = new WeightsPerTimeBin();
				map.put( key , val );
			}

			return val.getValue( time );
		}
	}

	private class StatsWriter {
		private final OutputDirectoryHierarchy io;

		public StatsWriter(final Controler c)  {
			io = c.getControlerIO();
		}

		public void writeStats(final int iter) {
			try {
				write( io.getIterationFilename( iter , "weightsForDrivers.dat.gz" ) , weightsForDrivers);
				write( io.getIterationFilename( iter , "weightsForPassengers.dat.gz" ) , weightsForPassengers);
			}
			catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}

		private void write(
				final String fileName,
				final Map<Id, WeightsPerTimeBin> weights) throws IOException {
			BufferedWriter writer = IOUtils.getBufferedWriter( fileName ); 

			writer.write( "linkId\tbinStart\tbinEnd\tweight" );
			for (Map.Entry<Id, WeightsPerTimeBin> entry : weights.entrySet()) {
				Id id = entry.getKey();
				for ( Map.Entry<Integer, MyDouble> w : entry.getValue().weights.entrySet() ) {
					writer.newLine();
					int bin = w.getKey();
					writer.write( id +"\t" +(bin * binSize)+"\t"+((bin + 1) * binSize)+"\t"+(w.getValue().value + baseWeight) );
				}
			}
			writer.close();
		}
	}
}

