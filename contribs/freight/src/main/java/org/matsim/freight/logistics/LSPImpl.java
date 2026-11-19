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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;

/* package-private */ final class LSPImpl extends LSPDataObject<LSP> implements LSP {
  @SuppressWarnings("unused")
  private static final Logger log = LogManager.getLogger(LSPImpl.class);

  private final Collection<LspShipment> lspShipments;
  private final ArrayList<LSPPlan> lspPlans;
  private final LogisticChainScheduler logisticChainScheduler;
  private final Collection<LSPResource> resources;
  private LSPPlan selectedPlan;
  private LSPScorer scorer;


  LSPImpl(LSPUtils.LSPBuilder builder) {
    super(builder.id);
    this.lspShipments = new ArrayList<>();
    this.lspPlans = new ArrayList<>();
    this.resources = new ArrayList<>();
    this.logisticChainScheduler = builder.logisticChainScheduler;
    this.logisticChainScheduler.setEmbeddingContainer(this);
    // addPlan collects the resources of the plan, sets the LSP of the plan, and adds it to lspPlans.
    addPlan(builder.initialPlan);
    this.selectedPlan = builder.initialPlan;
  }

  /**
   * Creates a copy of the given plan, e.g. for replanning.
   *
   * <ul>
   *   <li>Copied: the {@link InitialShipmentAssigner} (same instance), score, type, and LSP of the plan.
   *   <li>Each {@link LogisticChain} is a new object with the same id, a new collection of the same
   *       shipment ids, and a copy of the attributes (same attribute values, new attribute map).
   *   <li>Shared, not copied: the {@link LogisticChainElement}s. Original and copy refer to the same
   *       element instances, i.e. the same structure (resources, predecessors, successors) and the same
   *       attributes and simulation trackers of the elements. A deep copy would register additional
   *       client elements with the resources each time a plan is copied (see the constructor of
   *       {@link LogisticChainElementImpl}), and there is no way to unregister them.
   *   <li>Not copied: the simulation trackers of the chains. A tracker has exactly one backpointer to its
   *       chain, so it cannot be shared between the chain of the original and the chain of the copy.
   *   <li>Not copied: the shipment plans. They are re-created when the plan is scheduled.
   * </ul>
   */
  public static LSPPlan copyPlan(LSPPlan plan2copy) {
    LSPPlan copiedPlan = LSPUtils.createLSPPlan();
    copiedPlan.setInitialShipmentAssigner(plan2copy.getInitialShipmentAssigner());
    copiedPlan.setScore(plan2copy.getScore());
    copiedPlan.setType(plan2copy.getType());

    for (LogisticChain initialPlanChain : plan2copy.getLogisticChains()) {
      LogisticChain newPlanChain = LSPUtils.LogisticChainBuilder.newInstance(initialPlanChain.getId()).build();
      newPlanChain.getLogisticChainElements().addAll(initialPlanChain.getLogisticChainElements());
      newPlanChain.getLspShipmentIds().addAll(initialPlanChain.getLspShipmentIds());
      AttributesUtils.copyTo(initialPlanChain.getAttributes(), newPlanChain.getAttributes());
      copiedPlan.addLogisticChain(newPlanChain);
    }

    // Set the LSP after the chains are added, so that the chains also get the backpointer to the LSP.
    copiedPlan.setLSP(plan2copy.getLSP());
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
    resetSchedulingState();
    logisticChainScheduler.scheduleLogisticChain();
  }

  /**
   * Resets the state that a scheduling run builds up, so that every run starts from the same state:
   *
   * <ul>
   *   <li>The shipment plans of the selected plan are removed. They are the result of the scheduling run and
   *       are re-created by it.
   *   <li>The incoming and outgoing {@link WaitingShipments} of all {@link LogisticChainElement}s of all plans
   *       of this LSP are emptied. The waiting shipments are temporary state of a scheduling run. Without this,
   *       shipments that remain in them from a previous run (e.g. in the outgoing shipments of the last
   *       element, or after a run that was aborted) would be handled again. The elements of all plans are
   *       cleared, since the elements are shared between plans (see {@link #copyPlan(LSPPlan)}) and a resource
   *       schedules the incoming shipments of all its client elements.
   * </ul>
   *
   * The assignment of the shipments to the logistic chains is part of the plan and is not changed.
   */
  private void resetSchedulingState() {
    selectedPlan.getShipmentPlans().clear();
    for (LSPPlan plan : lspPlans) {
      for (LogisticChain chain : plan.getLogisticChains()) {
        for (LogisticChainElement element : chain.getLogisticChainElements()) {
          element.getIncomingShipments().clear();
          element.getOutgoingShipments().clear();
        }
      }
    }
  }

  @Override
  public boolean addPlan(LSPPlan plan) {
    for (LogisticChain chain : plan.getLogisticChains()) {
      for (LogisticChainElement element : chain.getLogisticChainElements()) {
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
  public void assignShipmentToLspPlan(LspShipment lspShipment) {
    //		shipment.setLspId(this.getId()); // und rückweg dann auch darüber und dann
    // lsp.getselectedPlan.getShipment...
    lspShipments.add(lspShipment);
    for (LSPPlan lspPlan : lspPlans) {
      lspPlan.getInitialShipmentAssigner().assignToPlan(lspPlan, lspShipment);
		LspShipmentUtils.getOrCreateShipmentPlan(lspPlan, lspShipment.getId());
    }
  }

  @Override
  public Collection<LspShipment> getLspShipments() {
    return this.lspShipments;
  }
}
