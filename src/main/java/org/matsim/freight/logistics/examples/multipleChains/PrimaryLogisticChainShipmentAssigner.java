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

import org.matsim.core.gbl.Gbl;
import org.matsim.freight.logistics.InitialShipmentAssigner;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LogisticChain;
import org.matsim.freight.logistics.shipment.LspShipment;

/**
 * The {@link LspShipment} is assigned to the first {@link LogisticChain}. In case of one chain the
 * shipment is assigned to that chain. If there are more chains, the shipment is assigned to the
 * first of all chains. Requirements: There must be at least one logisticChain in the plan
 */
class PrimaryLogisticChainShipmentAssigner implements InitialShipmentAssigner {


  public PrimaryLogisticChainShipmentAssigner() {}

  @Override
  public void assignToPlan(LSPPlan lspPlan, LspShipment lspShipment) {
    Gbl.assertIf(!lspPlan.getLogisticChains().isEmpty());
    LogisticChain firstLogisticChain = lspPlan.getLogisticChains().iterator().next();
    firstLogisticChain.addShipmentToChain(lspShipment);
  }
}
