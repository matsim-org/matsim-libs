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

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author thibautd
 */
public class GeolocalizingAPIsUtils {
	public static JSONObject toJSON(final URL request) {
		try {
			final JSONTokener tokener = new JSONTokener( request.openStream() );
			return new JSONObject( tokener );
		}
		catch (final JSONException e) {
			throw new RuntimeException( e );
		}
		catch (final IOException e) {
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

	public static String pasteNominatimAddressString(final Address address) {
		final StringBuilder builder = new StringBuilder();
		
		if ( address.getStreet() != null ) {
			builder.append( "street=" );
			builder.append( address.getStreet().replace( ' ' , '+' ) );
			builder.append( "+" );

			if ( address.getNumber() != null ) {
				// number with spaces are possible (for instance igendeinestrasse 60 F)
				builder.append( address.getNumber().replace( ' ' , '+' ) );
				builder.append( "+" );
			}
			builder.append( "&" );
		}
		if ( address.getZipcode() != null ) {
			builder.append( "postalCode=" );
			builder.append( address.getZipcode() );
			builder.append( "&" );
		}
		if ( address.getMunicipality() != null ) {
			builder.append( "city=" );
			builder.append( address.getMunicipality().replace( ' ' , '+' ) );
			builder.append( "&" );
		}
		if ( address.getCountry() != null ) {
			builder.append( "country=" );
			builder.append( address.getCountry().replace( ' ' , '+' ) );
			builder.append( "&" );
		}

		return builder.toString();
	}

	public static URL pasteAsURL(final String... pieces) {
		final StringBuilder builder = new StringBuilder();
		for ( final String p : pieces ) builder.append( p );
		try {
			return new URL( builder.toString() );
		}
		catch (final MalformedURLException e) {
			throw new RuntimeException( e );
		}
	}

}

