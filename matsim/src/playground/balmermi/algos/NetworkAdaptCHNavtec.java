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

package playground.balmermi.algos;

import java.util.ArrayList;

import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.algorithms.NetworkAlgorithm;

public class NetworkAdaptCHNavtec extends NetworkAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkAdaptCHNavtec() {
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

		System.out.println("      adapt lanes and caps...");
		for (Link l : network.getLinks().values()) {
			int type = Integer.parseInt(l.getType());
			if ((type == 13) || (type == 16)) {
				System.out.println("        link id=" + l.getId() + "; type=" + l.getType() + "; lanes=" + l.getLanesAsInt() + " ==> lanes=3");
				l.setLanes(3);
			}
			if (type == 31) {
				System.out.println("        link id=" + l.getId() + "; type=" + l.getType() + "; lanes=" + l.getLanesAsInt() + " ==> lanes=1");
				l.setLanes(1);
			}
			if (l.getLanesAsInt() == 3) {
				System.out.println("        link id=" + l.getId() + "; lanes=" + l.getLanesAsInt() + " ==> lanes=2");
				l.setLanes(2);
			}
			int lanes = l.getLanesAsInt();
			if (lanes == 1) { l.setCapacity(2000.0); }      // Info from CH Norm
			else if (lanes == 2) { l.setCapacity(4000.0); } // ask bernard at ivt for more info
			else if (lanes == 3) { l.setCapacity(5800.0); }
			else { Gbl.errorMsg(l.toString() + " wrong number of lanes!!!"); }

			if (type == 90) {
				link90.add(l);
			}
		}
		System.out.println("      done.");

		System.out.println("      remove links with type = 90...");
		for (int i=0; i<link90.size(); i++) {
			Link l = link90.get(i);
			boolean removed = network.removeLink(l);
			if (!removed) {
				System.out.println("        link id=" + l.getId() + "; type=" + l.getType() + " could not be removed!!!");
			}
			else {
				System.out.println("        link id=" + l.getId() + "; type=" + l.getType() + " removed.");
			}
		}
		System.out.println("      done.");

		System.out.println("    done.");
	}
}
