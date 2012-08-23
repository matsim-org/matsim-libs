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

import java.io.BufferedWriter;
import java.io.IOException;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.thibautd.hitchiking.HitchHikingConstants;

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
public class FrequentationSpotWeighter implements SpotWeighter, AgentDepartureEventHandler, StartupListener {
	public static final String CONFIG_GROUP_NAME = "frequentationSpotWeighter";
	public static final String CONFIG_PARAM_RATE = "learningRate";
	public static final String CONFIG_PARAM_BASE_WEIGHT = "baseWeight";
	public static final String CONFIG_PARAM_WRITE_INTERVAL = "writeInterval";
	private final double learningRate;
	private final double baseWeight;
	private final int writeInterval;

	private StatsWriter statsWriter = null;

	private final Map<Id, MyDouble> weightsForDrivers = new TreeMap<Id, MyDouble>();
	private final Map<Id, MyDouble> weightsForPassengers = new TreeMap<Id, MyDouble>();

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
		return baseWeight + getValue( originLink , weightsForDrivers ).value;
	}

	@Override
	public double weightPassengerOrigin(
			final double departureTime,
			final Id originLink,
			final Id destinationLink) {
		return baseWeight + getValue( originLink , weightsForPassengers ).value;
	}

	// ///////////////////////////////////////////////////////////////////////////
	// event handler interface
	// ///////////////////////////////////////////////////////////////////////////
	@Override
	public void reset(final int iteration) {
		if (iteration > currentIter) {
			currentIter = iteration;

			if (writeInterval > 0 && iteration % writeInterval == 0) {
				statsWriter.writeStats( iteration );
			}

			for (MyDouble v : weightsForDrivers.values()) {
				v.value *= learningRate;
			}
			for (MyDouble v : weightsForPassengers.values()) {
				v.value *= learningRate;
			}
		}
	}

	@Override
	public void handleEvent(final AgentDepartureEvent event) {
		final String mode = event.getLegMode();
		if (mode.equals( HitchHikingConstants.DRIVER_MODE )) {
			getValue( event.getLinkId() , weightsForPassengers ).value++;
		}
		else if (mode.equals( HitchHikingConstants.PASSENGER_MODE )) {
			getValue( event.getLinkId() , weightsForDrivers ).value++;
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

	private static MyDouble getValue(
			final Id key,
			final Map<Id, MyDouble> map) {
		MyDouble val = map.get( key );

		if (val == null) {
			val = new MyDouble();
			map.put( key , val );
		}

		return val;
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
				final Map<Id, MyDouble> weights) throws IOException {
			BufferedWriter writer = IOUtils.getBufferedWriter( fileName ); 

			writer.write( "linkId\tweight" );
			for (Map.Entry<Id, MyDouble> entry : weights.entrySet()) {
				writer.newLine();
				writer.write( entry.getKey() +"\t" +(entry.getValue().value + baseWeight) );
			}
			writer.close();
		}
	}
}

