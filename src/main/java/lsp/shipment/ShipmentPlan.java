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

package lsp.shipment;

import java.util.Map;

import org.matsim.api.core.v01.Id;

public interface ShipmentPlan {

	LSPShipment getShipment();

	Map<Id<ShipmentPlanElement>, ShipmentPlanElement> getPlanElements();

	void addPlanElement( Id<ShipmentPlanElement> id, ShipmentPlanElement element );
	
	ShipmentPlanElement getMostRecentEntry();
	
	void clear();
	
}
