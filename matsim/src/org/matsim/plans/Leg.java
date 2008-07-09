/* *********************************************************************** *
 * project: org.matsim.*
 * Leg.java
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

package org.matsim.plans;

import org.matsim.basic.v01.BasicLegImpl;
import org.matsim.basic.v01.BasicNodeImpl;
import org.matsim.basic.v01.BasicRouteImpl;
import org.matsim.utils.misc.Time;

public class Leg extends BasicLegImpl /*implements Serializable*/{

//	private static final long serialVersionUID = 5123937717277263980L;

	public Leg(final int num, final String mode, final String depTime, final String travTime, final String arrTime) {
		this.num = num;
		if (this.num < 0) {
			throw new NumberFormatException("A Leg's num has to be an  integer >= 0.");
		}
		this.mode = mode.intern();
		if (depTime != null) {
			this.setDepTime(Time.parseTime(depTime));
		}
		if (travTime != null) {
			this.setTravTime(Time.parseTime(travTime));
		}
		if (arrTime != null) {
			this.setArrTime(Time.parseTime(arrTime));
		}
	}

	public Leg(final int num, final String mode, final double depTime, final double travTime, final double arrTime) {
		this.num = num;
		if ((this.num < 0) && (this.num != Integer.MIN_VALUE)) {
			throw new NumberFormatException("A Leg's num has to be an  integer >= 0.");
		}
		this.mode = mode.intern();
		this.setDepTime(depTime);
		this.setTravTime(travTime);
		this.setArrTime(arrTime);
	}

	/**
	 * Makes a deep copy of this leg, however only when the Leg has a route which is
	 * instance of Route or BasicRoute. Other route instances are not considered.
	 * @param leg
	 */
	public Leg(final Leg leg) {
		this.num = leg.num;
		this.mode = leg.mode;
		this.setDepTime(leg.getDepTime());
		this.setTravTime(leg.getTravTime());
		this.setArrTime(leg.getArrTime());
		if (leg.route instanceof Route) {
			this.route = new Route((Route) leg.route);
		}
		else {
			this.route = new BasicRouteImpl<BasicNodeImpl>();
			this.route.setRoute(leg.getRoute().getRoute());
		}

	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final Route createRoute(final String dist, final String time) {
		this.route = new Route(dist, time);
		return getRoute();
	}

	//////////////////////////////////////////////////////////////////////
	// remove methods
	//////////////////////////////////////////////////////////////////////

	protected final void removeRoute() {
		this.route = null;
	}

	@Override
	public Route getRoute() {
		return (Route) this.route;
	}

	@Override
	public final String toString() {
		return "[num=" + this.num + "]" +
				"[mode=" + this.mode + "]" +
				"[depTime=" + Time.writeTime(this.getDepTime()) + "]" +
				"[travTime=" + Time.writeTime(this.getTravTime()) + "]" +
				"[arrTime=" + Time.writeTime(this.getArrTime()) + "]" +
				"[route=" + this.route + "]";
	}

	/* seems the code below is nowhere really used, so I commented it out. Additionally,
	 * I think it doesn't work correctly, as it serializes some non-transient members 
	 * manually, so they are basically serialized twice.
	 * If nobody needs this code, I will delete it soon.   marcel/9jul2008
	 * TODO [MR] delete code
	 */
	// BasicLeg is not yet serializable, so we have to serialize it by hand
/*	private void writeObject(final ObjectOutputStream s) throws IOException
	{
	    // The standard non-transient fields.
	  s.defaultWriteObject();
	  s.writeInt(getNum());
	  s.writeObject(getMode());
	  s.writeObject(getRoute());
	}

	private void readObject(final ObjectInputStream s)
	  throws IOException, ClassNotFoundException
	{
	  // the `size' field.
	  s.defaultReadObject();
	  setNum(s.readInt());
	  setMode((String)s.readObject());
	  setRoute((Route)s.readObject());
	}
*/
}
