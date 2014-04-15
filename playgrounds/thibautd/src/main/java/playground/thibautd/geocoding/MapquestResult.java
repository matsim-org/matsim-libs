/* *********************************************************************** *
 * project: org.matsim.*
 * MapquestResult.java
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

import java.util.ArrayList;
import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author thibautd
 */
public class MapquestResult {
	private final JSONObject jsonResult;

	public static enum Status {
		OK,
		INPUT_ERROR,
		KEY_ERROR,
		UNKNOWN_ERROR;
	}

	public static enum GeocodeQuality {
		POINT,
		ADDRESS,
		INTERSECTION,
		STREET,
		COUNTRY,
		STATE,
		COUNTY,
		CITY,
		ZIP,
		ZIP_EXTENDED;
	}

	public MapquestResult( final JSONObject jsonResult ) {
		this.jsonResult = jsonResult;
	}

	public int getNumberResults() {
		final JSONArray arr = jsonResult.getJSONArray( "results" );
		return arr.length();
	}

	public Result getResults( final int i ) {
		return new Result( i );
	}

	public Status getStatus() {
		final Integer status = (Integer) jsonResult.getJSONObject( "info" )
			.get( "statuscode" );

		switch ( status.intValue() ) {
			case 0: return Status.OK;
			case 400: return Status.INPUT_ERROR;
			case 403: return Status.KEY_ERROR;
			case 500: return Status.UNKNOWN_ERROR;
			default: throw new IllegalArgumentException( "unknown return code "+status );
		}
	}

	public Collection<String> getMessages() {
		final JSONArray arr = jsonResult.getJSONObject( "info" ).getJSONArray( "messages" );
		final Collection<String> msg = new ArrayList<String>( arr.length() );

		for ( int i=0; i < arr.length(); i++ ) {
			msg.add( arr.getString( i ) );
		}

		return msg;
	}

	public class Result {
		private final int i;

		private Result( final int i ) {
			this.i = i;
		}

		public int getIndex() {
			return i;
		}

		public String getFormattedAddress() {
			final String street = (String) getJSONResults().get( "street" );
			final String city = (String) getJSONResults().get( "adminArea5" );
			final String country = (String) getJSONResults().get( "adminArea1" );
			final String zip = (String) getJSONResults().get( "postalCode" );

			return street+", "+zip+" "+city+", "+country;
		}

		public Double getLatitude() {
			return (Double) getJSONResults()
					.getJSONObject( "latLng" )
					.get( "lat" );
		}

		public Double getLongitude() {
			return (Double) getJSONResults()
					.getJSONObject( "latLng" )
					.get( "lng" );
		}

		public GeocodeQuality getGeocodeQuality() {
			final String type = (String) getJSONResults().get( "geocodeQuality" );
			return GeocodeQuality.valueOf( type );
		}

		public String getGeocodeQualityCode() {
			// see http://open.mapquestapi.com/geocoding/geocodequality.html
			return (String) getJSONResults().get( "geocodeQualityCode" );
		}

		private JSONObject getJSONResults() {
			final JSONArray arr = jsonResult.getJSONArray( "results" );
			if ( arr.length() != 1 ) throw new RuntimeException( "cannot handle multiple result lists. got "+arr.length() );
			final JSONObject resultList = arr.getJSONObject( 0 );
			return resultList.getJSONArray( "locations" ).getJSONObject( i );
		}
	}
}

