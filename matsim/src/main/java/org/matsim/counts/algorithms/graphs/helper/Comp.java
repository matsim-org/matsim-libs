/* *********************************************************************** *
 * project: org.matsim.*
 * Comp.java
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

package org.matsim.counts.algorithms.graphs.helper;

public class Comp {
	// make private of course
	private final double xval_;
	private final String url_;
	private final String tooltip_;
	
	public Comp(double xval, String url, String tooltip) {
		this.xval_=xval;
		this.url_=url;
		this.tooltip_=tooltip;
	}
	
	public double getXval() {
		return this.xval_;
	}
	public String getURL() {
		return this.url_;
	}
	public String getTooltip() {
		return this.tooltip_;
	}
}
