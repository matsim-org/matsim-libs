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

package playground.michalm.jtrrouter.matsim;

import playground.michalm.jtrrouter.Flow;

/**
 * @author michalm
 */
public class MATSimFlow extends Flow {
	final int inLink;
	final int outLink;

	public MATSimFlow(int node, int inLink, int outLink, int next, int count) {
		super(node, next, new int[] { count }, inLink != -1, outLink != -1);

		this.inLink = inLink;
		this.outLink = outLink;
	}

	public int getInLink() {
		return inLink;
	}

	public int getOutLink() {
		return outLink;
	}
}
