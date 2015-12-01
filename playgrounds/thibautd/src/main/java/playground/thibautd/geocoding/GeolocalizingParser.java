/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractGeolocalizingParser.java
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
package playground.thibautd.geocoding;

import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.Counter;

import java.util.Iterator;

/**
 * @author thibautd
 */
public class GeolocalizingParser<T extends GeolocalizationResult> {
	private static final Logger log =
		Logger.getLogger(GeolocalizingParser.class);

	public static interface GeolocalizationListenner<TT extends GeolocalizationResult> {
		public void handleResult( Address address , TT result );
	}

	public static interface NonlocalizedAddressListenner {
		public void handleNonlocalizedAddress( Address address , Status cause );
	}

	private final Geolocalizer<T> utils;

	public GeolocalizingParser( final Geolocalizer<T> utils ) {
		this.utils = utils;
	}

	public void parse(
			final Iterator<? extends Address> addressProvider,
			final GeolocalizationListenner<T> geolocalisationListenner,
			final NonlocalizedAddressListenner nonlocalizedAddressListenner) {
		parseUntilLimit( addressProvider , geolocalisationListenner , nonlocalizedAddressListenner );

		final Counter counter = new Counter( "Process aborted address # " );
		while ( addressProvider.hasNext() ) {
			counter.incCounter();
			final Address address = addressProvider.next();
			nonlocalizedAddressListenner.handleNonlocalizedAddress( address , Status.ABORT );
		}
		counter.printCounter();
	}

	public void parseUntilLimit(
			final Iterator<? extends Address> addressProvider,
			final GeolocalizationListenner<T> geolocalisationListenner,
			final NonlocalizedAddressListenner nonlocalizedAddressListenner) {
		final Counter counter = new Counter( "Parse adress # " );
		while ( addressProvider.hasNext() ) {
			counter.incCounter();
			final Address address = addressProvider.next();

			final T result = utils.getLocation( address );

			switch ( result.getStatus() ) {
				case ABORT:
					counter.printCounter();
					log.error( "reached limit. Try processing the rest latter." );
					nonlocalizedAddressListenner.handleNonlocalizedAddress( address , result.getStatus() );
					return;
				case ERROR:
					log.error( "error for Address "+address );
					nonlocalizedAddressListenner.handleNonlocalizedAddress( address , result.getStatus() );
				case NO_RESULT:
					log.warn( "Address "+address+" gave no result!" );
					nonlocalizedAddressListenner.handleNonlocalizedAddress( address , result.getStatus() );
					break;
				case OK:
					geolocalisationListenner.handleResult( address , result );
					break;
				default:
					throw new RuntimeException();
			}
		}
		counter.printCounter();
	}

}

