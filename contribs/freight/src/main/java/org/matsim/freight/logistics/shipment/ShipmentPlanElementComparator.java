/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.freight.logistics.shipment;

import java.util.Comparator;

/**
 * Orders {@link LspShipmentPlanElement}s for a shipment by time, first by start time and then by end
 * time. Used to determine which element should be added next to a shipment's plan.
 */
final class ShipmentPlanElementComparator implements Comparator<LspShipmentPlanElement> {

	/*package-private*/ ShipmentPlanElementComparator() {}

	/**
	 * @return a negative, zero, or positive integer as {@code o1} is earlier, equal to, or later than
	 *     {@code o2}
	 */

	@Override
	public int compare(LspShipmentPlanElement o1, LspShipmentPlanElement o2) {
		if (o1.getStartTime() > o2.getStartTime()) {
			return 1;
		}
		if (o1.getStartTime() < o2.getStartTime()) {
			return -1;
		}
		if (o1.getStartTime() == o2.getStartTime()) {
			if (o1.getEndTime() > o2.getEndTime()) {
				return 1;
			}
			if (o1.getEndTime() < o2.getEndTime()) {
				return -1;
			}
		}
		return 0;
	}
}
