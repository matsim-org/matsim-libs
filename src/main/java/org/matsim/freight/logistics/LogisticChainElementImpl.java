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

/* package-private */ class LogisticChainElementImpl extends LSPDataObject<LogisticChainElement>
    implements LogisticChainElement {

  private final LSPResource resource;
  private final WaitingShipments incomingShipments;
  private final WaitingShipments outgoingShipments;
  // die beiden nicht im Builder. Die können erst in der Solution als ganzes gesetzt werden
  private LogisticChainElement previousElement;
  private LogisticChainElement nextElement;

  LogisticChainElementImpl(LSPUtils.LogisticChainElementBuilder builder) {
    super(builder.id);
    this.resource = builder.resource;
    this.incomingShipments = builder.incomingShipments;
    this.outgoingShipments = builder.outgoingShipments;
    resource.getClientElements().add(this);
  }

  @Override
  public void connectWithNextElement(LogisticChainElement element) {
    this.nextElement = element;
    ((LogisticChainElementImpl) element).previousElement = this;
  }

  @Override
  public LSPResource getResource() {
    return resource;
  }

  @Override
  public WaitingShipments getIncomingShipments() {
    return incomingShipments;
  }

  @Override
  public WaitingShipments getOutgoingShipments() {
    return outgoingShipments;
  }

  @Override
  public void setEmbeddingContainer(LogisticChain logisticChain) {
    /* not */
  }

  @Override
  public LogisticChainElement getPreviousElement() {
    return previousElement;
  }

  @Override
  public LogisticChainElement getNextElement() {
    return nextElement;
  }

  @Override
  public String toString() {
    StringBuilder strb = new StringBuilder();
    strb.append("LogisticsSolutionElementImpl{")
        .append("resourceId=")
        .append(resource.getId())
        .append(", incomingShipments=")
        .append(incomingShipments)
        .append(", outgoingShipments=")
        .append(outgoingShipments);

    if (previousElement != null) {
      strb.append(", previousElementId=").append(previousElement.getId());
    } else {
      strb.append(", previousElementId=").append("null");
    }

    if (nextElement != null) {
      strb.append(", nextElementId=").append(nextElement.getId());
    } else {
      strb.append(", nextElementId=").append("null");
    }

    strb.append('}');
    return strb.toString();
  }
}
