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

package org.matsim.freight.logistics.examples;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.resourceImplementations.TransshipmentHubResource;

/**
 * Example {@link LSPScorer} that aggregates the scores of the LSP's chosen elements. Carrier scores
 * are summed (see {@link #scoreLspCarriers()}), and hubs are charged fixed costs (see {@link
 * #scoreHub()}).
 *
 *  @author Kai Martins-Turner (kturner)
 */
public class MyLSPScorer implements LSPScorer {
  private final static Logger logger = LogManager.getLogger(MyLSPScorer.class);
  private double score = 0;
  private LSP lsp;

  /**
   * Resets the accumulated score before a new iteration.
   *
   * @param iteration the current planning iteration (ignored beyond the reset)
   */
  @Override
  public void reset(int iteration) {
    score = 0.;
  }

  /**
   * Computes the score of the currently selected plan by summing the carrier scores, subtracting
   * the hub fixed costs, and penalizing shipments that are missing from the plan.
   *
   * @return the aggregated score of the selected plan
   */
  @Override
  public double getScoreForCurrentPlan() {
    scoreLspCarriers();
    scoreHub();
    scoreMissingShipments();
    return score;
  }

  /** Sums the scores of the carriers used by every element of the selected plan. */
  private void scoreLspCarriers() {
    var lspPlan = lsp.getSelectedPlan();
    for (LogisticChain logisticChain : lspPlan.getLogisticChains()) {
      for (LogisticChainElement logisticChainElement : logisticChain.getLogisticChainElements()) {
        if (logisticChainElement.getResource() instanceof LSPCarrierResource carrierResource) {
          var carriersScore = carrierResource.getCarrier().getSelectedPlan().getScore();
          if (carriersScore != null) {
            score = score + carriersScore;
          }
        }
      }
    }
  }

  /**
   * Subtracts the fixed costs of any hub resource contained in the selected plan.
   *
   * <p>Hub resources are scored only through their fixed costs because they do not (yet) generate
   * their own events, so a direct per-hub scoring is not possible.
   * <p>This is somehow a quickfix, because the hubs do **not** have any own events yet. This needs
   * to be implemented later KMT oct'22
   */
  private void scoreHub() {
    var lspPlan = lsp.getSelectedPlan();
    for (LogisticChain logisticChain : lspPlan.getLogisticChains()) {
      for (LogisticChainElement logisticChainElement : logisticChain.getLogisticChainElements()) {
        if (logisticChainElement.getResource() instanceof TransshipmentHubResource hub) {
          score = score - LSPUtils.getFixedCost(hub);
        }
      }
    }
  }

  /**
   * Penalizes the score for shipments that are absent from the selected plan, e.g. because they
   * were lost during replanning. The penalty is proportional to the number of missing shipments.
   */
  private void scoreMissingShipments() {
    LSPPlan lspPlan = lsp.getSelectedPlan();
    int lspPlanShipmentCount =
        lspPlan.getLogisticChains().stream()
            .mapToInt(logisticChain -> logisticChain.getLspShipmentIds().size())
            .sum();
    int shipmentCountDifference = lsp.getLspShipments().size() - lspPlanShipmentCount;
    if (shipmentCountDifference > 0) {
      logger.error("LspPlan contains less shipments than LSP; shipments probably lost during replanning.");
      score -= 10000 * shipmentCountDifference;
    }
  }

  /**
   * Receives a back-pointer to the {@link LSP} that owns the plan being scored.
   *
   * @param pointer the embedding {@link LSP}, used to inspect the selected plan
   */
  @Override
  public void setEmbeddingContainer(LSP pointer) {
    this.lsp = pointer;
  }
}
