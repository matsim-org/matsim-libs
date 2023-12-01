/* *********************************************************************** *
 * project: org.matsim.*
 * Volume.java
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
// import org.matsim.demandmodeling.gbl.Gbl;

/**
 * Old API to access data fron {@link Count}. This class is not needed when using new API via {@link MeasurementLocation}.
 */
public class Volume {
	private final int h_;
	private double val_;

	protected Volume(final int h, final double val) {

		/* no error checking needed as we use schema instead of dtd

		if ((h == -1)) {
			Gbl.errorMsg("[h="+h+", negative values are not allowed!]");
		}
		if ((val == -1)) {
			Gbl.errorMsg("[val="+val+", negative values are not allowed!]");
		}
		*/

		this.h_ = h;
		this.val_ = val;
	}

	public final void setValue(double val) {
		this.val_ = val;
	}

	public final int getHourOfDayStartingWithOne() {
		return this.h_;
	}
	public final double getValue() {
		return this.val_;
	}

	@Override
	public final String toString() {
		return "[" + this.h_ + "===" + this.val_ + "]";
	}
}
