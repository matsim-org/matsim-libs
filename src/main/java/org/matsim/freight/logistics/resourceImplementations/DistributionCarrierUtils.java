/*
  *********************************************************************** *
  * project: org.matsim.*
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       :  (C) 2024 by the members listed in the COPYING,       *
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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LogisticChainElement;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class DistributionCarrierUtils {
  public static DistributionCarrierScheduler createDefaultDistributionCarrierScheduler() {
    return new DistributionCarrierScheduler();
  }

  public static class DistributionCarrierResourceBuilder {

    final Id<LSPResource> id;
    final ArrayList<LogisticChainElement> clientElements;
    final Network network;
    Carrier carrier;
    Id<Link> locationLinkId;
    DistributionCarrierScheduler distributionHandler;

    private DistributionCarrierResourceBuilder(Carrier carrier, Network network) {
      this.id = Id.create(carrier.getId().toString(), LSPResource.class);
      ResourceImplementationUtils.setCarrierType(
          carrier, ResourceImplementationUtils.CARRIER_TYPE.distributionCarrier);
      this.carrier = carrier;
      this.clientElements = new ArrayList<>();
      this.network = network;
    }

    public static DistributionCarrierResourceBuilder newInstance(Carrier carrier, Network network) {
      return new DistributionCarrierResourceBuilder(carrier, network);
    }

    public DistributionCarrierResourceBuilder setLocationLinkId(Id<Link> locationLinkId) {
      this.locationLinkId = locationLinkId;
      return this;
    }

    public DistributionCarrierResourceBuilder setDistributionScheduler(
        DistributionCarrierScheduler distributionCarrierScheduler) {
      this.distributionHandler = distributionCarrierScheduler;
      return this;
    }

    public DistributionCarrierResource build() {
      return new DistributionCarrierResource(this);
    }
  }
}
