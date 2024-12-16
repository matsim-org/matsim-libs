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

import java.util.List;
import org.matsim.api.core.v01.Id;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.shipment.LspShipment;

/**
 * In the class SimpleForwardSolutionScheduler two tasks are performed:
 *
 * <p>1.) the {@link LspShipment}s that were assigned to the suitable {@link LogisticChain} by the
 * {@link InitialShipmentAssigner} in a previous step are handed over to the first {@link
 * LogisticChainElement}.
 *
 * <p>2.) all {@link LSPResource}s that were handed over to the SimpleForwardSolutionScheduler
 * exogenous, are now scheduled sequentially in an order that was also specified exogenously. This
 * order ensures that each {@link LogisticChain} is traversed from the first to the last {@link
 * LogisticChainElement}. During this procedure, the concerned {@link LspShipment}s are taken from
 * the collection of incoming shipments, handled by the {@link LSPResource} in charge and then added
 * to the collection of outgoing shipments of the client {@link LogisticChainElement}.
 *
 * <p>The SimpleForwardSolutionScheduler needs the sequence in which the Resources are scheduled as
 * exogenous input.
 *
 * <p>The expression "`forward"' refers to the fact that in both cases the scheduling process starts
 * at the first element of each {@link LogisticChain} and from the earliest possible point of time.
 */
/*package-private*/ class SimpleForwardLogisticChainScheduler implements LogisticChainScheduler {

  private final List<LSPResource> resources;
  private LSP lsp;
  private int bufferTime;

  SimpleForwardLogisticChainScheduler(List<LSPResource> resources) {
    this.resources = resources;
  }

  @Override
  public void scheduleLogisticChain() {
    insertShipmentsAtBeginning();
    for (LSPResource resource : resources) {
      for (LSPResource lspResource : lsp.getResources()) {
        if (lspResource == resource) {
          lspResource.schedule(bufferTime, lsp.getSelectedPlan());
        }
      }
    }
  }

  @Override
  public void setEmbeddingContainer(LSP lsp) {
    this.lsp = lsp;
  }

  private void insertShipmentsAtBeginning() {
    for (LogisticChain solution : lsp.getSelectedPlan().getLogisticChains()) {
      LogisticChainElement firstElement = getFirstElement(solution);
      assert firstElement != null;
      for (Id<LspShipment> lspShipmentId : solution.getLspShipmentIds()) {
        var shipment = LSPUtils.findLspShipment(lsp, lspShipmentId);
        assert shipment != null;
        firstElement
            .getIncomingShipments()
            .addShipment(shipment.getPickupTimeWindow().getStart(), shipment);
      }
    }
  }

  private LogisticChainElement getFirstElement(LogisticChain solution) {
    for (LogisticChainElement element : solution.getLogisticChainElements()) {
      if (element.getPreviousElement() == null) {
        return element;
      }
    }
    return null;
  }

  @Override
  public void setBufferTime(int bufferTime) {
    this.bufferTime = bufferTime;
  }
}
