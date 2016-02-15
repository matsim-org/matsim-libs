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

import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.net.URL;

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
		MapquestResult result = null;
		try {
			result = new MapquestResult( getJSONMapquestLocation( address ) );
		}
		catch (Exception e) {
			log.warn( "got exception for address "+address , e );
			log.warn( "trying again in 2 seconds." );
			try {
				Thread.sleep( 2000 );
			}
			catch (InterruptedException ie) {
				throw new RuntimeException( ie );
			}
			result = new MapquestResult( getJSONMapquestLocation( address ) );
		}

		switch ( result.getMapquestStatus() ) {
			case OK:
				return result;
			default:
				log.error( "problem with request "+address+": "+result.getMapquestStatus() );
				for ( String m : result.getMessages() ) {
					log.error( m );
				}
				return result;
		}
	}

	private JSONObject getJSONMapquestLocation( final Address address ) {
		final URL request = GeolocalizingAPIsUtils.pasteAsURL(
				"http://www.mapquestapi.com/geocoding/v1/address?",
				"key=", key , "&" ,
				GeolocalizingAPIsUtils.pasteNominatimAddressString( address ),
				"format=json" );

		if ( log.isTraceEnabled() ) log.trace( "send request "+request.toString() );
		try {
			return GeolocalizingAPIsUtils.toJSON( request );
		}
		catch (Exception e) {
			throw new RuntimeException( "problem with request "+request , e);
		}
	}
}

