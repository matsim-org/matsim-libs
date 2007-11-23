/* *********************************************************************** *
 * project: org.matsim.*
 * BasicAct.java
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

package org.matsim.basic.v01;

import org.matsim.gbl.Gbl;

public class BasicAct {

	// TODO: should be private but needs refactoring in derived classes
	protected double endTime = Gbl.UNDEFINED_TIME;
	protected String type;
	protected BasicLink link = null;

	//////////////////////////////////////////////////////////////////////
	// getter methods
	//////////////////////////////////////////////////////////////////////

	public final double getEndTime() {
		return this.endTime;
	}

	public final String getType() {
		return this.type;
	}

	public BasicLink getLink() {
		return this.link;
	}

	//////////////////////////////////////////////////////////////////////
	// setter methods
	//////////////////////////////////////////////////////////////////////

	public final void setEndTime(final double endTime) {
		this.endTime = endTime;
	}

	public final void setType(final String type) {
		this.type = type.intern();
	}

	public final void setLink(final BasicLink link) {
		this.link = link;
	}

}
