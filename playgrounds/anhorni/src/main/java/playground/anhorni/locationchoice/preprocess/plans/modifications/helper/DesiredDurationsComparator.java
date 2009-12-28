/* *********************************************************************** *
 * project: org.matsim.*
 * DesiredDurationsComparator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.preprocess.plans.modifications.helper;

import java.io.Serializable;
import java.util.Comparator;

public class DesiredDurationsComparator implements Comparator<DesiredDurationPerson>, Serializable {
	private static final long serialVersionUID = 1L;

	public int compare(final DesiredDurationPerson o1, final DesiredDurationPerson o2) {
		return Double.compare(o1.getDuration(), o2.getDuration());
	}
}
