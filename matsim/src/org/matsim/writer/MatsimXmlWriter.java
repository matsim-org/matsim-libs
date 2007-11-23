/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimXmlWriter.java
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

package org.matsim.writer;

import java.io.IOException;

/**
 * A simple abstract class to write XML files.
 *
 * @author mrieser
 */
public abstract class MatsimXmlWriter extends MatsimWriter {

	/** The default location where dtds are stored on the internet. */
	public static final String DEFAULT_DTD_LOCATION = "http://www.matsim.org/files/dtd/";
	
	/**
	 * Writes the standard xml 1.0 header to the output.
	 *
	 * @throws IOException
	 */
	protected void writeXmlHead() throws IOException {
		this.writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
	}

	/**
	 * Writes the doctype declaration to the output.
	 *
	 * @param rootTag The root tag of the written XML document.
	 * @param dtdUrl The location of the document type definition of this XML document.
	 * @throws IOException
	 */
	protected void writeDoctype(String rootTag, String dtdUrl) throws IOException {
		this.writer.write("<!DOCTYPE " + rootTag + " SYSTEM \"" + dtdUrl + "\">\n");
	}
	
}
