/* *********************************************************************** *
 * project: matsim
 * TeleportationArrivalTimeComparator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.ptproject.qsim.comparators;

import java.io.Serializable;
import java.util.Comparator;

import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.utils.collections.Tuple;

public class TeleportationArrivalTimeComparator implements Comparator<Tuple<Double, PersonAgent>>, Serializable {
	private static final long serialVersionUID = 1L;
	@Override
	public int compare(final Tuple<Double, PersonAgent> o1, final Tuple<Double, PersonAgent> o2) {
		int ret = o1.getFirst().compareTo(o2.getFirst()); // first compare time information
		if (ret == 0) {
			ret = o2.getSecond().getPerson().getId().compareTo(o1.getSecond().getPerson().getId()); // if they're equal, compare the Ids: the one with the larger Id should be first
		}
		return ret;
	}
}