/* *********************************************************************** *
 * project: org.matsim.*
 * LinLogDiscretizer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.common.stats;

/**
 * A composition of a linear and a log discretizer. Bin indices are first
 * determined with the linear discretizer and then discretized with the log
 * discretizer.
 * 
 * @author illenberger
 * 
 */
public class LinLogDiscretizer extends DiscretizerComposite {

	/**
	 * Creates a new discretizer.
	 * 
	 * @param linearBinWidth
	 *            the bin width for the linear discretizer
	 * @param base
	 *            the base for the log discretizer
	 */
	public LinLogDiscretizer(double linearBinWidth, double base) {
		super(new LinearDiscretizer(linearBinWidth), new LogDiscretizer(base));
	}

}
