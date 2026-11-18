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
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierVehicle;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils.CollectionCarrierResourceBuilder;

/**
 * The {@link LSPCarrierResource} responsible for the physical collection of shipments from the
 * logistic-chain elements on the demand side, backed by a {@link Carrier} from the freight contrib.
 */
/*package-private*/ class CollectionCarrierResource extends LSPDataObject<LSPResource>
    implements LSPCarrierResource {

  private final Carrier carrier;
  private final List<LogisticChainElement> clientElements;
  private final CollectionCarrierScheduler collectionScheduler;

  CollectionCarrierResource(CollectionCarrierResourceBuilder builder) {
    super(builder.id);
    this.collectionScheduler = builder.collectionScheduler;
    this.clientElements = builder.clientElements;
    this.carrier = builder.carrier;
  }

  /** @return the depot link where the collection carrier's vehicles start and end. */
  @Override
  public Id<Link> getStartLinkId() {
    Id<Link> depotLinkId = null;
    for (CarrierVehicle vehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
      if (depotLinkId == null || depotLinkId == vehicle.getLinkId()) {
        depotLinkId = vehicle.getLinkId();
      }
    }

    return depotLinkId;
  }

  /** @return the depot link where the collection carrier's vehicles start and end. */
  @Override
  public Id<Link> getEndLinkId() {
    Id<Link> depotLinkId = null;
    for (CarrierVehicle vehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
      if (depotLinkId == null || depotLinkId == vehicle.getLinkId()) {
        depotLinkId = vehicle.getLinkId();
      }
    }

    return depotLinkId;
  }

  /** @return the logistics-chain elements this resource is responsible for collecting. */
  @Override
  public Collection<LogisticChainElement> getClientElements() {
    return clientElements;
  }

  /**
   * Schedules the shipments assigned to this resource via the
   * {@link CollectionCarrierScheduler}.
   *
   * @param bufferTime the buffer time to use when scheduling
   * @param lspPlan the plan that carries the shipments to schedule
   */
  @Override
  public void schedule(int bufferTime, LSPPlan lspPlan) {
    collectionScheduler.scheduleLspShipments(lspPlan, this, bufferTime);
  }

  public Carrier getCarrier() {
    return carrier;
  }

}
