/* *********************************************************************** *
 * project: org.matsim.*
 * Geocoder.java
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

package playground.muelleki.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;

public class Geocoder {
	private final static String ENCODING = "UTF-8";
	
	// Google Maps key for localhost
	private final static String KEY = "ABQIAAAAnfs7bKE82qgb3Zc2YyS-oBT2yXp_ZAY8_ufC3CFXhHIE1NvwkxSySz_REpPq-4WZA27OwgbtyR3VcA";

	
	public static class Location {
		public String lon, lat;

		private Location (String lat, String lon) {
			this.lon = lon;
			this.lat = lat;
		}

		@Override
		public String toString () { 
			return "Lat: "+lat+", Lon: "+lon; 
		}
	}
	
	public static Location getLocation (String address) throws IOException {
		BufferedReader in = new BufferedReader (new InputStreamReader (new URL ("http://maps.google.com/maps/geo?q="+URLEncoder.encode (address, ENCODING)+"&output=csv&key="+KEY).openStream ()));
		String line;
		Location location = null;
		int statusCode = -1;
		
		while ((line = in.readLine ()) != null) {
			// Format: 200,6,42.730070,-73.690570
			statusCode = Integer.parseInt (line.substring (0, 3));
			if (statusCode == 200) location = new Location (line.substring ("200,6,".length (), line.indexOf (',', "200,6,".length ())), line.substring (line.indexOf (',', "200,6,".length ())+1, line.length ()));
		}
		
		if (location == null) {
			switch (statusCode) {
				case 400: throw new IOException ("Bad Request");
				case 500: throw new IOException ("Unknown error from Google Encoder");
				case 601: throw new IOException ("Missing query");
				case 602: return null;
				case 603: throw new IOException ("Legal problem");
				case 604: throw new IOException ("No route");
				case 610: throw new IOException ("Bad key");
				case 620: throw new IOException ("Too many queries");
			}
		}
		return location;
	}

	public static void main (String[] argv) throws Exception {
		System.out.println (Geocoder.getLocation ("Zürich"));
	}
}