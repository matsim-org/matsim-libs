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
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.io.BufferedWriter;
import java.io.IOException;
/*package*/ class CountsWriterHandlerImplV1 implements CountsWriterHandler {
	private final CoordinateTransformation coordinateTransformation;

	CountsWriterHandlerImplV1(CoordinateTransformation coordinateTransformation) {
		this.coordinateTransformation = coordinateTransformation;
	}

	@Override
	public void startCounts(final Counts counts, final BufferedWriter out) throws IOException {
		out.write("<counts ");
		out.write("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
		out.write("xsi:noNamespaceSchemaLocation=\"http://matsim.org/files/dtd/counts_v1.xsd\"\n");

		if (counts.getName() != null) {
			out.write(" name=\"" + counts.getName() + "\"");
		} else {
			out.write(" name=\"\"");
		}
		if (counts.getDescription() != null) {
			out.write(" desc=\"" + counts.getDescription() + "\"");
		}
		out.write(" year=\"" + counts.getYear() + "\" ");
		out.write(" > \n");
	}

	@Override
	public void endCounts(final BufferedWriter out) throws IOException {
		out.write("</counts>\n");
	}

	@Override
	public void startCount(final Count count, final BufferedWriter out) throws IOException {
		out.write("\t<count");
		out.write(" loc_id=\"" + count.getId() + "\"");
		out.write(" cs_id=\"" + count.getCsLabel() + "\"");
		if (count.getCoord() != null) {
			final Coord coord = coordinateTransformation.transform( count.getCoord() );
			out.write(" x=\"" + coord.getX() + "\"");
			out.write(" y=\"" + coord.getY() + "\"");
		}
		out.write(">\n");
	}

	@Override
	public void endCount(final BufferedWriter out) throws IOException {
		out.write("\t</count>\n\n");
	}

	@Override
	public void startVolume(final Volume volume, final BufferedWriter out) throws IOException {
		out.write("\t\t<volume");
		out.write(" h=\"" + volume.getHourOfDayStartingWithOne() + "\"");
		out.write(" val=\"" + volume.getValue() + "\"");
		out.write(" />\n");
	}

	@Override
	public void endVolume(final BufferedWriter out) throws IOException {
	}

	@Override
	public void writeSeparator(final BufferedWriter out) throws IOException {
		out.write("<!-- ====================================================================== -->\n\n");
	}
}
