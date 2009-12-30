/* *********************************************************************** *
 * project: org.matsim.*
 * AgentContentmentComparator.java
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

package org.matsim.withinday.contentment;

import java.io.Serializable;
import java.util.Comparator;

import org.matsim.withinday.WithindayAgent;

/**
 * Compares two WithindayAgent instances by the return value of their getReplanningNeed() method.
 * Note: this comparator imposes orderings that are inconsistent with equals.
 * 
 * @author dgrether
 */
public class AgentContentmentComparator implements Comparator<WithindayAgent>, Serializable {

	private static final long serialVersionUID = 1L;

	public int compare(WithindayAgent a1, WithindayAgent a2) {
		if (a1.getReplanningNeed() > a2.getReplanningNeed()) {
			return 1;
		}
		if (a1.getReplanningNeed() < a2.getReplanningNeed()) {
			return -1;
		}
		return 0;
	}

}
