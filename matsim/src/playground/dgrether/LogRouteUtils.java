/* *********************************************************************** *
 * project: org.matsim.*
 * LogRouteUtils.java
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

package playground.dgrether;

import java.util.ArrayList;

import org.matsim.network.Node;
import org.matsim.population.Route;


/**
 * @author dgrether
 *
 */
public class LogRouteUtils {

	public static String getNodeRoute(final Route r) {
		StringBuffer buffer = new StringBuffer();
		for (Node n : r.getRoute()) {
			buffer.append(n.getId().toString());
			buffer.append(" ");
		}
		return buffer.toString();
	}
	
	public static String getNodeRoute(final ArrayList<Node> l) {
		StringBuffer buffer = new StringBuffer();
		for (Node n : l) {
			buffer.append(n.getId().toString());
			buffer.append(" ");
		}
		return buffer.toString();
	}
}
