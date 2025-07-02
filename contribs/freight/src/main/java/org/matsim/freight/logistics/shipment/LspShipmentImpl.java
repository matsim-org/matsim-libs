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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.TimeWindow;
import org.matsim.freight.logistics.LSPDataObject;

class LspShipmentImpl extends LSPDataObject<LspShipment> implements LspShipment {

  private final Id<Link> fromLinkId;
  private final Id<Link> toLinkId;
  private final TimeWindow startTimeWindow;
  private final TimeWindow endTimeWindow;
  private final int capacityDemand;
  private final double deliveryServiceTime;
  private final double pickupServiceTime;
  //	private final ShipmentPlan shipmentPlan;
  @Deprecated // This will be removed in the future and replaced by using the events. KMT, Mai'23
  private final LspShipmentPlan shipmentLog;
  private final List<LspShipmentRequirement> lspShipmentRequirements;

  //	private Id<LSP> lspId;

  LspShipmentImpl(LspShipmentUtils.LspShipmentBuilder builder) {
    super(builder.id);
    this.fromLinkId = builder.fromLinkId;
    this.toLinkId = builder.toLinkId;
    this.startTimeWindow = builder.startTimeWindow;
    this.endTimeWindow = builder.endTimeWindow;
    this.capacityDemand = builder.capacityDemand;
    this.deliveryServiceTime = builder.deliveryServiceTime;
    this.pickupServiceTime = builder.pickupServiceTime;
    //		this.shipmentPlan = new ShipmentPlanImpl(this.getId());
    this.shipmentLog = new LspShipmentPlanImpl(this.getId());
    this.lspShipmentRequirements = new ArrayList<>();
    this.lspShipmentRequirements.addAll(builder.lspShipmentRequirements);
  }

  @Override
  public Id<Link> getFrom() {
    return fromLinkId;
  }

  @Override
  public Id<Link> getTo() {
    return toLinkId;
  }

  @Override
  public TimeWindow getPickupTimeWindow() {
    return startTimeWindow;
  }

  @Override
  public TimeWindow getDeliveryTimeWindow() {
    return endTimeWindow;
  }

    @Deprecated // This will be removed in the future and replaced by using the events. KMT, Mai'23
	//Consider changing this ShipmentLog to MATSim's experienced plans.
	//This would be closer to MATSim and makes clear that this is what happened in the simulation. kmt/kn jan'25
  @Override
  public LspShipmentPlan getShipmentLog() {
    return shipmentLog;
  }

  @Override
  public int getSize() {
    return capacityDemand;
  }

  @Override
  public double getDeliveryServiceTime() {
    return deliveryServiceTime;
  }

  @Override
  public Collection<LspShipmentRequirement> getRequirements() {
    return lspShipmentRequirements;
  }

  @Override
  public double getPickupServiceTime() {
    return pickupServiceTime;
  }



    @Override
  public String toString() {
    return "LSPShipmentImpl{"
        + "Id="
        + getId()
        + "\t fromLinkId="
        + fromLinkId
        + "\t toLinkId="
        + toLinkId
        + "\t capacityDemand="
        + capacityDemand
        + "\t startTimeWindow="
        + startTimeWindow
        + "\t endTimeWindow="
        + endTimeWindow
        + "\t capacityDemand="
        + capacityDemand
        + "\t deliveryServiceTime="
        + deliveryServiceTime
        + "\t pickupServiceTime="
        + pickupServiceTime
        +
        //				"\t schedule=" + schedule +
        //				"\t log=" + log +
        //				"\t requirements=" + requirements +
        '}';
  }
}
