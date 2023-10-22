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
import java.io.UncheckedIOException;
import java.util.Map;

/*package*/ class ConfigWriterHandlerImplV1 extends ConfigWriterHandler {

	private String newline = "\n";

	@Override
	public String setNewline(final String newline) {
		String former = this.newline;
		this.newline = newline;
		return former;
	}

	//////////////////////////////////////////////////////////////////////
	// <config ... > ... </config>
	//////////////////////////////////////////////////////////////////////

	@Override
	 void startConfig(final Config config, final BufferedWriter out) throws UncheckedIOException {
		try {
			out.write("<config>");
			out.write(this.newline);
			out.write(this.newline);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	 void endConfig(final BufferedWriter out) throws UncheckedIOException {
		try {
			out.write("</config>");
			out.write(this.newline);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

//////////////////////////////////////////////////////////////////////
// <module ... > ... </module>
//////////////////////////////////////////////////////////////////////

	@Override
	 void writeModule(final ConfigGroup module, final BufferedWriter out) throws UncheckedIOException {
		Map<String, String> params = module.getParams();
		Map<String, String> comments = module.getComments();

		try {
			out.write("\t<module");
			out.write(" name=\"" + module.getName() + "\" >");
			out.write(this.newline);

			boolean lastHadComment = false;

			for (Map.Entry<String, String> entry : params.entrySet()) {
				if (comments.get(entry.getKey()) != null) {
					out.write(this.newline);
					out.write( "\t\t<!-- " + comments.get(entry.getKey()) + " -->");
					out.write(this.newline);
					lastHadComment = true;
				} else {
					if (lastHadComment) {
						out.write(this.newline);
					}
					lastHadComment = false;
				}
				out.write("\t\t<param name=\"" + entry.getKey() + "\" value=\"" + entry.getValue() + "\" />");
				out.write(this.newline);
			}
			out.write("\t</module>");
			out.write(this.newline);
			out.write(this.newline);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// <!-- ============ ... ========== -->
	//////////////////////////////////////////////////////////////////////

	@Override
	 void writeSeparator(final BufferedWriter out) throws UncheckedIOException {
		try {
			out.write("<!-- ====================================================================== -->");
			out.write(this.newline);
			out.write(this.newline);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
