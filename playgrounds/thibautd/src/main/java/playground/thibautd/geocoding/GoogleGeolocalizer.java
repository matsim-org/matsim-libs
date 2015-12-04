/* *********************************************************************** *
 * project: org.matsim.*
 * GoogleGeolocalizer.java
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
import org.json.JSONObject;

import java.net.URL;

/**
 * @author thibautd
 */
public class GoogleGeolocalizer implements Geolocalizer<GoogleAPIResult> {
	private static final Logger log =
		Logger.getLogger(GoogleGeolocalizer.class);

	private final String key;

	public GoogleGeolocalizer() {
		this( null );
	}

	public GoogleGeolocalizer(final String key ) {
		this.key = key;
	}

	@Override
	public GoogleAPIResult getLocation( final Address address ) {
		final GoogleAPIResult result = new GoogleAPIResult( getJSONGoogleLocation( address ) );

		switch ( result.getGoogleStatus() ) {
			case OVER_QUERY_LIMIT:
				log.trace( "reached limit: try pausing 2secs." );
				try {
					Thread.sleep( 2000 );
				}
				catch (InterruptedException e) {
					throw new RuntimeException( e );
				}
				return new GoogleAPIResult( getJSONGoogleLocation( address ) );
			default:
				return result;
		}
	}

	private JSONObject getJSONGoogleLocation( final Address address ) {
		final URL request = GeolocalizingAPIsUtils.pasteAsURL(
				"http://maps.googleapis.com/maps/api/geocode/",
				"json?",
				"address=",
				GeolocalizingAPIsUtils.pasteAddressString( address ),
				"&sensor=false",  // absolutely no idea why this is needed...
				key == null ? "" : "&key="+key );
		if ( log.isTraceEnabled() ) log.trace( "send request "+request.toString() );
		return GeolocalizingAPIsUtils.toJSON( request );
	}
}



