/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.freight.logistics;

import org.matsim.api.core.v01.Id;
import org.matsim.freight.logistics.shipment.LspShipment;

/**
 * @author Kai Martins-Turner (kturner)
 */
public interface HasLspShipmentId {

  String ATTRIBUTE_LSP_SHIPMENT_ID = "lspShipmentId";

  Id<LspShipment> getLspShipmentId();
}
