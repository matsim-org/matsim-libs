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


/* *********************************************************************** *
 *                    org.matsim.demandmodeling.config                     *
 *                     ConfigWriterHandlerImplV1.java                      *
 *                          ---------------------                          *
 * copyright       : (C) 2006 by                                           *
 *                   Michael Balmer, Konrad Meister, Marcel Rieser,        *
 *                   David Strippgen, Kai Nagel, Kay W. Axhausen,          *
 *                   Technische Universitaet Berlin (TU-Berlin) and        *
 *                   Swiss Federal Institute of Technology Zurich (ETHZ)   *
 * email           : balmermi at gmail dot com                             *
 *                 : rieser at gmail dot com                               *
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
import java.util.Iterator;
import java.util.Map;

public class ConfigWriterHandlerImplV1 implements ConfigWriterHandler {

	//////////////////////////////////////////////////////////////////////
	//
	// interface implementation
	//
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// <config ... > ... </config>
	//////////////////////////////////////////////////////////////////////

	public void startConfig(final Config config, final BufferedWriter out) throws IOException {
		out.write("<config>\n\n");
	}

	public void endConfig(final BufferedWriter out) throws IOException {
		out.write("</config>\n");
	}

//////////////////////////////////////////////////////////////////////
// <module ... > ... </module>
//////////////////////////////////////////////////////////////////////

	public void writeModule(final Module module, final BufferedWriter out) throws IOException {
		Map<String, String> params = module.getParams();

		out.write("\t<module");
		out.write(" name=\"" + module.getName() + "\" >\n");

		Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> entry = it.next();
			out.write("\t\t<param name=\"" + entry.getKey() + "\" value=\"" + entry.getValue() + "\" />\n");
		}
		out.write("\t</module>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <!-- ============ ... ========== -->
	//////////////////////////////////////////////////////////////////////

	public void writeSeparator(final BufferedWriter out) throws IOException {
		out.write("<!-- =================================================" +
							"===================== -->\n\n");
	}
}
