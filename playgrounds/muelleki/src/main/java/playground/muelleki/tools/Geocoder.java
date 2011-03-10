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
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.regex.*;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class Geocoder {
	private final static String ENCODING = "UTF-8";
	
	// Google Maps key for localhost
	private final static String KEY = "ABQIAAAAnfs7bKE82qgb3Zc2YyS-oBT2yXp_ZAY8_ufC3CFXhHIE1NvwkxSySz_REpPq-4WZA27OwgbtyR3VcA";

	
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
	     * Constructs an {@code TooManyQueriesException} with the specified cause.
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
	
	// Format: 200,6,42.730070,-73.690570
	private static Pattern _googleApiReply = Pattern.compile("([0-9]+),[0-9],([0-9.+-]+),([0-9.+-]+)");

	public static Coord getLocation (String address) throws IOException {
		BufferedReader in = new BufferedReader (new InputStreamReader (new URL ("http://maps.google.com/maps/geo?q="+URLEncoder.encode (address, ENCODING)+"&output=csv&key="+KEY).openStream ()));
		String line;
		Coord location = null;
		int statusCode = -1;
		
		while ((line = in.readLine ()) != null) {
			Matcher m = _googleApiReply.matcher(line);
			
			if (!m.matches())
				continue;

			statusCode = Integer.parseInt (m.group(1));
			String lat = m.group(2);
			String lon = m.group(3);
			if (statusCode == 200) { 
				location = new CoordImpl (Double.parseDouble(lon), Double.parseDouble(lat));
				break;
			}
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
		BufferedReader in;
		if (argv.length > 0)
			in = new BufferedReader (new InputStreamReader(new FileInputStream(argv[0])));
		else
			in = new BufferedReader (new InputStreamReader (System.in));
		
		CoordinateTransformation tr = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.CH1903_LV03_GT);
		
		String line;
		Pattern pattern = Pattern.compile("([^\t]*).*");
		while ((line = in.readLine ()) != null) {
			Matcher matcher = pattern.matcher(line);
			matcher.matches();
			Coord loc = null;

			while (true) {
				try {
					loc = Geocoder.getLocation (matcher.group(1));
				}
				catch (TooManyQueriesException e) {
					Thread.sleep(60000);
					continue;
				}
				break;
			}
			
			Coord loct = tr.transform(loc);
			
			System.out.printf (Locale.US, "%s\t%f\t%f\t%f\t%f\n", line, loc.getX(), loc.getY(), loct.getX(), loct.getY());
			Thread.sleep(300);
		}
	}
}
