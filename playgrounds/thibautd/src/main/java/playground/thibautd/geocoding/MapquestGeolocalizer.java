/* *********************************************************************** *
 * project: org.matsim.*
 * MapquestGeolocalizer.java
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

import java.net.URL;

import org.apache.log4j.Logger;

import org.json.JSONObject;

/**
 * @author thibautd
 */
public class MapquestGeolocalizer implements Geolocalizer<MapquestResult> {
	private static final Logger log =
		Logger.getLogger(MapquestGeolocalizer.class);

	private final String key;

	public MapquestGeolocalizer() {
		this( null );
	}

	public MapquestGeolocalizer(final String key ) {
		this.key = key;
	}

	@Override
	public MapquestResult getLocation( final Address address ) {
		final MapquestResult result = new MapquestResult( getJSONMapquestLocation( address ) );

		switch ( result.getStatus() ) {
			case OK:
				return result;
			default:
				log.error( "problem with request: "+result.getStatus() );
				for ( String m : result.getMessages() ) {
					log.error( m );
				}
				throw new RuntimeException();
		}
	}

	private JSONObject getJSONMapquestLocation( final Address address ) {
		final URL request = GeolocalizingAPIsUtils.pasteAsURL(
				"http://www.mapquestapi.com/geocoding/v1/address?",
				"key=", key , "&" ,
				GeolocalizingAPIsUtils.pasteNominatimAddressString( address ),
				"format=json" );

		if ( log.isTraceEnabled() ) log.trace( "send request "+request.toString() );
		return GeolocalizingAPIsUtils.toJSON( request );
	}
}

