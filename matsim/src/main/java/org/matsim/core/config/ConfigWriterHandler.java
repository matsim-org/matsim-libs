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

package org.matsim.core.config;

import java.io.BufferedWriter;

/*package*/ abstract class ConfigWriterHandler {

	abstract void startConfig(final Config config, final BufferedWriter out);
	
	abstract void endConfig(final BufferedWriter out);
	
	abstract void writeModule(final ConfigGroup module, final BufferedWriter out);
	
	abstract void writeSeparator(final BufferedWriter out);
	
	/**
	 * Sets the string to be used as newline separator.
	 * The idea behind this is that by default, "\n" should be used,
	 * so that files generated on different OSes can be compared by checksum.
	 * Using the System property (see <code>System.getProperty("line.separator");</code>) 
	 * may however be necessary for proper display
	 * of the config dump in the console...
	 *
	 * @param newline the newline separator
	 * @return the former newline separator
	 */
	abstract String setNewline(final String newline);
}
