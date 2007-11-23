/* *********************************************************************** *
 * project: org.matsim.*
 * CountsWriterHandlerImplV1.java
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

package org.matsim.counts;
import java.io.BufferedWriter;
import java.io.IOException;
public class CountsWriterHandlerImplV1 implements CountsWriterHandler {
	// interface implementation
	//////////////////////////////////////////////////////////////////////
	// <counts ... > ... </counts>
	//////////////////////////////////////////////////////////////////////
	public void startCounts(final Counts counts, final BufferedWriter out) throws IOException {
		out.write("<counts ");
		out.write("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
		out.write("xsi:noNamespaceSchemaLocation=\"http://matsim.org/files/dtd/counts_v1.xsd\"\n");

		if (counts.getName() != null) {
			out.write(" name=\"" + counts.getName() + "\"");
		}
		if (counts.getDescription() != null) {
			out.write(" desc=\"" + counts.getDescription() + "\"");
		}
		out.write(" year=\"" + counts.getYear() + "\" ");
		if (counts.getLayer() != null) {
			out.write(" layer=\"" + counts.getLayer() + "\"> \n");
		}
	}
	public void endCounts(final BufferedWriter out) throws IOException {
		out.write("</counts>\n");
	}
	//////////////////////////////////////////////////////////////////////
	// <count ... > ... </count>
	//////////////////////////////////////////////////////////////////////
	public void startCount(final Count count, final BufferedWriter out) throws IOException {
		out.write("\t<count");
		out.write(" loc_id=\"" + count.getLocId() + "\"");
		out.write(" cs_id=\"" + count.getCsId() + "\"");
		out.write(">\n");
	}
	public void endCount(final BufferedWriter out) throws IOException {
		out.write("\t</count>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <volume ... />
	//////////////////////////////////////////////////////////////////////
	public void startVolume(final Volume volume, final BufferedWriter out) throws IOException {
		out.write("\t\t<volume");
		out.write(" h=\"" + volume.getHour() + "\"");
		out.write(" value=\"" + volume.getValue() + "\"");
		out.write(" />\n");
	}
	public void endVolume(final BufferedWriter out) throws IOException {
	}
	//////////////////////////////////////////////////////////////////////
	// <!-- ============ ... ========== -->
	//////////////////////////////////////////////////////////////////////
	public void writeSeparator(final BufferedWriter out) throws IOException {
		out.write("<!-- =================================================" +
							"===================== -->\n\n");
	}
}
