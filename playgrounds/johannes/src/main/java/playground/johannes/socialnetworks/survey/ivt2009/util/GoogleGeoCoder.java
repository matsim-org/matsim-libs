/* *********************************************************************** *
 * project: org.matsim.*
 * GeoCoder2.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.survey.ivt2009.util;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.code.geocoder.AdvancedGeoCoder;
import com.google.code.geocoder.Geocoder;
import com.google.code.geocoder.GeocoderRequestBuilder;
import com.google.code.geocoder.model.GeocodeResponse;
import com.google.code.geocoder.model.GeocoderRequest;
import com.google.code.geocoder.model.GeocoderStatus;
import com.google.code.geocoder.model.LatLng;

/**
 * @author illenberger
 * 
 */
public class GoogleGeoCoder {
	
	private static final Logger logger = Logger.getLogger(GoogleGeoCoder.class);
	
	private final Geocoder geocoder;
	
	private final long sleepInterval;
	
	private HashMapCash cache = new HashMapCash();

	public GoogleGeoCoder() {
		this(null, 0, 0);
	}
	
	public GoogleGeoCoder(String proxy, int port, long sleep) {
		HttpClient httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
		if(proxy != null) {
			httpClient.getHostConfiguration().setProxy(proxy, port);
		}
		geocoder = new AdvancedGeoCoder(httpClient);
		
		this.sleepInterval = sleep;
	}

	public LatLng requestCoordinate(String query) {
		Level level = Logger.getRootLogger().getLevel();
		Logger.getRootLogger().setLevel(Level.INFO);
		
		GeocodeResponse response = cache.get(query);
		if (response == null) {
			try {
				Thread.sleep(sleepInterval);
				GeocoderRequest geocoderRequest = new GeocoderRequestBuilder().setAddress(query).setLanguage("de").getGeocoderRequest();
				response = geocoder.geocode(geocoderRequest);
				if(response.getStatus() == GeocoderStatus.OK) {
					cache.put(query, response);
				} else if(response.getStatus() == GeocoderStatus.ZERO_RESULTS){
					logger.warn(String.format("No results for query \"%s\" found.", query));
				} else {
					logger.warn(String.format("Request failed with error \"%s\".", response.getStatus().name()));
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		Logger.getRootLogger().setLevel(level);
		
		if(response != null) {
			if(!response.getResults().isEmpty()) {
				LatLng c = response.getResults().get(0).getGeometry().getLocation();
				return c;//new CoordImpl(c.getLng().doubleValue(), c.getLat().doubleValue());
			} else { // should not happen
				logger.warn(String.format("No results for query \"%s\" found.", query));
			}
		}
		
		return null;
	}

	private static class HashMapCash extends LinkedHashMap<String, GeocodeResponse> {

		private static final long serialVersionUID = 1L;

		private static final int MAX_ENTRIES = 100;

		protected boolean removeEldestEntry(Map.Entry<String, GeocodeResponse> eldest) {
			return size() > MAX_ENTRIES;
		}
	}
}
