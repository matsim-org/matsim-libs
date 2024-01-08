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
public class MainRunCarrierUtils {
  public static MainRunCarrierScheduler createDefaultMainRunCarrierScheduler() {
    return new MainRunCarrierScheduler();
  }

  public static class MainRunCarrierResourceBuilder {

    private final Id<LSPResource> id;
    private final ArrayList<LogisticChainElement> clientElements;
    private final Network network;
    private Carrier carrier;
    private Id<Link> fromLinkId;
    private Id<Link> toLinkId;
    private MainRunCarrierScheduler mainRunScheduler;
    private ResourceImplementationUtils.VehicleReturn vehicleReturn;

    private MainRunCarrierResourceBuilder(Carrier carrier, Network network) {
      this.id = Id.create(carrier.getId().toString(), LSPResource.class);
      ResourceImplementationUtils.setCarrierType(
          carrier, ResourceImplementationUtils.CARRIER_TYPE.mainRunCarrier);
      this.carrier = carrier;
      this.clientElements = new ArrayList<>();
      this.network = network;
    }

    public static MainRunCarrierResourceBuilder newInstance(Carrier carrier, Network network) {
      return new MainRunCarrierResourceBuilder(carrier, network);
    }

    public MainRunCarrierResourceBuilder setMainRunCarrierScheduler(
        MainRunCarrierScheduler mainRunScheduler) {
      this.mainRunScheduler = mainRunScheduler;
      return this;
    }

    public MainRunCarrierResource build() {
      return new MainRunCarrierResource(this);
    }

    Id<LSPResource> getId() {
      return id;
    }

    Carrier getCarrier() {
      return carrier;
    }

    public MainRunCarrierResourceBuilder setCarrier(Carrier carrier) {
      ResourceImplementationUtils.setCarrierType(
          carrier, ResourceImplementationUtils.CARRIER_TYPE.mainRunCarrier);
      this.carrier = carrier;
      return this;
    }

    Id<Link> getFromLinkId() {
      return fromLinkId;
    }

    // --- Getter ---

    public MainRunCarrierResourceBuilder setFromLinkId(Id<Link> fromLinkId) {
      this.fromLinkId = fromLinkId;
      return this;
    }

    Id<Link> getToLinkId() {
      return toLinkId;
    }

    public MainRunCarrierResourceBuilder setToLinkId(Id<Link> toLinkId) {
      this.toLinkId = toLinkId;
      return this;
    }

    ArrayList<LogisticChainElement> getClientElements() {
      return clientElements;
    }

    MainRunCarrierScheduler getMainRunScheduler() {
      return mainRunScheduler;
    }

    Network getNetwork() {
      return network;
    }

    ResourceImplementationUtils.VehicleReturn getVehicleReturn() {
      return vehicleReturn;
    }

    public MainRunCarrierResourceBuilder setVehicleReturn(
        ResourceImplementationUtils.VehicleReturn vehicleReturn) {
      this.vehicleReturn = vehicleReturn;
      return this;
    }
  }
}
