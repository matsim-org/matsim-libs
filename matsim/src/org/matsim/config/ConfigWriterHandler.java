/* *********************************************************************** *
 * project: org.matsim.*
 * ConfigWriterHandler.java
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

package org.matsim.config;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.writer.WriterHandler;

interface ConfigWriterHandler extends WriterHandler {

	//////////////////////////////////////////////////////////////////////
	// <config ... > ... </config>
	//////////////////////////////////////////////////////////////////////

	public void startConfig(final Config config, final BufferedWriter out)
			throws IOException;

	public void endConfig(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <module ... > ... </module>
	//////////////////////////////////////////////////////////////////////

	public void writeModule(final Module module, final BufferedWriter out)
			throws IOException;
}
