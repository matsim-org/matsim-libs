/* *********************************************************************** *
 * project: org.matsim.*
 * GoogleAPIResult.java
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

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author thibautd
 */
public class GoogleAPIResult {
	private final JSONObject jsonResult;

	public static enum Status {
		/**
		 * indicates that no errors occurred; the address was successfully parsed and at least one geocode was returned.
		 */
		OK,
		/**
		 * indicates that the geocode was successful but returned no results. This may occur if the geocoder was passed a non-existent address.
		 */
		ZERO_RESULTS,
		/**
		 * indicates that you are over your quota.
		 */
		OVER_QUERY_LIMIT,
		/**
		 * indicates that your request was denied, generally because of lack of a sensor parameter.
		 */
		REQUEST_DENIED,
		/**
		 * generally indicates that the query (address, components or latlng) is missing.
		 */
		INVALID_REQUEST,
		/**
		 * indicates that the request could not be processed due to a server error. The request may succeed if you try again.
		 */
		UNKNOWN_ERROR;
	}

	public static enum LocationType {
		/**
		 * indicates that the returned result is a precise geocode for which we have
		 * location information accurate down to street address precision.
		 */
		ROOFTOP,
		/**
		 * indicates that the returned result reflects an approximation (usually on
		 * a road) interpolated between two precise points (such as intersections).
		 * Interpolated results are generally returned when rooftop geocodes are unavailable for a street address.
		 */
		RANGE_INTERPOLATED,
		/**
		 * indicates that the returned result is the geometric center of a result
		 * such as a polyline (for example, a street) or polygon (region).
		 */
		GEOMETRIC_CENTER,
		/**
		 * indicates that the returned result is approximate.
		 */
		APPROXIMATE;
	}

	public GoogleAPIResult( final JSONObject jsonResult ) {
		this.jsonResult = jsonResult;

	}

	private final JSONObject getResults() {
		final JSONArray arr = jsonResult.getJSONArray( "results" );
		if ( arr.length() != 1 ) throw new IllegalStateException( "only designed to handle unique results, got "+arr.length() );
		return arr.getJSONObject( 0 );
	}

	public String getFormattedAddress() {
		return (String) getResults().get( "formatted_address" );
	}

	public Double getLatitude() {
		return (Double) getResults()
				.getJSONObject( "geometry" )
				.getJSONObject( "location" )
				.get( "lat" );
	}

	public Double getLongitude() {
		return (Double) getResults()
				.getJSONObject( "geometry" )
				.getJSONObject( "location" )
				.get( "lng" );
	}

	public Status getStatus() {
		final String status = (String) jsonResult.get( "status" );
		return Status.valueOf( status );
	}

	public LocationType getLocationType() {
		final String type = (String) getResults().getJSONObject( "geometry" ).get( "location_type" );
		return LocationType.valueOf( type );
	}

	@Override
	public String toString() {
		return "{Status="+getStatus()+"; LocationType="+getLocationType()+"; lat="+getLatitude()+"; lng="+getLongitude()+"; formattedAddress="+getFormattedAddress()+"}";
	}
}


