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

import org.matsim.gbl.Gbl;
import org.matsim.utils.io.IOUtils;
import org.matsim.writer.Writer;

public class MatricesWriter extends Writer {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private MatricesWriterHandler handler = null;
	private final Matrices matrices;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public MatricesWriter(final Matrices matrices) {
		super();
		this.matrices = matrices;
		this.outfile = Gbl.getConfig().matrices().getOutputFile();
		// always write out in newest version, currently v1
		this.dtd = "http://matsim.org/files/dtd/matrices_v1.dtd";
		this.handler = new MatricesWriterHandlerImplV1();
	}

	//////////////////////////////////////////////////////////////////////
	// write methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final void write() {
		try {
			this.out = IOUtils.getBufferedWriter(this.outfile);
			writeDtdHeader("matrices");
			this.out.flush();
			this.handler.startMatrices(this.matrices, this.out);
			this.handler.writeSeparator(this.out);
			Iterator<Matrix> m_it = this.matrices.getMatrices().values().iterator();
			while (m_it.hasNext()) {
				Matrix m = m_it.next();
				this.handler.startMatrix(m, this.out);
				Iterator<ArrayList<Entry>> eal_it = m.getFromLocations().values().iterator();
				while (eal_it.hasNext()) {
					ArrayList<Entry> eal = eal_it.next();
					Iterator<Entry> e_it = eal.iterator();
					while (e_it.hasNext()) {
						Entry e = e_it.next();
						this.handler.startEntry(e, this.out);
						this.handler.endEntry(this.out);
					}
				}
				this.handler.endMatrix(this.out);
				this.handler.writeSeparator(this.out);
				this.out.flush();
			}
			this.handler.endMatrices(this.out);
			this.out.flush();
			this.out.close();
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	public final void writeFile(final String filename) {
		this.outfile = filename;
		write();
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return super.toString();
	}
}
