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

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;

/**
 * @author thibautd
 */
public class MapquestResult implements GeolocalizationResult {
	private final JSONObject jsonResult;

	public static enum MapquestStatus {
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
		ZIP_EXTENDED,
		UNKNOWN;
	}

	public MapquestResult( final JSONObject jsonResult ) {
		this.jsonResult = jsonResult;
	}

	public int getNumberResults() {
		final JSONArray arr = getLocationsArray();
		return arr.length();
	}

	public Result getResults( final int i ) {
		return new Result( i );
	}

	@Override
	public Status getStatus() {
		switch ( getMapquestStatus() ) {
			case OK:
				return getNumberResults() > 0  ? Status.OK : Status.NO_RESULT;
			case INPUT_ERROR:
			case KEY_ERROR:
			case UNKNOWN_ERROR:
			default:
				return Status.ERROR;
		}
	}
	
	public MapquestStatus getMapquestStatus() {
		final Integer status = (Integer) jsonResult.getJSONObject( "info" )
			.get( "statuscode" );

		switch ( status.intValue() ) {
			case 0: return MapquestStatus.OK;
			case 400: return MapquestStatus.INPUT_ERROR;
			case 403: return MapquestStatus.KEY_ERROR;
			case 500: return MapquestStatus.UNKNOWN_ERROR;
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
			return getStreet()+", "+getZip()+" "+getCity()+", "+getCountry();
		}

		public String getStreet() {
			return (String) getJSONResults().get( "street" );
		}

		public String getCity() {
			return (String) getJSONResults().get( "adminArea5" );
		}

		public String getCountry() {
			return (String) getJSONResults().get( "adminArea1" );
		}

		public String getZip() {
			return (String) getJSONResults().get( "postalCode" );
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

		public Coord getCH03Coord() {
			final CoordinateTransformation t = new WGS84toCH1903LV03();
			final Coord wgs = new Coord(getLongitude(), getLatitude());
			return t.transform( wgs );
		}

		public GeocodeQuality getGeocodeQuality() {
			final String type = (String) getJSONResults().get( "geocodeQuality" );
			return GeocodeQuality.valueOf( type );
		}

		public String getGeocodeQualityCode() {
			// see http://open.mapquestapi.com/geocoding/geocodequality.html
			return ""+getJSONResults().get( "geocodeQualityCode" );
		}

		private JSONObject getJSONResults() {
			return getLocationsArray().getJSONObject( i );
		}
	}

	private JSONArray getLocationsArray() {
		final JSONArray arr = jsonResult.getJSONArray( "results" );
		if ( arr.length() != 1 ) throw new RuntimeException( "cannot handle multiple result lists. got "+arr.length() );
		final JSONObject resultList = arr.getJSONObject( 0 );
		return resultList.getJSONArray( "locations" );
	}
}

