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

public class LspShipmentWithTime {
	// yyyyyy find better solution for this.  It is not so good to define an interface, and then immediately define a class that goes beyond it.
	// Maybe the time should be added to the interface?  However, I don't even know what that time means (delivery time?  current time?).  kai,
	// jun'22

	private final LSPShipment shipment;
	private final double time;

	public LspShipmentWithTime(double time, LSPShipment shipment) {
		this.shipment = shipment;
		this.time = time;
	}

	public LSPShipment getShipment() {
		return shipment;
	}

	public double getTime() {
		return time;
	}

}
