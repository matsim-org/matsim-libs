/* *********************************************************************** *
 * project: org.matsim.*
 * BasicRoute.java
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

import java.util.ArrayList;

import org.matsim.interfaces.networks.basicNet.BasicNodeI;

public class BasicRoute <T extends BasicNodeI>{
	protected ArrayList<T> route = new ArrayList<T>();

	public ArrayList<T> getRoute() {
		return this.route;
	}

	/**
	 * sets the route from a given ArrayList of Nodes
	 * @param srcRoute an ArrayList containing nodes from the startNode to the endNode of the route, both start and end included
	 */
	public void setRoute (ArrayList<T> srcRoute) {
		if (srcRoute == null) {
			this.route.clear();
		} else {
			this.route = srcRoute;
		}
		this.route.trimToSize();
	}

}
