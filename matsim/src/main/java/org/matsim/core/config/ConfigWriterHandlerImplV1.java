/* *********************************************************************** *
 * project: org.matsim.*
 * ConfigWriterHandlerImplV1.java
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
import java.io.IOException;
import java.util.Map;

/*package*/ class ConfigWriterHandlerImplV1 implements ConfigWriterHandler {

	private String newline = "\n";

	/**
	 * Sets the string to be used as newline separator (see <code>System.getProperty("line.separator");</code>)
	 *
	 * @param newline
	 */
	public void setNewline(final String newline) {
		this.newline = newline;
	}

	//////////////////////////////////////////////////////////////////////
	// <config ... > ... </config>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startConfig(final Config config, final BufferedWriter out) throws IOException {
		out.write("<config>");
		out.write(this.newline);
		out.write(this.newline);
	}

	@Override
	public void endConfig(final BufferedWriter out) throws IOException {
		out.write("</config>");
		out.write(this.newline);
	}

//////////////////////////////////////////////////////////////////////
// <module ... > ... </module>
//////////////////////////////////////////////////////////////////////

	@Override
	public void writeModule(final Module module, final BufferedWriter out) throws IOException {
		Map<String, String> params = module.getParams();
		Map<String, String> comments = module.getComments();

		out.write("\t<module");
		out.write(" name=\"" + module.getName() + "\" >");
		out.write(this.newline);

		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (comments.get(entry.getKey()) != null) {
				out.write(this.newline);
				out.write( "\t\t<!-- " + comments.get(entry.getKey()) + " -->");
				out.write(this.newline);
			}
			out.write("\t\t<param name=\"" + entry.getKey() + "\" value=\"" + entry.getValue() + "\" />");
			out.write(this.newline);
		}
		out.write("\t</module>");
		out.write(this.newline);
		out.write(this.newline);
	}

	//////////////////////////////////////////////////////////////////////
	// <!-- ============ ... ========== -->
	//////////////////////////////////////////////////////////////////////

	@Override
	public void writeSeparator(final BufferedWriter out) throws IOException {
		out.write("<!-- ====================================================================== -->");
		out.write(this.newline);
		out.write(this.newline);
	}
}
