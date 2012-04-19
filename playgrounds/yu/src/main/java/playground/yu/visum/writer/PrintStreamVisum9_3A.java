/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.yu.visum.writer;

import java.io.DataOutputStream;
import java.io.IOException;

import playground.yu.visum.filter.finalFilters.FinalEventFilterA;

/**
 * @author ychen
 * 
 */
public abstract class PrintStreamVisum9_3A implements PrintStreamVisum9_3I {
	/**
	 * out - an underly static DataOutputStream
	 */
	protected DataOutputStream out;

	@Override
	public abstract void output(FinalEventFilterA fef);

	/**
	 * @Specified by: close in interface Closeable
	 * @throws IOException
	 *             - if an I/O error occurs.
	 */
	@Override
	public void close() throws IOException {
		out.close();
	}
}
