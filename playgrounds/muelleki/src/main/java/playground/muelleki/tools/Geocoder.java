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
import java.util.regex.*;

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
			return lat+"\t"+lon; 
		}
	}
	
	public static class TooManyQueriesException extends IOException {

		/**
		 * 
		 */
		private static final long serialVersionUID = -2893388603747747668L;
		
		private static final String _desc = "Too many queries";
		

	    /**
	     * Constructs an {@code IOException} with {@code null}
	     * as its error detail message.
	     */
	    public TooManyQueriesException() {
		super(_desc);
	    }

	    /**
	     * Constructs an {@code TooManyQueriesException} with the specified cause and a
	     * detail message of {@code (cause==null ? null : cause.toString())}
	     * (which typically contains the class and detail message of {@code cause}).
	     * This constructor is useful for IO exceptions that are little more
	     * than wrappers for other throwables.
	     *
	     * @param cause
	     *        The cause (which is saved for later retrieval by the
	     *        {@link #getCause()} method).  (A null value is permitted,
	     *        and indicates that the cause is nonexistent or unknown.)
	     *
	     * @since 1.6
	     */
	    public TooManyQueriesException(Throwable cause) {
	        super(_desc, cause);
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
				case 620: throw new TooManyQueriesException();
			}
		}
		return location;
	}

	public static void main (String[] argv) throws Exception {
		BufferedReader in = new BufferedReader (new InputStreamReader (System.in));
		String line;
		Pattern pattern = Pattern.compile("([^\t]*).*");
		while ((line = in.readLine ()) != null) {
			Matcher matcher = pattern.matcher(line);
			matcher.matches();
			Location loc = null;

			do {
				try {
					loc = Geocoder.getLocation (matcher.group(1));
				}
				catch (TooManyQueriesException e) {
					Thread.sleep(60000);
				}
			}
			while (loc == null);
			
			System.out.printf ("%s\t%s\n", line, loc);
			Thread.sleep(300);
		}
	}
}
