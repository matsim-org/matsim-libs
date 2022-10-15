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

package lsp;

import lsp.shipment.LSPShipment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/* package-private */ class WaitingShipmentsImpl implements WaitingShipments {

	private final List<ShipmentWithTime> shipments;

	WaitingShipmentsImpl() {
		this.shipments = new ArrayList<>();
	}


	@Override
	public void addShipment(double time, LSPShipment shipment) {
		ShipmentWithTime tuple = new ShipmentWithTime(time, shipment);
		this.shipments.add(tuple);
		shipments.sort(Comparator.comparingDouble(ShipmentWithTime::getTime));
	}

	@Override
	public Collection<ShipmentWithTime> getSortedShipments() {
		shipments.sort(Comparator.comparingDouble(ShipmentWithTime::getTime));
		return shipments;
	}

	public void clear() {
		shipments.clear();
	}

	@Override
	public Collection<ShipmentWithTime> getShipments() {
		return shipments;
	}

	@Override public String toString() {
		StringBuilder strb = new StringBuilder();
		strb.append("WaitingShipmentsImpl{")
				.append("No of Shipments= ").append(shipments.size());
		if (shipments.size() >0 ){
			strb.append("; ShipmentIds=");
			for (ShipmentWithTime shipment : getSortedShipments()) {
				strb.append("[")
						.append(shipment.getShipment().getId())
						.append("]");
			}
		}
		strb.append('}');
		return strb.toString();
	}
}
