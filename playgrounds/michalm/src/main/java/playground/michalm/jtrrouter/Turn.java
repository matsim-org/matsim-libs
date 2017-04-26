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

/**
 * @author michalm
 */
public class Turn {
	// structure
	final int node;
	final int prev;
	final int[] next;
	final double[] probs;

	// algorithm
	boolean visited;

	public Turn(int node, int prev, int[] next, double[] probs) {
		this.node = node;
		this.prev = prev;
		this.next = next;
		this.probs = probs;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(prev).append(" -> ").append(node).append('\n');

		for (int i = 0; i < next.length; i++) {
			sb.append(next[i]).append(':').append(probs[i]).append('\n');
		}

		return sb.toString();
	}
}
