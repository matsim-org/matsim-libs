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
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;

public class NetworkAdaptCaps {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final Logger log = Logger.getLogger(NetworkAdaptCaps.class);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkAdaptCaps() {
		log.info("init "+this.getClass().getName()+" module...");
		log.info("done. ("+this.getClass().getName()+")");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(NetworkLayer network) {
		log.info("running "+this.getClass().getName()+" module...");
		for (Link l : network.getLinks().values()) {
			double cap = l.getCapacity();
			// set standard caps
			if (cap < 1000) { l.setCapacity(500); }
			else if (cap < 2000) { l.setCapacity(1000); }
			else if (cap < 4000) { l.setCapacity(2000); }
			else if (cap < 6000) { l.setCapacity(4000); }
			else if (cap < 8000) { l.setCapacity(6000); }
			else if (cap < 10000) { l.setCapacity(8000); }
			else { l.setCapacity(10000); }

			// move links with teleatlas type 3 and higher to the next lower cap class
			cap = l.getCapacity();
			String type = ((LinkImpl) l).getType();
			if (type.startsWith("3-") || type.startsWith("4-") ||
			    type.startsWith("5-") || type.startsWith("6-") ||
			    type.startsWith("7-")) {
				if (cap == 500) { ; }
				else if (cap == 1000) { l.setCapacity(500); }
				else if (cap == 2000) { l.setCapacity(1000); }
				else if (cap == 4000) { l.setCapacity(2000); }
				else if (cap == 6000) { l.setCapacity(4000); }
				else if (cap == 8000) { l.setCapacity(6000); }
				else if (cap == 10000) { l.setCapacity(8000); }
				else { throw new RuntimeException("capacity not known!"); }
			}
		}
		log.info("done. ("+this.getClass().getName()+")");
	}
}
