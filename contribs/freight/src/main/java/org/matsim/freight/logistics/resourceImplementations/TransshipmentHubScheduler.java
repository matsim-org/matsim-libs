/*
  *********************************************************************** *
  * project: org.matsim.*
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       :  (C) 2022 by the members listed in the COPYING,       *
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
  * ***********************************************************************
 */

package org.matsim.freight.logistics.resourceImplementations;

import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LSPResourceScheduler;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils.TranshipmentHubSchedulerBuilder;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlan;
import org.matsim.freight.logistics.shipment.LspShipmentPlanElement;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;

/*package-private*/ class TransshipmentHubScheduler extends LSPResourceScheduler {

  final Logger log = LogManager.getLogger(TransshipmentHubScheduler.class);
  private final double capacityNeedLinear;
  private final double capacityNeedFixed;
  private TransshipmentHubResource transshipmentHubResource;
  private TransshipmentHubTourEndEventHandler eventHandler;

  TransshipmentHubScheduler(TranshipmentHubSchedulerBuilder builder) {
    this.lspShipmentsToSchedule = new ArrayList<>();
    this.capacityNeedLinear = builder.getCapacityNeedLinear();
    this.capacityNeedFixed = builder.getCapacityNeedFixed();
  }

  @Override
  protected void initializeValues(LSPResource resource) {
    this.transshipmentHubResource = (TransshipmentHubResource) resource;
  }

  @Override
  protected void scheduleResource() {
    for (LspShipment lspShipmentToBeAssigned : lspShipmentsToSchedule) {
      updateSchedule(lspShipmentToBeAssigned);
    }
  }

  @Override
  @Deprecated
  protected void updateShipments() {
    log.error("This method is not implemented. Nothing will happen here. ");
  }

  private void updateSchedule(LspShipment lspShipment) {
    addShipmentHandleElement(lspShipment);
    addShipmentToEventHandler(lspShipment);
  }

  private void addShipmentHandleElement(LspShipment lspShipment) {
    LspShipmentUtils.ScheduledShipmentHandleBuilder builder =
        LspShipmentUtils.ScheduledShipmentHandleBuilder.newInstance();
    builder.setStartTime(LspShipmentUtils.getTimeOfLspShipment(lspShipment));
    builder.setEndTime(LspShipmentUtils.getTimeOfLspShipment(lspShipment) + capacityNeedFixed + capacityNeedLinear * lspShipment.getSize());
    builder.setResourceId(transshipmentHubResource.getId());
    for (LogisticChainElement element : transshipmentHubResource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(lspShipment)) {
        builder.setLogisticsChainElement(element);
      }
    }
    LspShipmentPlanElement handle = builder.build();

    String idString =
        handle.getResourceId()
            + String.valueOf(handle.getLogisticChainElement().getId())
            + handle.getElementType();
    Id<LspShipmentPlanElement> id = Id.create(idString, LspShipmentPlanElement.class);
    LspShipmentUtils.getOrCreateShipmentPlan(super.lspPlan, lspShipment.getId())
        .addPlanElement(id, handle);
  }

  private void addShipmentToEventHandler(LspShipment lspShipment) {
    for (LogisticChainElement element : transshipmentHubResource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(lspShipment)) {
        LspShipmentPlan lspShipmentPlan =
            LspShipmentUtils.getOrCreateShipmentPlan(lspPlan, lspShipment.getId());
        eventHandler.addShipment(lspShipment, element, lspShipmentPlan);
        break;
      }
    }
  }

  public double getCapacityNeedLinear() {
    return capacityNeedLinear;
  }

  public double getCapacityNeedFixed() {
    return capacityNeedFixed;
  }

  public void setTranshipmentHub(TransshipmentHubResource transshipmentHubResource) {
    this.transshipmentHubResource = transshipmentHubResource;
  }

  public void setTransshipmentHubTourEndEventHandler(
      TransshipmentHubTourEndEventHandler eventHandler) {
    this.eventHandler = eventHandler;
  }
}
