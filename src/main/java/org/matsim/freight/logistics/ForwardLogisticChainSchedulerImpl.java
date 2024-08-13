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
import org.matsim.api.core.v01.Id;
import org.matsim.freight.logistics.shipment.LspShipment;

/**
 * .... Macht 3 Schritte: 1.) the LSPShipments are handed over to the first {@link
 * LogisticChainElement} of their {@link LogisticChain} 2.) the neighbors, i.e. the predecessors and
 * successors of all {@link LSPResource}s are determined 3.) the Resources are brought into the
 * right sequence according to the algorithm.
 *
 * <p>When traversing this list of {@link LSPResource}s, the operations in each {@link LSPResource}
 * are scheduled individually by calling their {@link LSPResourceScheduler}.
 */
/* package-private */ class ForwardLogisticChainSchedulerImpl implements LogisticChainScheduler {

  /**
   * The Resources are brought into the right sequence according to the algorithm. The result of
   * this algorithm is a list of Resources that is later traversed from the front to the back, i.e.
   * starting with the entry at index 0. In the algorithm, this list is called sortedResourceList.
   */
  private final ArrayList<LSPResource> sortedResourceList;

  /**
   * The determination of the neighborhood structure among the Resources resulted in the
   * neighborList.
   */
  private final ArrayList<ResourceNeighbours> neighbourList;

  private LSP lsp;
  private int bufferTime;

  ForwardLogisticChainSchedulerImpl() {
    this.sortedResourceList = new ArrayList<>();
    this.neighbourList = new ArrayList<>();
  }

  @Override
  public void scheduleLogisticChain() {
    insertShipmentsAtBeginning();
    setResourceNeighbours();
    sortResources();
    for (LSPResource resource : sortedResourceList) {
      resource.schedule(bufferTime, lsp.getSelectedPlan());
    }
  }

  @Override
  public void setEmbeddingContainer(LSP lsp) {
    this.lsp = lsp;
  }

  private void setResourceNeighbours() {
    // internal data structure, try to ignore when looking from outside.  kai/kai, jan'22
    neighbourList.clear();
    for (LSPResource resource : lsp.getResources()) {
      ResourceNeighbours neighbours = new ResourceNeighbours(resource);
      for (LogisticChainElement element : resource.getClientElements()) {
        LogisticChainElement predecessor = element.getPreviousElement();
        LSPResource previousResource = predecessor.getResource();
        neighbours.addPredecessor(previousResource);
        LogisticChainElement successor = element.getNextElement();
        LSPResource nextResource = successor.getResource();
        neighbours.addSuccessor(nextResource);
      }
      neighbourList.add(neighbours);
    }
  }

  private void sortResources() {
    sortedResourceList.clear();
    while (!neighbourList.isEmpty()) {
      for (ResourceNeighbours neighbours : neighbourList) {
        if (allPredecessorsAlreadyScheduled(neighbours)
            && noSuccessorAlreadyScheduled(neighbours)) {
          sortedResourceList.add(neighbours.resource);
          neighbourList.remove(neighbours);
        }
      }
    }
  }

  private boolean allPredecessorsAlreadyScheduled(ResourceNeighbours neighbours) {
    if (neighbours.predecessors.isEmpty()) {
      return true;
    }

    for (LSPResource predecessor : neighbours.predecessors) {
      if (!sortedResourceList.contains(predecessor)) {
        return true;
      }
    }
    return false;
  }

  private boolean noSuccessorAlreadyScheduled(ResourceNeighbours neighbours) {
    if (neighbours.successors.isEmpty()) {
      return true;
    }

    for (LSPResource successor : neighbours.successors) {
      if (!sortedResourceList.contains(successor)) {
        return true;
      }
    }
    return false;
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

  /**
   * The relationship between different {@link LSPResource}s allows to handle various supply
   * structures that the {@link LSP} might decide to maintain. Thus, a {@link LSPResource} can have
   * several successors or predecessors or can be used by several different {@link LogisticChain}s.
   * The neighborhood structure among the {@link LSPResource}s is stored in instances of the class
   * {@link ResourceNeighbours} which contain references on the considered {@link LSPResource} and
   * on the set of immediate successors respective predecessors. As the result of this step, a
   * collection of {@link ResourceNeighbours} called neighborList is created that contains the
   * neighbors of all {@link LSPResource}s in the plan of the considered {@link LSP}.
   */
  private static class ResourceNeighbours {
    // internal data structure, try to ignore when looking from outside.  kai/kai, jan'22

    private final ArrayList<LSPResource> predecessors;
    private final ArrayList<LSPResource> successors;
    private final LSPResource resource;

    private ResourceNeighbours(LSPResource resource) {
      this.resource = resource;
      this.predecessors = new ArrayList<>();
      this.successors = new ArrayList<>();
    }

    private void addPredecessor(LSPResource resource) {
      this.predecessors.add(resource);
    }

    private void addSuccessor(LSPResource resource) {
      this.successors.add(resource);
    }
  }
}
