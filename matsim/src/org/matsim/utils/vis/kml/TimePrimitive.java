/* *********************************************************************** *
 * project: org.matsim.*
 * TimePrimitive.java
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

package org.matsim.utils.vis.kml;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

/**
 * For documentation, refer to
 * <a href="http://code.google.com/apis/kml/documentation/kmlreference.html#timeprimitive">
 * http://code.google.com/apis/kml/documentation/kmlreference.html#timeprimitive</a>
 * 
 * @author dgrether, meisterk, mrieser
 * @deprecated For working with KML files, please use the library kml-2.2-jaxb-2.1.7.jar. 
 * See ch.ethz.ivt.KMLDemo in that library for examples of usage.
 *
 */
public abstract class TimePrimitive extends Object {

	/* DateFormats are not thread-safe, and should not be static for this reason
	 * as static members are more likely to be accessed from different places at
	 * the same time.      	 */
	private final SimpleDateFormat KML_DATETIME_UTC = new SimpleDateFormat("yyyy-MM-DD'T'HH:mm:ss'Z'");

	/**
	 * Constructs an empty <code>TimePrimitive</code> object.
	 */
	protected TimePrimitive() {

		super("");

	}

	/**
	 * Formats a date into the UTC <it>dateTime</it> resolution of the KML
	 * language.
	 *
	 * For documentation, refer to
	 * <a href="http://earth.google.com/kml/kml_tags_21.html#timestamp">
	 * http://earth.google.com/kml/kml_tags_21.html#timestamp</a>
	 *
	 * @param gc the date to be formatted
	 * @return
	 * the string that may be written into a KML file
	 */
	protected String getKMLDateFormat(final GregorianCalendar gc) {
		return KML_DATETIME_UTC.format(gc.getTime());
	}

}
