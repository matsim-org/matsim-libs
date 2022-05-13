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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;


//TODO: Unused? -> delete? //set to package-private in first step  KMT, Jun'20
/*package-private*/ class LSPShipments {

	private Map<Id<LSPShipment>, LSPShipment> lspShipments; 

	public LSPShipments(Collection<LSPShipment> lspShipments) {
		makeMap(lspShipments);
	}

	private void makeMap(Collection<LSPShipment> lspShipments) {
		for (LSPShipment l : lspShipments) {
			this.lspShipments.put(l.getId(), l);
		}
	}

	public LSPShipments() {
		this.lspShipments = new HashMap<>();
	}

	public  Map<Id<LSPShipment>, LSPShipment> getShipments() {
		return lspShipments;
	}

	public void addShipment(LSPShipment lspShipment) {
		if(!lspShipments.containsKey(lspShipment.getId())){
			lspShipments.put(lspShipment.getId(), lspShipment);
		}
		else {
			
		}
	}
}
