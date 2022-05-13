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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentComparator;

/* package-private */ class WaitingShipmentsImpl implements WaitingShipments {

		
	private final ArrayList<ShipmentWithTime> shipments;
	
	WaitingShipmentsImpl() {
		this.shipments = new ArrayList<>();
	}
	
	
	@Override
	public void addShipment(double time, LSPShipment shipment) {
		ShipmentWithTime tuple = new ShipmentWithTime(time, shipment);
		this.shipments.add(tuple);
		shipments.sort(new ShipmentComparator());
	}

	@Override
	public Collection <ShipmentWithTime> getSortedShipments() {
		shipments.sort(new ShipmentComparator());
		return shipments;
	}

	public void clear(){
		shipments.clear();
	}

	@Override
	public Collection<ShipmentWithTime> getShipments() {
		return shipments;
	}
		
}
