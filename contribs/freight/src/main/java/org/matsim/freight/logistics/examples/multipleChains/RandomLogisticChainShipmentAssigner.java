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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.freight.logistics.InitialShipmentAssigner;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LogisticChain;
import org.matsim.freight.logistics.shipment.LspShipment;

/**
 * The {@link LspShipment} is assigned randomly to a {@link LogisticChain}. The logistic chains of a
 * plan are collected in a list. The chain to which the shipment is to be assigned is selected by a
 * seeded random index. Requirements: There must be at least one logisticChain in the plan.
 */
class RandomLogisticChainShipmentAssigner implements InitialShipmentAssigner {

  private final Random random;

  RandomLogisticChainShipmentAssigner() {
    MatsimRandom.reset();
    this.random = MatsimRandom.getLocalInstance();
  }

  @Override
  public void assignToPlan(LSPPlan lspPlan, LspShipment lspShipment) {
    Gbl.assertIf(!lspPlan.getLogisticChains().isEmpty());
    List<LogisticChain> logisticChains = new ArrayList<>(lspPlan.getLogisticChains());
    int index = random.nextInt(logisticChains.size());
    LogisticChain logisticChain = logisticChains.get(index);
    logisticChain.addShipmentToChain(lspShipment);
  }
}
