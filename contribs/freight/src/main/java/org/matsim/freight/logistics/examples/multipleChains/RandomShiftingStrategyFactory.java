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
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
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
 *  This strategy removes **one** randomly selected shipment from the logistic chain it was assigned to and reassign it to another chain.
 *  This strategy allows to slowly change the plans and therefore follow the iterative learning process.
 *  But it is i) slow, because it needs a lot of iterations and ii) has a high chance to get stuck in a local optimum.
 *  @author nrichter (during his master thesis @VSP)
 */
class RandomShiftingStrategyFactory {

    private static Random random = null;

    //This is ok so as long as it is **non-public**.
    //Before making it public, it should be configurable either via config or Injection.
    //KMT, KN (Jan'24)
    RandomShiftingStrategyFactory() {}  // class contains only static methods; do not instantiate.

    static GenericPlanStrategy<LSPPlan, LSP> createStrategy() {

        MatsimRandom.reset();
        random = MatsimRandom.getLocalInstance();

        GenericPlanStrategyImpl<LSPPlan, LSP> strategy = new GenericPlanStrategyImpl<>(new ExpBetaPlanSelector<>(new ScoringConfigGroup()));
        GenericPlanStrategyModule<LSPPlan> randomModule = new GenericPlanStrategyModule<>() {

            @Override
            public void prepareReplanning(ReplanningContext replanningContext) {}

            @Override
            public void handlePlan(LSPPlan lspPlan) {

                // Shifting lspShipments only makes sense for multiple chains
                if (lspPlan.getLogisticChains().size() < 2) return;

                LSP lsp = lspPlan.getLSP();

                // Make a new list of lspShipments and pick a random lspShipment from it
                List<LspShipment> lspShipments = new ArrayList<>(lsp.getLspShipments());
                int shipmentIndex = random.nextInt(lsp.getLspShipments().size());
                LspShipment lspShipment = lspShipments.get(shipmentIndex);

                // Find and remove the random lspShipment from its current logistic chain
                LogisticChain sourceLogisticChain = null;
                for (LogisticChain logisticChain : lsp.getSelectedPlan().getLogisticChains()) {
                    if (logisticChain.getLspShipmentIds().remove(lspShipment.getId())) {
                        sourceLogisticChain = logisticChain;
                        break;
                    }
                }

                // Find a new logistic chain for the lspShipment
                // Ensure that the chain selected is not the same as the one it was removed from
                int chainIndex;
                LogisticChain targetLogisticChain = null;
                do {
                    chainIndex = random.nextInt(lsp.getSelectedPlan().getLogisticChains().size());
                    Iterator<LogisticChain> iterator = lsp.getSelectedPlan().getLogisticChains().iterator();
                    for (int i = 0; iterator.hasNext(); i++) {
                        targetLogisticChain = iterator.next();
                        if (i == chainIndex) {
                            break;
                        }
                    }
                } while (targetLogisticChain == sourceLogisticChain);

                // Add the lspShipment to the new logistic chain
                assert targetLogisticChain != null;
                targetLogisticChain.addShipmentToChain(lspShipment);
            }

            @Override
            public void finishReplanning() {}
        };

        strategy.addStrategyModule(randomModule);
        return strategy;
    }
}
