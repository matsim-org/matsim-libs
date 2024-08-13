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

package org.matsim.freight.logistics.examples.lspReplanning;

import java.util.Collection;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;

@Deprecated
/*package-private*/ class TomorrowShipmentAssignerStrategyFactory {

  private final InitialShipmentAssigner assigner;

  /*package-private*/ TomorrowShipmentAssignerStrategyFactory(InitialShipmentAssigner assigner) {
    this.assigner = assigner;
  }

  /*package-private*/ GenericPlanStrategy<LSPPlan, LSP> createStrategy() {
    GenericPlanStrategyImpl<LSPPlan, LSP> strategy =
        new GenericPlanStrategyImpl<>(new BestPlanSelector<>());

    GenericPlanStrategyModule<LSPPlan> tomorrowModule =
        new GenericPlanStrategyModule<>() {

          @Override
          public void prepareReplanning(ReplanningContext replanningContext) {
            // TODO Auto-generated method stub

          }

          @Override
          public void handlePlan(LSPPlan plan) {
            plan.getLogisticChains().iterator().next().getLspShipmentIds().clear();
            plan.setInitialShipmentAssigner(assigner);
            //				LSP lsp = assigner.getLSP();
            LSP lsp = plan.getLSP();
            Collection<LspShipment> lspShipments = lsp.getLspShipments();
            for (LspShipment lspShipment : lspShipments) {
              assigner.assignToPlan(plan, lspShipment);
            }

            for (LogisticChain solution : plan.getLogisticChains()) {
              solution.getLspShipmentIds().clear();
              for (LogisticChainElement element : solution.getLogisticChainElements()) {
                element.getIncomingShipments().clear();
                element.getOutgoingShipments().clear();
              }
            }

            for (LspShipment lspShipment : plan.getLSP().getLspShipments()) {
              LspShipmentUtils.getOrCreateShipmentPlan(plan, lspShipment.getId()).clear();
              lspShipment.getShipmentLog().clear();
              plan.getInitialShipmentAssigner().assignToPlan(plan, lspShipment);
            }
          }

          @Override
          public void finishReplanning() {
            // TODO Auto-generated method stub

          }
        };

    strategy.addStrategyModule(tomorrowModule);
    return strategy;
  }
}
