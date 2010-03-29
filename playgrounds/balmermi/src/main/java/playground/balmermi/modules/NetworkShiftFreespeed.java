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

public class NetworkShiftFreespeed {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final Logger log = Logger.getLogger(NetworkShiftFreespeed.class);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkShiftFreespeed() {
		log.info("init "+this.getClass().getName()+" module...");
		log.info("done. ("+this.getClass().getName()+")");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(NetworkLayer network) {
		log.info("running "+this.getClass().getName()+" module...");
		for (LinkImpl l : network.getLinks().values()) {
			double fs = l.getFreespeed();
			// set standard speeds
			if (fs < 10/3.6) { l.setFreespeed(5/3.6); }
			else if (fs < 40/3.6) { l.setFreespeed(30/3.6); }
			else if (fs < 60/3.6) { l.setFreespeed(50/3.6); }
			else if (fs < 70/3.6) { l.setFreespeed(60/3.6); }
			else if (fs < 100/3.6) { l.setFreespeed(80/3.6); }
			else if (fs < 120/3.6) { l.setFreespeed(100/3.6); }
			else { l.setFreespeed(120/3.6); }

			// reduce standard speeds by 10km/h except Major highways
			fs = l.getFreespeed();
			if ((!l.getType().equals("0-4110-0")) && (fs > 20/3.6)) { l.setFreespeed(fs-(10/3.6)); }
		}
		log.info("done. ("+this.getClass().getName()+")");
	}
}
