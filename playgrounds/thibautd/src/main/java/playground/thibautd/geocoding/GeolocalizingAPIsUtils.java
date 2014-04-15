/* *********************************************************************** *
 * project: org.matsim.*
 * GoogleAPIUtils.java
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

import java.io.IOException;

import java.lang.InterruptedException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;

import org.json.JSONException;
import org.json.JSONTokener;
import org.json.JSONObject;

import org.matsim.core.utils.io.UncheckedIOException;

/**
 * @author thibautd
 */
public class GeolocalizingAPIsUtils {
	private static final Logger log =
		Logger.getLogger(GeolocalizingAPIsUtils.class);

	private final String key;

	public GeolocalizingAPIsUtils() {
		this( null );
	}

	public GeolocalizingAPIsUtils(final String key ) {
		this.key = key;
	}

	public GoogleAPIResult getLocation( final Address address ) {
		final GoogleAPIResult result = new GoogleAPIResult( getJSONLocation( address ) );

		switch ( result.getStatus() ) {
			case OVER_QUERY_LIMIT:
				log.trace( "reached limit: try pausing 2secs." );
				try {
					Thread.sleep( 2000 );
				}
				catch (InterruptedException e) {
					throw new RuntimeException( e );
				}
				return new GoogleAPIResult( getJSONLocation( address ) );
			default:
				return result;
		}
	}

	public JSONObject getJSONLocation( final Address address ) {
		final URL request = pasteAsURL(
				"http://maps.googleapis.com/maps/api/geocode/",
				"json?",
				"address=",
				pasteAddressString( address ),
				"&sensor=false",  // absolutely no idea why this is needed...
				key == null ? "" : "&key="+key );
		if ( log.isTraceEnabled() ) log.trace( "send request "+request.toString() );
		return toJSON( request );
	}

	private static JSONObject toJSON(final URL request) {
		try {
			final JSONTokener tokener = new JSONTokener( request.openStream() );
			return new JSONObject( tokener );
		}
		catch (JSONException e) {
			throw new RuntimeException( e );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	public static String pasteAddressString(final Address address) {
		final StringBuilder builder = new StringBuilder();
		
		if ( address.getNumber() != null ) {
			// number with spaces are possible (for instance igendeinestrasse 60 F)
			builder.append( address.getNumber().replace( ' ' , '+' ) );
			builder.append( "+" );
		}
		if ( address.getStreet() != null ) {
			builder.append( address.getStreet().replace( ' ' , '+' ) );
			builder.append( "+" );
		}
		if ( address.getZipcode() != null ) {
			builder.append( address.getZipcode() );
			builder.append( "+" );
		}
		if ( address.getMunicipality() != null ) {
			builder.append( address.getMunicipality().replace( ' ' , '+' ) );
			builder.append( "+" );
		}
		if ( address.getCountry() != null ) {
			builder.append( address.getCountry().replace( ' ' , '+' ) );
		}

		return builder.toString();
	}

	private static URL pasteAsURL(final String... pieces) {
		final StringBuilder builder = new StringBuilder();
		for ( String p : pieces ) builder.append( p );
		try {
			return new URL( builder.toString() );
		}
		catch (MalformedURLException e) {
			throw new RuntimeException( e );
		}
	}
}

