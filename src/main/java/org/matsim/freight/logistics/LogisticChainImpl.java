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
import org.matsim.api.core.v01.Id;
import org.matsim.freight.logistics.shipment.LspShipment;

/* package-private */ class LogisticChainImpl extends LSPDataObject<LogisticChain>
    implements LogisticChain {
  private static final Logger log = LogManager.getLogger(LogisticChainImpl.class);

  private final Collection<LogisticChainElement> logisticChainElements;
  private final Collection<Id<LspShipment>> lspShipmentIds;
  private LSP lsp;

  LogisticChainImpl(LSPUtils.LogisticChainBuilder builder) {
    super(builder.id);
    this.logisticChainElements = builder.elements;
    for (LogisticChainElement element : this.logisticChainElements) {
      element.setEmbeddingContainer(this);
    }
    this.lspShipmentIds = new ArrayList<>();
  }

  @Override
  public LSP getLSP() {
    return lsp;
  }

  @Override
  public void setLSP(LSP lsp) {
    this.lsp = lsp;
  }

  @Override
  public Collection<LogisticChainElement> getLogisticChainElements() {
    return logisticChainElements;
  }

  @Override
  public Collection<Id<LspShipment>> getLspShipmentIds() {
    return lspShipmentIds;
  }

  @Override
  public void addShipmentToChain(LspShipment lspShipment) {
    lspShipmentIds.add(lspShipment.getId());
  }

  @Override
  public String toString() {
    StringBuilder strb = new StringBuilder();
    strb.append("LogisticsSolutionImpl{")
        .append("[No of SolutionsElements=")
        .append(logisticChainElements.size())
        .append("] \n");
    if (!logisticChainElements.isEmpty()) {
      strb.append("{SolutionElements=");
      for (LogisticChainElement solutionElement : logisticChainElements) {
        strb.append("\n [").append(solutionElement.toString()).append("]");
      }
      strb.append("}");
    }
    strb.append("[No of Shipments=").append(lspShipmentIds.size()).append("] \n");
    if (!lspShipmentIds.isEmpty()) {
      strb.append("{ShipmentIds=");
      for (Id<LspShipment> lspShipmentId : lspShipmentIds) {
        strb.append("[").append(lspShipmentId.toString()).append("]");
      }
      strb.append("}");
    }
    strb.append('}');
    return strb.toString();
  }
}
