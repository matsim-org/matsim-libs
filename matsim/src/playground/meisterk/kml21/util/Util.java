/* *********************************************************************** *
 * project: org.matsim.*
 * Util.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.meisterk.kml21.util;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import playground.meisterk.kml21.StyleSelectorType;

public class Util {

	public static String getKMLDateString(GregorianCalendar gc) {
		
		SimpleDateFormat kmlDateTimeUTC = new SimpleDateFormat("yyyy-MM-DD'T'HH:mm:ss'Z'");
		return kmlDateTimeUTC.format(gc.getTime());

	}
	
	public static String getKMLCoordinateString(double longitude, double latitude, double altitude) {
		
		return new String(longitude + "," + latitude + "," + altitude);
		
	}
	
	public static String getStyleUrlString(StyleSelectorType styleSelector) {

		return new String("#" + styleSelector.getId());

	}
	
	public static OutputStream getOutputStream(String kmlFilename, boolean useCompression) throws FileNotFoundException {
		
		OutputStream os;
		if (useCompression) {
			String outFilename = kmlFilename.substring(0, kmlFilename.length() - 4);
			os = new ZipOutputStream(new FileOutputStream(outFilename.concat(".kmz")));
			ZipEntry ze = new ZipEntry(kmlFilename);
			ze.setMethod(ZipEntry.DEFLATED);
			try {
				((ZipOutputStream) os).putNextEntry(ze);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		} else {
			os = new FileOutputStream(kmlFilename);
		}
		
		return os;
		
	}
}
