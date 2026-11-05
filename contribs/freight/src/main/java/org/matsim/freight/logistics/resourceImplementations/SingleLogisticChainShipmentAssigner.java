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

package org.matsim.freight.logistics.resourceImplementations;

import org.matsim.core.gbl.Gbl;
import org.matsim.freight.logistics.InitialShipmentAssigner;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LogisticChain;
import org.matsim.freight.logistics.shipment.LspShipment;

/**
 * A simple {@link InitialShipmentAssigner} that assumes an {@link LSPPlan} with exactly one
 * {@link LogisticChain}, and assigns the given {@link LspShipment} to that chain.
 *
 * <p>Throws a {@link RuntimeException} if the plan does not contain exactly one logistic chain.
 */
class SingleLogisticChainShipmentAssigner implements InitialShipmentAssigner {

  SingleLogisticChainShipmentAssigner() {}

  @Override
  public void assignToPlan(LSPPlan lspPlan, LspShipment lspShipment) {
    Gbl.assertIf(lspPlan.getLogisticChains().size() == 1);
    LogisticChain singleSolution = lspPlan.getLogisticChains().iterator().next();
    singleSolution.addShipmentToChain(lspShipment);
  }
}
