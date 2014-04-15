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
public class GoogleAPIResult implements GeolocalizationResult {
	private final JSONObject jsonResult;

	public static enum GoogleStatus {
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

	private JSONObject getJSONResults(final int i) {
		final JSONArray arr = jsonResult.getJSONArray( "results" );
		return arr.getJSONObject( i );
	}

	public int getNumberResults() {
		final JSONArray arr = jsonResult.getJSONArray( "results" );
		return arr.length();
	}

	public Result getResults( final int i ) {
		return new Result( i );
	}

	@Override
	public Status getStatus() {
		switch ( getGoogleStatus() ) {
		case OK:
			return Status.OK;
		case OVER_QUERY_LIMIT:
			return Status.ABORT;
		case ZERO_RESULTS:
			return Status.NO_RESULT;
		case INVALID_REQUEST:
		case REQUEST_DENIED:
		case UNKNOWN_ERROR:
		default:
			return Status.ERROR;
		}
	}
	
	public GoogleStatus getGoogleStatus() {
		final String status = (String) jsonResult.get( "status" );
		return GoogleStatus.valueOf( status );
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
			return (String) getJSONResults( i ).get( "formatted_address" );
		}

		public Double getLatitude() {
			return (Double) getJSONResults( i )
					.getJSONObject( "geometry" )
					.getJSONObject( "location" )
					.get( "lat" );
		}

		public Double getLongitude() {
			return (Double) getJSONResults( i )
					.getJSONObject( "geometry" )
					.getJSONObject( "location" )
					.get( "lng" );
		}

		public LocationType getLocationType() {
			final String type = (String) getJSONResults( i ).getJSONObject( "geometry" ).get( "location_type" );
			return LocationType.valueOf( type );
		}
	}
}


