/* *********************************************************************** *
 * project: org.matsim.*
 * LaneMeterFromLinkEndComparator
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
package org.matsim.lanes.data.v20;

import java.io.Serializable;
import java.util.Comparator;

import org.matsim.core.api.internal.MatsimComparator;

/**
 * Comparator which implements a comparision function for the Lane.getStartsAtMeterFromLinkEnd()
 * attribute.
 * @author dgrether
 */
public class LaneData20MeterFromLinkEndComparator implements Comparator<Lane>, Serializable, MatsimComparator {

	private static final long serialVersionUID = 1L;

	@Override
	public int compare(Lane o1, Lane o2) {
    if (o1.getStartsAtMeterFromLinkEnd() < o2.getStartsAtMeterFromLinkEnd()) {
      return -1;
    } else if (o1.getStartsAtMeterFromLinkEnd() > o2.getStartsAtMeterFromLinkEnd()) {
      return 1;
    } else {
      return 0;
    }
	}
}