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

import lsp.HasSimulationTrackers;
import lsp.LogisticChain;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.utils.objectattributes.attributable.Attributable;

import java.util.Collection;

/**
 * This is, for example, a shipment that DHL moves from A to B.  It may use multiple carriers to achieve that.
 * <p>
 * Questions/comments:<ul>
 * <li>Within more modern MATSim, we would probably prefer to have from and to in coordinates, not link IDs.</li>
 * </ul>
 */
public interface LSPShipment extends Identifiable<LSPShipment>, Attributable, HasSimulationTrackers<LSPShipment> {

	Id<Link> getFrom(); // same as in CarrierShipment

	Id<Link> getTo(); // same as in CarrierShipment

	TimeWindow getPickupTimeWindow(); // same as in CarrierShipment

	TimeWindow getDeliveryTimeWindow(); // same as in CarrierShipment

	int getSize(); // same as in CarrierShipment

	double getDeliveryServiceTime(); // same as in CarrierShipment

	double getPickupServiceTime(); // same as in CarrierShipment

	ShipmentPlan getShipmentPlan();

	ShipmentPlan getLog();

	Collection<Requirement> getRequirements();

}
