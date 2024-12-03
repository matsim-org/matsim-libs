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

package org.matsim.freight.logistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.freight.logistics.shipment.LspShipment;

/* package-private */ class LSPImpl extends LSPDataObject<LSP> implements LSP {
  private static final Logger log = LogManager.getLogger(LSPImpl.class);

  private final Collection<LspShipment> lspShipments;
  private final ArrayList<LSPPlan> lspPlans;
  private final LogisticChainScheduler logisticChainScheduler;
  private final Collection<LSPResource> resources;
  private LSPPlan selectedPlan;
  private LSPScorer scorer;

  //	private LSPReplanner replanner;

  LSPImpl(LSPUtils.LSPBuilder builder) {
    super(builder.id);
    this.lspShipments = new ArrayList<>();
    this.lspPlans = new ArrayList<>();
    this.logisticChainScheduler = builder.logisticChainScheduler;
    this.logisticChainScheduler.setEmbeddingContainer(this);
    this.selectedPlan = builder.initialPlan;
    this.selectedPlan.setLSP(this);
    this.lspPlans.add(builder.initialPlan);
    this.resources = builder.resources;
  }

  public static LSPPlan copyPlan(LSPPlan plan2copy) {
    List<LogisticChain> newPlanChains = new ArrayList<>();
    for (LogisticChain initialPlanChain : plan2copy.getLogisticChains()) {
      LogisticChain newPlanChain =
          LSPUtils.LogisticChainBuilder.newInstance(initialPlanChain.getId()).build();
      newPlanChain.getLogisticChainElements().addAll(initialPlanChain.getLogisticChainElements());
      newPlanChain.getLspShipmentIds().addAll(initialPlanChain.getLspShipmentIds());
      newPlanChains.add(newPlanChain);
    }

    LSPPlan copiedPlan = LSPUtils.createLSPPlan();
    copiedPlan.setInitialShipmentAssigner(plan2copy.getInitialShipmentAssigner());
    copiedPlan.setLSP(plan2copy.getLSP());
    copiedPlan.setScore(plan2copy.getScore());
    copiedPlan.setType(plan2copy.getType());
    copiedPlan.getLogisticChains().addAll(newPlanChains);
    return copiedPlan;
  }

  /**
   * This is used from {@link LSPControllerListener} and not meant to be used from user code. Users
   * should bind {@link LSPScorerFactory}.
   */
  /* package-private */ void setScorer(LSPScorer scorer) {
    this.scorer = scorer;
    scorer.setEmbeddingContainer(this);
    this.addSimulationTracker(scorer);
  }

  @Override
  public void scheduleLogisticChains() {
    logisticChainScheduler.scheduleLogisticChain();
  }

  @Override
  public boolean addPlan(LSPPlan plan) {
    for (LogisticChain solution : plan.getLogisticChains()) {
      for (LogisticChainElement element : solution.getLogisticChainElements()) {
        if (!resources.contains(element.getResource())) {
          resources.add(element.getResource());
        }
      }
    }
    plan.setLSP(this);
    return lspPlans.add(plan);
  }

  @Override
  public LSPPlan createCopyOfSelectedPlanAndMakeSelected() {
    LSPPlan newPlan = LSPImpl.copyPlan(this.selectedPlan);
    this.setSelectedPlan(newPlan);
    return newPlan;
  }

  @Override
  public ArrayList<LSPPlan> getPlans() {
    return lspPlans;
  }

  @Override
  public LSPPlan getSelectedPlan() {
    return selectedPlan;
  }

  @Override
  public void setSelectedPlan(LSPPlan selectedPlan) {
    if (!lspPlans.contains(selectedPlan)) {
      lspPlans.add(selectedPlan);
    }
    this.selectedPlan = selectedPlan;
  }

  @Override
  public boolean removePlan(LSPPlan lspPlan) {
    if (lspPlans.contains(lspPlan)) {
      lspPlans.remove(lspPlan);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public Collection<LSPResource> getResources() {
    return resources;
  }

  public void scoreSelectedPlan() {
    if (this.scorer != null) {
      this.selectedPlan.setScore(scorer.getScoreForCurrentPlan());
    } else {
      throw new RuntimeException("trying to score the current LSP plan, but scorer is not set.");
    }
  }

  @Override
  public void assignShipmentToLSP(LspShipment lspShipment) {
    //		shipment.setLspId(this.getId()); // und rückweg dann auch darüber und dann
    // lsp.getselectedPlan.getShipment...
    lspShipments.add(lspShipment);
    for (LSPPlan lspPlan : lspPlans) {
      lspPlan.getInitialShipmentAssigner().assignToPlan(lspPlan, lspShipment);
    }
  }

  @Override
  public Collection<LspShipment> getLspShipments() {
    return this.lspShipments;
  }
}
