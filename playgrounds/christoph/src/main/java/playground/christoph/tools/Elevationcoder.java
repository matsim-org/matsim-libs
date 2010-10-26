/* *********************************************************************** *
 * project: org.matsim.*
 * Elevationcoder.java
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

package playground.christoph.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class Elevationcoder {

/*
 * See http://code.google.com/intl/de/apis/maps/documentation/elevation/ 
 */ 
	
/*	
 * Sample response:	
	<?xml version="1.0" encoding="UTF-8"?>
	<ElevationResponse>
	 <status>OK</status>
	 <result>
	  <location>
	   <lat>47.3690239</lat>
	   <lng>8.5380326</lng>
	  </location>
	  <elevation>408.4848938</elevation>
	 </result>
	</ElevationResponse>
*/
	
	public static GoogleResponse getElevation (String lat, String lon) throws IOException {
//		http://maps.google.com/maps/api/elevation/outputFormat?parameters
//		http://maps.google.com/maps/api/elevation/xml?locations=39.7391536,-104.9847034&sensor=false
		String url = "http://maps.google.com/maps/api/elevation/xml?locations=";
		url = url + String.valueOf(lat);
		url = url + ",";
		url = url + String.valueOf(lon);
		url = url + "&sensor=false";
		
		BufferedReader in = new BufferedReader (new InputStreamReader (new URL (url).openStream ()));
		String line;
		GoogleResponse googleResponse = new GoogleResponse();
		googleResponse.lat = Double.valueOf(lat);
		googleResponse.lon = Double.valueOf(lon);
		
		while ((line = in.readLine ()) != null) {
			line = line.trim();
			if (line.startsWith("<status>")) {
				line = line.replace("<status>", "");
				line = line.replace("</status>", "");
				googleResponse.status = line;
				if (!line.toLowerCase().equals("ok")) return googleResponse;
			}
			else if (line.startsWith("<elevation>")) {
				line = line.replace("<elevation>", "");
				line = line.replace("</elevation>", "");
				googleResponse.elevation = Double.valueOf(line);
				return googleResponse;
			}
		}
		return googleResponse;
	}
	
	public static class GoogleResponse {
		public String status = null;
		public double lon = Double.NaN;
		public double lat = Double.NaN;
		public double elevation = Double.NaN;

		private GoogleResponse () {
		}

		@Override
		public String toString () { 
			return "Latitude: " + lat + ", Longitude: " + lon + ", Elevation: " + elevation; 
		}
	}
	
	public static void main (String[] argv) throws Exception {
		Geocoder.Location zurich = Geocoder.getLocation ("Zürich");
		GoogleResponse googleResponse = getElevation(zurich.lat, zurich.lon);
		System.out.println(googleResponse.toString());
	}
}
