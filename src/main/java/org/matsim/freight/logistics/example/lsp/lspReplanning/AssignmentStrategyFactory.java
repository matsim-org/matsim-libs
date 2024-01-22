package org.matsim.freight.logistics.example.lsp.lspReplanning;

import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.freight.logistics.LSP;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LogisticChain;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.freight.logistics.shipment.ShipmentUtils;

/**
 * @deprecated This class is a work-around. Please do not use this method for any new runs!
 *     <p>The plan-handling below was previously done in the LSPControlerListener during replanning.
 *     Since we (nrichter, kturner) are now running with one replanning strategies, which will allow
 *     to move shipments within the same (LSP) plan to a different logisticChain. As a consequence,
 *     the code below is not needed for us (starting from now). More than that, it is in part
 *     working against the new workflow, because the (re) assignment should **not** be done during
 *     replanning anymore.
 *     <p>The code here is used so that the old stuff from (tm). It keeps this old behaviour (and
 *     tests passing) with its own strategy, instead running it for everyone.
 *     <p>nrichter, kturner Jul'23
 */
@Deprecated
public class AssignmentStrategyFactory {

  public AssignmentStrategyFactory() {}

  public GenericPlanStrategy<LSPPlan, LSP> createStrategy() {

    GenericPlanStrategyImpl<LSPPlan, LSP> strategy =
        new GenericPlanStrategyImpl<>(new RandomPlanSelector<>());
    GenericPlanStrategyModule<LSPPlan> assignmentModule =
        new GenericPlanStrategyModule<>() {

          @Override
          public void prepareReplanning(ReplanningContext replanningContext) {}

          @Override
          public void handlePlan(LSPPlan lspPlan) {

            for (LogisticChain solution : lspPlan.getLogisticChains()) {
              solution.getShipmentIds().clear();
              for (LogisticChainElement element : solution.getLogisticChainElements()) {
                element.getIncomingShipments().clear();
                element.getOutgoingShipments().clear();
              }
            }

            for (LSPShipment shipment : lspPlan.getLSP().getShipments()) {
              ShipmentUtils.getOrCreateShipmentPlan(lspPlan, shipment.getId()).clear();
              shipment.getShipmentLog().clear();
              lspPlan.getInitialShipmentAssigner().assignToPlan(lspPlan, shipment);
            }
          }

          @Override
          public void finishReplanning() {}
        };

    strategy.addStrategyModule(assignmentModule);
    return strategy;
  }
}
