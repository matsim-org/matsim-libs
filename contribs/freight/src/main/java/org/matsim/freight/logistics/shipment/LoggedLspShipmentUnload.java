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
 * A {@link LspShipmentPlanElement} recording a shipment unload that happened during the simulation,
 * as derived from events. Contrasts with {@code ScheduledLspShipmentUnload}, which describes a
 * planned (pre-simulation) unload.
 */
class LoggedLspShipmentUnload extends AbstractLspShipmentActivity {
	//Consider changing this Logged<...> ShipmentPlanElement to MATSim's experienced plans.
	//This would be closer to MATSim and makes clear that this is what happened in the simulation. kmt/kn jan'25

	LoggedLspShipmentUnload(LspShipmentUtils.LoggedShipmentUnloadBuilder builder) {
		super(builder.startTime, builder.endTime, builder.element, builder.resourceId, builder.linkId);
	}

	@Override
	public ElementType getElementType() {
		return ElementType.UNLOAD;
	}
}
