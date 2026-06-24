/* *********************************************************************** *
 * project: org.matsim.*
 * MatricesWriterHandler.java
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

import java.io.Writer;
import java.io.IOException;

interface MatricesWriterHandler {

	//////////////////////////////////////////////////////////////////////
	// <matrices ... > ... </matrices>
	//////////////////////////////////////////////////////////////////////

	public void startMatrices(final Matrices matrices, final Writer out) throws IOException;

	public void endMatrices(final Writer out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <matrix ... > ... </matrix>
	//////////////////////////////////////////////////////////////////////

	public void startMatrix(final Matrix matrix, final Writer out) throws IOException;

	public void endMatrix(final Writer out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <entry ... />
	//////////////////////////////////////////////////////////////////////

	public void startEntry(final Entry entry, final Writer out) throws IOException;

	public void endEntry(final Writer out) throws IOException;
	
	public void writeSeparator(final Writer out) throws IOException;
}
