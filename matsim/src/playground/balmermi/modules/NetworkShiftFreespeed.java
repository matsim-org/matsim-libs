/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkDoubleLinks.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.balmermi.modules;

import org.apache.log4j.Logger;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.misc.Time;

public class NetworkShiftFreespeed {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final Logger log = Logger.getLogger(NetworkShiftFreespeed.class);
	private double shift = -10.0/3.6;
	private double lowerBorder = 20.0/3.6;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkShiftFreespeed() {
		this(-10.0/3.6,20.0/3.6);
	}

	public NetworkShiftFreespeed(final double shift, final double lowerBorder) {
		log.info("init "+this.getClass().getName()+" module...");
		this.shift = shift;
		this.lowerBorder = lowerBorder;
		log.info("  shifting speed with shift="+this.shift+" and lowerBorder="+this.lowerBorder+".");
		log.info("done. ("+this.getClass().getName()+")");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(NetworkLayer network) {
		log.info("running "+this.getClass().getName()+" module...");
		for (LinkImpl l : network.getLinks().values()) {
			double fs = l.getFreespeed(Time.UNDEFINED_TIME);
			if (fs > lowerBorder) { l.setFreespeed(fs+shift); }
		}
		log.info("done. ("+this.getClass().getName()+")");
	}
}
