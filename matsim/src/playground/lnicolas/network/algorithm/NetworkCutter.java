/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkCutter.java
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

package playground.lnicolas.network.algorithm;

import java.util.Iterator;

import org.matsim.basic.v01.IdSet;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.network.algorithms.NetworkAlgorithm;
import org.matsim.world.Coord;

/**
 * @author lnicolas
 *
 */
public class NetworkCutter extends NetworkAlgorithm {

	double leftX;
	double upperY;
	double rightX;
	double lowerY;

	public NetworkCutter(double leftX, double upperY, double rightX, double lowerY) {
		this.leftX = leftX;
		this.upperY = upperY;
		this.rightX = rightX;
		this.lowerY = lowerY;
	}

	/* (non-Javadoc)
	 * @see org.matsim.demandmodeling.network.algorithms.NetworkAlgorithm#run(org.matsim.demandmodeling.network.NetworkLayer)
	 */
	@Override
	public void run(NetworkLayer network) {

		int roleIndex = network.requestNodeRole();

		markNodes(network, roleIndex);

		cutNetwork(network, roleIndex);
	}

	private void cutNetwork(NetworkLayer network, int roleIndex) {

		IdSet allNodesCopy = new IdSet();
		allNodesCopy.addAll(network.getNodes());
		Iterator it = allNodesCopy.iterator();
		while (it.hasNext()) {
			Node n = (Node)it.next();
			NetworkCutterRole r = getRole(n, roleIndex);

			if (r.isWithinTargetRectangle() == false) {
				network.removeNode(n);
			}
		}
	}

	private void markNodes(NetworkLayer network, int roleIndex) {
		Iterator it = network.getNodes().iterator();

		while (it.hasNext()) {
			Node n = (Node)it.next();
			Coord c = n.getCoord();
			NetworkCutterRole r = getRole(n, roleIndex);

			if (c.getX() < leftX
					|| c.getX() > rightX
					|| c.getY() < upperY
					|| c.getY() > lowerY) {
				r.setWithinTargetRectangle(false);
			} else {
				r.setWithinTargetRectangle(true);
			}
		}
	}

	/**
	 * Returns the role for the given Node. Creates a new Role if none exists yet.
	 * @param n The Node for which to create a role.
	 * @return The role for the given Node
	 */
	NetworkCutterRole getRole(Node n, int roleIndex) {
		NetworkCutterRole r = (NetworkCutterRole) n.getRole(roleIndex);
		if (null == r) {
			r = new NetworkCutterRole();
			n.setRole(roleIndex, r);
		}
		return r;
	}


	class NetworkCutterRole {
		boolean isWithinTargetRectangle = false;

		/**
		 * @return the isWithinTargetRectangle
		 */
		public boolean isWithinTargetRectangle() {
			return isWithinTargetRectangle;
		}

		/**
		 * @param isWithinTargetRectangle the isWithinTargetRectangle to set
		 */
		public void setWithinTargetRectangle(boolean isWithinTargetRectangle) {
			this.isWithinTargetRectangle = isWithinTargetRectangle;
		}
	}

}
