/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.jtrrouter;

import java.util.*;

/**
 * @author michalm
 */
public class Flow {
	final int node;
	final int next;

	final int[] counts;

	final boolean isInFlow;
	final boolean isOutFlow;

	final List<Route> routes;// routes found

	public Flow(int node, int next, int[] counts, boolean isInFlow, boolean isOutFlow) {
		this.node = node;
		this.next = next;

		this.counts = counts;

		this.isInFlow = isInFlow;
		this.isOutFlow = isOutFlow;

		routes = new ArrayList<>();
	}
}
