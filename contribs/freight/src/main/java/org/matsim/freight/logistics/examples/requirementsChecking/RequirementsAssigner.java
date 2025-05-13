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

package org.matsim.freight.logistics.examples.requirementsChecking;

import java.util.ArrayList;
import java.util.Collection;
import org.matsim.freight.logistics.InitialShipmentAssigner;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LogisticChain;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentRequirement;

class RequirementsAssigner implements InitialShipmentAssigner {

  private final Collection<LogisticChain> feasibleLogisticChains;

  public RequirementsAssigner() {
    this.feasibleLogisticChains = new ArrayList<>();
  }

  @Override
  public void assignToPlan(LSPPlan lspPlan, LspShipment lspShipment) {
    feasibleLogisticChains.clear();

    label:
    for (LogisticChain solution : lspPlan.getLogisticChains()) {
      for (LspShipmentRequirement requirement : lspShipment.getRequirements()) {
        if (!requirement.checkRequirement(solution)) {

          continue label;
        }
      }
      feasibleLogisticChains.add(solution);
    }
    LogisticChain chosenSolution = feasibleLogisticChains.iterator().next();
    chosenSolution.addShipmentToChain(lspShipment);
  }

}
