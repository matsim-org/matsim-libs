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

package org.matsim.freight.logistics.examples.multipleChains;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.freight.logistics.LSP;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LogisticChain;
import org.matsim.freight.logistics.shipment.LspShipment;

/**
 *  This strategy removes **all** shipments from the logistic chains and reassigns them.
 *  The reassignment is done in a round-robin fashion, so that in the hand all chains have the same number of shipments.
 *  It does not seem to be a very useful strategy in terms of going forward towards a (local) optimum, as long as it is the only one.
 *
 *  @author nrichter (during his master thesis @VSP)
 */
/*package-private*/ class RoundRobinDistributionAllShipmentsStrategyFactory {
  //This is ok so as long as it is **non-public**.
  //Before making it public, it should be configurable either via config or Injection.
  //KMT, KN (Jan'24)

  private
  RoundRobinDistributionAllShipmentsStrategyFactory() {} // class contains only static methods; do
                                                         // not instantiate

  /*package-private*/ static GenericPlanStrategy<LSPPlan, LSP> createStrategy() {
    GenericPlanStrategyImpl<LSPPlan, LSP> strategy =
        new GenericPlanStrategyImpl<>(new ExpBetaPlanSelector<>(new ScoringConfigGroup()));
    GenericPlanStrategyModule<LSPPlan> roundRobinModule =
        new GenericPlanStrategyModule<>() {

          @Override
          public void prepareReplanning(ReplanningContext replanningContext) {}

          @Override
          public void handlePlan(LSPPlan lspPlan) {

            // Shifting shipments only makes sense for multiple chains
            if (lspPlan.getLogisticChains().size() < 2) return;

            for (LogisticChain logisticChain : lspPlan.getLogisticChains()) {
              logisticChain.getLspShipmentIds().clear();
            }

            LSP lsp = lspPlan.getLSP();
            Map<LogisticChain, Integer> shipmentCountByChain = new LinkedHashMap<>();

            for (LspShipment lspShipment : lsp.getLspShipments()) {
              if (shipmentCountByChain.isEmpty()) {
                for (LogisticChain chain : lsp.getSelectedPlan().getLogisticChains()) {
                  shipmentCountByChain.put(chain, 0);
                }
              }
              LogisticChain minChain =
                  Collections.min(shipmentCountByChain.entrySet(), Map.Entry.comparingByValue())
                      .getKey();
              minChain.addShipmentToChain(lspShipment);
              shipmentCountByChain.merge(minChain, 1, Integer::sum);
            }
          }

          @Override
          public void finishReplanning() {}
        };

    strategy.addStrategyModule(roundRobinModule);
    return strategy;
  }
}
