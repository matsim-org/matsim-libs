/* *********************************************************************** *
 * project: org.matsim.*
 * TimeStamp.java
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.GregorianCalendar;

import org.matsim.utils.vis.kml.KMLWriter.XMLNS;

/**
 * For documentation, refer to
 * <a href="http://code.google.com/apis/kml/documentation/kmlreference.html#timestamp">
 * http://code.google.com/apis/kml/documentation/kmlreference.html#timestamp</a>
 * 
 * @author dgrether, meisterk, mrieser
 * @deprecated For working with KML files, please use the library kml-2.2-jaxb-2.1.7.jar. 
 * See ch.ethz.ivt.KMLDemo in that library for examples of usage.
 *
 */
public class TimeStamp extends TimePrimitive {

	private GregorianCalendar when;

	/**
	 * Constructs a <code>TimeStamp</code> from a date.
	 * 
	 * @param when
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#when">
	 * when</a> attribute of the new time stamp.
	 */
	public TimeStamp(GregorianCalendar when) {
		super();
		this.when = when;
	}

	@Override
	protected void writeObject(BufferedWriter out, XMLNS version, int offset,
			String offsetString) throws IOException {
		
		out.write(Object.getOffset(offset, offsetString));
		out.write("<TimeStamp>");
		out.newLine();
		out.write(Object.getOffset(offset + 1, offsetString));
		out.write("<when>" + getKMLDateFormat(when) + "</when>");
		out.newLine();
		out.write(Object.getOffset(offset, offsetString));
		out.write("</TimeStamp>");
		out.newLine();
		
	}

}
