/* *********************************************************************** *
 * project: org.matsim.*
 * BasicLeg.java
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

import org.apache.log4j.Logger;

public class BasicLegImpl implements BasicLeg {

	protected int num = Integer.MIN_VALUE;
	protected String mode;
	protected BasicRoute route = null;

	//////////////////////////////////////////////////////////////////////
	// getter methods
	//////////////////////////////////////////////////////////////////////

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicLeg#getNum()
	 */
	public final int getNum() {
		return this.num;
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicLeg#getMode()
	 */
	public final String getMode() {
		return this.mode;
	}

	// could be overwritten in higher classes for providing  BasicRoute derived
	// return values
	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicLeg#getRoute()
	 */
	public BasicRoute getRoute() {
		return this.route;
	}

	//////////////////////////////////////////////////////////////////////
	// setter methods
	//////////////////////////////////////////////////////////////////////

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicLeg#setNum(int)
	 */
	public final void setNum(int num) {
		this.num = num;
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicLeg#setMode(java.lang.String)
	 */
	public final void setMode(String mode) {
		if (MIVMODE.equalsIgnoreCase(mode))
			this.mode = MIVMODE;
		else if (CARMODE.equalsIgnoreCase(mode))
			this.mode = CARMODE;
		else if (RIDEMODE.equalsIgnoreCase(mode))
			this.mode = RIDEMODE;
		else if (MOTORBIKEMODE.equalsIgnoreCase(mode))
			this.mode = MOTORBIKEMODE;
		else if (PTMODE.equalsIgnoreCase(mode))
			this.mode = PTMODE;
		else if (TRAINMODE.equalsIgnoreCase(mode))
			this.mode = TRAINMODE;
		else if (BIKEMODE.equalsIgnoreCase(mode))
			this.mode = BIKEMODE;
		else if (WALKMODE.equalsIgnoreCase(mode))
			this.mode = WALKMODE;
		else {
			Logger.getLogger(BasicLegImpl.class).warn("Unknown Leg mode: " + mode);
			this.mode = mode.intern();
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicLeg#setRoute(org.matsim.basic.v01.BasicRouteImpl)
	 */
	public final void setRoute(BasicRoute route) {
		this.route = route;
	}

}
