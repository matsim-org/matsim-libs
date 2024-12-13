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

import java.util.Collection;
import java.util.List;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.logistics.LSPDataObject;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils.TransshipmentHubBuilder;
import org.matsim.freight.logistics.shipment.LspShipment;

/**
 * {@link LSPResource} bei der die geplanten Tätigkeiten NICHT am Verkehr teilnehmen.
 *
 * <p>Thus, these activities are entered directly in the Schedule of the LSPShipments that pass
 * through the TranshipmentHub.
 *
 * <p>An entry is added to the schedule of the shipments that is an instance of
 * ScheduledShipmentHandle. There, the name of the Resource and the client element are entered, so
 * that the way that the {@link LspShipment} takes is
 * specified. In addition, the planned start and end time of the handling (i.e. cross-docking) of
 * the shipment is entered. In the example, cross-docking starts as soon as the considered
 * LSPShipment arrives at the {@link TransshipmentHubResource} and ends after a fixed and a size
 * dependent amount of time.
 *
 * <p>I (KMT, oct'22) have done this temporally public - including a "do not instantiate"
 * constructor , because I need it the class for an instanceOf check for a quick scoring of hubs.
 * This can be reverted, once hubs will appear in the MATSim events stream.
 */
public class TransshipmentHubResource extends LSPDataObject<LSPResource> implements LSPResource {

  private final Id<Link> locationLinkId;
  private final TransshipmentHubScheduler transshipmentHubScheduler;
  private final List<LogisticChainElement> clientElements;

  private
  TransshipmentHubResource() { // Do not instantiate. (removable once this class is package-private
                               // again) KMT oct'22
    super(null);
    this.locationLinkId = null;
    this.transshipmentHubScheduler = null;
    this.clientElements = null;
    throw new RuntimeException(
        "This should have never been called, because it is not planed for getting instantiated.");
  }

  TransshipmentHubResource(
      TransshipmentHubBuilder builder, Scenario scenario) {
    super(builder.getId());
    this.locationLinkId = builder.getLocationLinkId();
    this.transshipmentHubScheduler = builder.getTransshipmentHubScheduler();
    transshipmentHubScheduler.setTranshipmentHub(this);
    TransshipmentHubTourEndEventHandler eventHandler =
        new TransshipmentHubTourEndEventHandler(this, scenario);
    transshipmentHubScheduler.setTransshipmentHubTourEndEventHandler(eventHandler);
    this.clientElements = builder.getClientElements();
  }

  @Override
  public Id<Link> getStartLinkId() {
    return locationLinkId;
  }

  @Override
  public Id<Link> getEndLinkId() {
    return locationLinkId;
  }

  @Override
  public Collection<LogisticChainElement> getClientElements() {
    return clientElements;
  }

  @Override
  public void schedule(int bufferTime, LSPPlan lspPlan) {
    transshipmentHubScheduler.scheduleShipments(lspPlan, this, bufferTime);
  }

  public double getCapacityNeedFixed() {
    return transshipmentHubScheduler.getCapacityNeedFixed();
  }

  public double getCapacityNeedLinear() {
    return transshipmentHubScheduler.getCapacityNeedLinear();
  }
}
