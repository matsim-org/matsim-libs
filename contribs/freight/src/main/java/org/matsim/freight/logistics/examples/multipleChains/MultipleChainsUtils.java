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

package org.matsim.freight.logistics.examples.multipleChains;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.matsim.api.core.v01.Id;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierShipment;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;

class MultipleChainsUtils {
  private MultipleChainsUtils() {}

  public static RandomLogisticChainShipmentAssigner createRandomLogisticChainShipmentAssigner() {
    return new RandomLogisticChainShipmentAssigner();
  }

  public static RoundRobinLogisticChainShipmentAssigner
      createRoundRobinLogisticChainShipmentAssigner() {
    return new RoundRobinLogisticChainShipmentAssigner();
  }

  public static PrimaryLogisticChainShipmentAssigner createPrimaryLogisticChainShipmentAssigner() {
    return new PrimaryLogisticChainShipmentAssigner();
  }

  public static Collection<LspShipment> createLSPShipmentsFromCarrierShipments(Carrier carrier) {
    List<LspShipment> shipmentList = new ArrayList<>();

    List<CarrierShipment> carrierShipments = carrier.getShipments().values().stream().toList();

    for (CarrierShipment shipment : carrierShipments) {
      LspShipmentUtils.LspShipmentBuilder builder =
          LspShipmentUtils.LspShipmentBuilder.newInstance(
              Id.create(shipment.getId().toString(), LspShipment.class));
        builder.setCapacityDemand(shipment.getCapacityDemand());
      builder.setFromLinkId(shipment.getPickupLinkId());
      builder.setToLinkId(shipment.getDeliveryLinkId());
		builder.setStartTimeWindow(shipment.getPickupStartingTimeWindow());
		builder.setEndTimeWindow(shipment.getDeliveryStartingTimeWindow());
      builder.setPickupServiceTime(shipment.getPickupDuration());
      builder.setDeliveryServiceTime(shipment.getDeliveryDuration());
      shipmentList.add(builder.build());
    }
    return shipmentList;
  }

  public enum LspPlanTypes {
    SINGLE_ONE_ECHELON_CHAIN("singleOneEchelonChain"),
    SINGLE_TWO_ECHELON_CHAIN("singleTwoEchelonChain"),
    MULTIPLE_ONE_ECHELON_CHAINS("multipleOneEchelonChains"),
    MULTIPLE_TWO_ECHELON_CHAINS("multipleTwoEchelonChains"),
    MULTIPLE_MIXED_ECHELON_CHAINS("multipleMixedEchelonChains");

    private static final Map<String, LspPlanTypes> stringToEnum =
        Stream.of(values()).collect(toMap(Object::toString, e -> e));
    private final String label;

    LspPlanTypes(String label) {
      this.label = label;
    }

    public static LspPlanTypes fromString(String label) {
      return stringToEnum.get(label);
    }

    @Override
    public String toString() {
      return label;
    }
  }
}
