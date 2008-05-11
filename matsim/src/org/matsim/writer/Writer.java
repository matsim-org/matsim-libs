/* *********************************************************************** *
 * project: org.matsim.*
 * Writer.java
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

package org.matsim.writer;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.gbl.Gbl;

public abstract class Writer {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	protected String outfile;
	protected String dtd;
	protected BufferedWriter out = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Writer() {
	}

	//////////////////////////////////////////////////////////////////////
	// write methods
	//////////////////////////////////////////////////////////////////////

	public abstract void write();

	protected void writeHeader(final String root_elem) {
		try {
			this.out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			this.out.write("<!DOCTYPE " + root_elem + " SYSTEM \"" + this.dtd + "\">\n\n");
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public BufferedWriter getWriter() {
		return this.out;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public String toString() {
		return "[outfile=" + this.outfile + "]" +
				"[dtd=" + this.dtd + "]" +
				"[out=" + this.out + "]";
	}
}
