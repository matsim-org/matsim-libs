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
 * A {@link LspShipmentPlanElement} recording an in-hub handling operation that happened to a
 * shipment, as derived from the simulation events (as opposed to the pre-plan in
 * {@code ScheduledLspShipmentHandling}).
 */
/*package*/ class LoggedLspShipmentHandling extends AbstractLspShipmentActivity {
	//"Handle" sounds a bit unclear in the German translation. Therefor we are breaking the unofficial naming convention here. KMT/KN jan'25
	//Consider changing this Logged<...> ShipmentPlanElement to MATSim's experienced plans.
	//This would be clother to MATSim and makes clear that this is what happened in the simulation. kmt/kn jan'25

	LoggedLspShipmentHandling(LspShipmentUtils.LoggedShipmentHandleBuilder builder) {
		super(builder.startTime, builder.endTime, builder.element, builder.resourceId, builder.linkId);
	}

	@Override
	public ElementType getElementType() {
		return ElementType.HANDLING;
	}
}
