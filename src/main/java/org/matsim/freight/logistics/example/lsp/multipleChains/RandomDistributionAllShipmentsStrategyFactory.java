package org.matsim.freight.logistics.example.lsp.multipleChains;

import java.util.ArrayList;
import java.util.List;
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
import org.matsim.freight.logistics.shipment.LSPShipment;

final class RandomDistributionAllShipmentsStrategyFactory {

  private
  RandomDistributionAllShipmentsStrategyFactory() {} // class contains only static methods; do not
                                                     // instantiate

  static GenericPlanStrategy<LSPPlan, LSP> createStrategy() {

    GenericPlanStrategyImpl<LSPPlan, LSP> strategy =
        new GenericPlanStrategyImpl<>(new ExpBetaPlanSelector<>(new ScoringConfigGroup()));
    GenericPlanStrategyModule<LSPPlan> randomModule =
        new GenericPlanStrategyModule<>() {

          @Override
          public void prepareReplanning(ReplanningContext replanningContext) {}

          @Override
          public void handlePlan(LSPPlan lspPlan) {

            //				Shifting shipments only makes sense for multiple chains
            if (lspPlan.getLogisticChains().size() < 2) return;

            for (LogisticChain logisticChain : lspPlan.getLogisticChains()) {
              logisticChain.getShipmentIds().clear();
            }

            LSP lsp = lspPlan.getLSP();
            List<LogisticChain> logisticChains =
                new ArrayList<>(lsp.getSelectedPlan().getLogisticChains());

            for (LSPShipment shipment : lsp.getShipments()) {
              int index = MatsimRandom.getRandom().nextInt(logisticChains.size());
              logisticChains.get(index).addShipmentToChain(shipment);
            }
          }

          @Override
          public void finishReplanning() {}
        };

    strategy.addStrategyModule(randomModule);
    return strategy;
  }
}
