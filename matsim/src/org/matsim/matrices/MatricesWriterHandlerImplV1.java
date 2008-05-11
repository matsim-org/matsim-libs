/* *********************************************************************** *
 * project: org.matsim.*
 * MatricesWriterHandlerImplV1.java
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

package org.matsim.matrices;

import java.io.BufferedWriter;
import java.io.IOException;

public class MatricesWriterHandlerImplV1 implements MatricesWriterHandler {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	//
	// interface implementation
	//
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// <matrices ... > ... </matrices>
	//////////////////////////////////////////////////////////////////////

	public void startMatrices(final Matrices matrices, final BufferedWriter out) throws IOException {
		out.write("<matrices");
		if (matrices.getName() != null) {
			out.write(" name=\"" + matrices.getName() + "\"");
		}
		out.write(">\n\n");
	}

	public void endMatrices(final BufferedWriter out) throws IOException {
		out.write("</matrices>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <matrix ... > ... </matrix>
	//////////////////////////////////////////////////////////////////////

	public void startMatrix(final Matrix matrix, final BufferedWriter out) throws IOException {
		out.write("\t<matrix");
		out.write(" id=\"" + matrix.getId() + "\"");
		out.write(" world_layer=\"" + matrix.getLayer().getType() + "\"");

		if (matrix.getDesc() != null) {
			out.write(" desc=\"" + matrix.getDesc() + "\"");
		}
		out.write(">\n");
	}

	public void endMatrix(final BufferedWriter out) throws IOException {
		out.write("\t</matrix>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <entry ... />
	//////////////////////////////////////////////////////////////////////

	public void startEntry(final Entry entry, final BufferedWriter out) throws IOException {
		out.write("\t\t<entry");
		out.write(" from_id=\"" + entry.getFromLocation().getId() + "\"");
		out.write(" to_id=\"" + entry.getToLocation().getId() + "\"");
		out.write(" value=\"" + entry.getValue() + "\"");
		out.write(" />\n");
	}

	public void endEntry(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <!-- ============ ... ========== -->
	//////////////////////////////////////////////////////////////////////

	public void writeSeparator(final BufferedWriter out) throws IOException {
		out.write("<!-- =================================================" +
							"===================== -->\n\n");
	}
}
