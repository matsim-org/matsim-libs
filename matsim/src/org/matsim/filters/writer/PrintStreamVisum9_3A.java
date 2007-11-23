/* *********************************************************************** *
 * project: org.matsim.*
 * PrintStreamVisum9_3A.java
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

package org.matsim.filters.writer;

import java.io.DataOutputStream;
import java.io.IOException;

import org.matsim.filters.filter.finalFilters.FinalEventFilterA;

/**
 * @author ychen
 *
 */
public abstract class PrintStreamVisum9_3A implements PrintStreamVisum9_3I {
	/**
	 * out - an underly static DataOutputStream
	 */
	protected DataOutputStream out;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.matsim.playground.filters.writer.PrintStreamVisum9_3I#output(org.matsim.playground.filters.filter.finalFilters.FinalEventFilterI)
	 */
	public abstract void output(FinalEventFilterA fef);

	/**
	 * @Specified by: close in interface Closeable
	 * @throws IOException -
	 *             if an I/O error occurs.
	 */
	/*
	 * (non-Javadoc)
	 *
	 * @see java.io.Closeable#close()
	 */
	public void close() throws IOException {
		this.out.close();
	}
}
