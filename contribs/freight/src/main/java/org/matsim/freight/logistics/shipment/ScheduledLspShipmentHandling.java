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

/**
 * A {@link LspShipmentPlanElement} describing a planned (pre-simulation) in-hub handling operation.
 * Contrasts with {@code LoggedLspShipmentHandling}, which is reconstructed from simulation events.
 */
class ScheduledLspShipmentHandling extends AbstractLspShipmentActivity {
	//"Handle" sounds a bit unclear in the German translation. Therefor we are breaking the unofficial naming convention here. KMT/KN jan'25

	ScheduledLspShipmentHandling(LspShipmentUtils.ScheduledShipmentHandleBuilder builder) {
		super(builder.startTime, builder.endTime, builder.element, builder.resourceId, builder.linkId);
	}

	@Override
	public ElementType getElementType() {
		return ElementType.HANDLING;
	}
}
