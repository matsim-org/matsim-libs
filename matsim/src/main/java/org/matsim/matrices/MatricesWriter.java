/* *********************************************************************** *
 * project: org.matsim.*
 * MatricesWriter.java
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.io.MatsimXmlWriter;

public class MatricesWriter extends MatsimXmlWriter implements MatsimWriter {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private MatricesWriterHandler handler = null;
	private final Matrices matrices;
	private String dtd;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public MatricesWriter(final Matrices matrices) {
		super();
		this.matrices = matrices;
		// always write out in newest version, currently v1
		this.dtd = "http://matsim.org/files/dtd/matrices_v1.dtd";
		this.handler = new MatricesWriterHandlerImplV1();
	}

	//////////////////////////////////////////////////////////////////////
	// write methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final void write(final String filename) {
		try {
			openFile(filename);
			writeXmlHead();
			writeDoctype("matrices", this.dtd);
			this.handler.startMatrices(this.matrices, this.writer);
			this.handler.writeSeparator(this.writer);
			Iterator<Matrix> m_it = this.matrices.getMatrices().values().iterator();
			while (m_it.hasNext()) {
				Matrix m = m_it.next();
				this.handler.startMatrix(m, this.writer);
				Iterator<ArrayList<Entry>> eal_it = m.getFromLocations().values().iterator();
				while (eal_it.hasNext()) {
					ArrayList<Entry> eal = eal_it.next();
					Iterator<Entry> e_it = eal.iterator();
					while (e_it.hasNext()) {
						Entry e = e_it.next();
						this.handler.startEntry(e, this.writer);
						this.handler.endEntry(this.writer);
					}
				}
				this.handler.endMatrix(this.writer);
				this.handler.writeSeparator(this.writer);
				this.writer.flush();
			}
			this.handler.endMatrices(this.writer);
			close();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return super.toString();
	}
}
