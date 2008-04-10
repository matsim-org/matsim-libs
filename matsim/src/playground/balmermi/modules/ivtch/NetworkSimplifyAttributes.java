/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkAdaptCHNavtec.java
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

package playground.balmermi.modules.ivtch;

import java.util.ArrayList;

import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.algorithms.NetworkAlgorithm;
import org.matsim.utils.misc.Time;

public class NetworkSimplifyAttributes extends NetworkAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final double speed_subtract = 10.0;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkSimplifyAttributes() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(NetworkLayer network) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		ArrayList<Link> link90 = new ArrayList<Link>();

		for (Link l : network.getLinks().values()) {
			double speed = l.getFreespeed(Time.UNDEFINED_TIME)*3.6;
			if (speed <= 0.0) { Gbl.errorMsg("speed = " + speed + " not allowed!"); }
			else if (speed <= 21.0) { l.setFreespeed(10.0/3.6); }
			else if (speed <= 36.0) { l.setFreespeed(30.0/3.6); }
			else if (speed <= 61.0) { l.setFreespeed((50.0-speed_subtract)/3.6); }
			else if (speed <= 66.0) { l.setFreespeed((60.0-speed_subtract)/3.6); }
			else if (speed <= 81.0) { l.setFreespeed((80.0-speed_subtract)/3.6); }
			else if (speed <= 91.0) { l.setFreespeed((80.0-speed_subtract)/3.6); } // ausland
			else if (speed <= 101.0) { l.setFreespeed(100.0/3.6); }
			else if (speed <= 111.0) { l.setFreespeed(120.0/3.6); } // ausland
			else if (speed <= 121.0) { l.setFreespeed(120.0/3.6); }
			else { Gbl.errorMsg("speed = " + speed + " not allowed!"); }
		}
		System.out.println("    done.");
	}
}
