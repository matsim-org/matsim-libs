/* *********************************************************************** *
 * project: org.matsim.*
 * ActivitySpaceBean.java
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

package playground.jhackney.activitySpaces;

import org.matsim.core.gbl.Gbl;

public class ActivitySpaceBean extends ActivitySpace {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ActivitySpaceBean(final String act_type) {
		this.act_type = act_type;
		this.params.put("x", null); // pre-defined params
		this.params.put("y", null);
		this.params.put("theta", null);
		this.params.put("a", null);
		this.params.put("b", null);
		this.params.put("cover", null);
	}

	//////////////////////////////////////////////////////////////////////
	// add methods
	//////////////////////////////////////////////////////////////////////

	@Override
	protected void addParam(final String name, final Double value) {
		double v = value.doubleValue();
		if (!this.params.containsKey(name)) {
			Gbl.errorMsg("[name="+name+" is not allowed]");
		}
		if (name.equals("theta")) {
			if ((v <= -Math.PI/2.0) || (v > Math.PI/2.0)) {
				Gbl.errorMsg("[name="+name+",value="+v+" is not element of ]-pi,pi].]");
			}
		}
		if (name.equals("cover")) {
			if (v <= 0.0) {
				Gbl.errorMsg("[name="+name+",value="+v+" is <= zero.]");
			}
		}
		// TODO: more checks needed? balmermi
		this.params.put(name.intern(), value);
	}

	@Override
	public void addParam(final String name, final String value) {
		Double val = Double.valueOf(value);
		this.addParam(name, val);
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////
}
