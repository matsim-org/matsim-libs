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

import geo.google.GeoAddressStandardizer;
import geo.google.GeoException;
import geo.google.datamodel.GeoCoordinate;
import geo.google.datamodel.GeoStatusCode;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import playground.johannes.socialnetworks.survey.ivt2009.util.GoogleLocationLookup.RequestLimitException;

/**
 * @author illenberger
 * 
 */
public class GoogleGeoCoder {
	
	private static final Logger logger = Logger.getLogger(GoogleGeoCoder.class);
	
	private static final long START_INTERVAL = 5800;
	
	private static final long MAX_INTERVAL = 6000;
	
	private static final long STEP_INTERVAL = 100;

	private GeoAddressStandardizer standardizer = new GeoAddressStandardizer("");

	private HashMapCash cache = new HashMapCash();

	public GoogleGeoCoder() {
		standardizer.setRateLimitInterval(START_INTERVAL);
	}
	
	public GeoCoordinate requestCoordinate(String query) {
		Level level = Logger.getRootLogger().getLevel();
		Logger.getRootLogger().setLevel(Level.INFO);
		
		GeoCoordinate coord = cache.get(query);
		if (coord == null) {
			try {
				coord = standardizer.standardizeToGeoCoordinate(query);
				if(coord != null)
					cache.put(query, coord);
				
			} catch (GeoException e) {
				logger.warn(String.format("%1$s: %2$s", e.getStatus().getCode(), e.getStatus().getDescription()));
				
				if(e.getStatus().getCode() == GeoStatusCode.G_GEO_TOO_MANY_QUERIES.getCode()) {
					standardizer.setRateLimitInterval(standardizer.getRateLimitInterval() + STEP_INTERVAL);
					logger.warn(String.format("Increasing request intervall (%1$s ms).", standardizer.getRateLimitInterval()));
				
					if(standardizer.getRateLimitInterval() > MAX_INTERVAL) {
						throw new RequestLimitException();
					}
				}
			}
		}
		
		Logger.getRootLogger().setLevel(level);
		
		return coord;
	}

	private static class HashMapCash extends LinkedHashMap<String, GeoCoordinate> {

		private static final long serialVersionUID = 1L;

		private static final int MAX_ENTRIES = 100;

		protected boolean removeEldestEntry(Map.Entry<String, GeoCoordinate> eldest) {
			return size() > MAX_ENTRIES;
		}
	}
}
