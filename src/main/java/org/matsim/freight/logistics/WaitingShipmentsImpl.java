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

package org.matsim.freight.logistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;

/* package-private */ class WaitingShipmentsImpl implements WaitingShipments {

  private final List<LspShipment> shipments;

  WaitingShipmentsImpl() {
    this.shipments = new ArrayList<>();
  }

  @Override
  public void addShipment(double time, LspShipment lspShipment) {
    LspShipmentUtils.setTimeOfLspShipment(lspShipment, time);
    this.shipments.add(lspShipment);
    shipments.sort(Comparator.comparingDouble(LspShipmentUtils::getTimeOfLspShipment));
  }

  @Override
  public Collection<LspShipment> getSortedLspShipments() {
    shipments.sort(Comparator.comparingDouble(LspShipmentUtils::getTimeOfLspShipment));
    return shipments;
  }

  public void clear() {
    shipments.clear();
  }

  @Override
  public Collection<LspShipment> getLspShipmentsWTime() {
    return shipments;
  }

  @Override
  public String toString() {
    StringBuilder strb = new StringBuilder();
    strb.append("WaitingShipmentsImpl{").append("No of Shipments= ").append(shipments.size());
    if (!shipments.isEmpty()) {
      strb.append("; ShipmentIds=");
      for (LspShipment shipment : getSortedLspShipments()) {
        strb.append("[").append(shipment.getId()).append("]");
      }
    }
    strb.append('}');
    return strb.toString();
  }
}
