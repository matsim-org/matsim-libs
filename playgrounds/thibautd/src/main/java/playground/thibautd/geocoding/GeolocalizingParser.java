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

import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * @author thibautd
 */
public class GeolocalizingParser {
	private static final Logger log =
		Logger.getLogger(GeolocalizingParser.class);

	public static enum RejectCause { error, abort; }

	public static interface GeolocalizationListenner {
		public void handleResult( Address address , GoogleAPIResult result );
	}

	public static interface NonlocalizedAddressListenner {
		public void handleNonlocalizedAddress( Address address , RejectCause cause );
	}

	private final GoogleAPIUtils utils;

	public GeolocalizingParser( final GoogleAPIUtils utils ) {
		this.utils = utils;
	}

	public void parse(
			final Iterator<? extends Address> addressProvider,
			final GeolocalizationListenner geolocalisationListenner,
			final NonlocalizedAddressListenner nonlocalizedAddressListenner) {
		parseUntilLimit( addressProvider , geolocalisationListenner , nonlocalizedAddressListenner );

		while ( addressProvider.hasNext() ) {
			final Address address = addressProvider.next();
			nonlocalizedAddressListenner.handleNonlocalizedAddress( address , RejectCause.abort );
		}
	}

	public void parseUntilLimit(
			final Iterator<? extends Address> addressProvider,
			final GeolocalizationListenner geolocalisationListenner,
			final NonlocalizedAddressListenner nonlocalizedAddressListenner) {
		while ( addressProvider.hasNext() ) {
			final Address address = addressProvider.next();

			final GoogleAPIResult result = utils.getLocation( address );

			switch ( result.getStatus() ) {
				case OVER_QUERY_LIMIT:
					log.error( "reached limit. Try processing the rest latter." );
					nonlocalizedAddressListenner.handleNonlocalizedAddress( address , RejectCause.abort );
					return;
				case INVALID_REQUEST:
					log.error( "invalid request for Address "+address );
					nonlocalizedAddressListenner.handleNonlocalizedAddress( address , RejectCause.error );
					break;
				case REQUEST_DENIED:
					log.error( "denied request for Address "+address );
					nonlocalizedAddressListenner.handleNonlocalizedAddress( address , RejectCause.error );
					break;
				case UNKNOWN_ERROR:
					log.error ( "unknown error for Address "+address );
					nonlocalizedAddressListenner.handleNonlocalizedAddress( address , RejectCause.error );
					break;
				case ZERO_RESULTS:
					log.warn( "Address "+address+" gave no result!" );
					break;
				case OK:
					geolocalisationListenner.handleResult( address , result );
					break;
				default:
					throw new RuntimeException();
			}
		}
	}

}

