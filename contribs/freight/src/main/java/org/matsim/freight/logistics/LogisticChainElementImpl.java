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
  // Not set by the builder of this element; set by LSPUtils.LogisticChainBuilder#build().
  private LogisticChainElement previousElement;
  private LogisticChainElement nextElement;

  LogisticChainElementImpl(LSPUtils.LogisticChainElementBuilder builder) {
    super(builder.id);
    this.resource = builder.resource;
    this.incomingShipments = builder.incomingShipments;
    this.outgoingShipments = builder.outgoingShipments;
    resource.getClientElements().add(this);
  }

  /**
   * Sets this element and the given element as neighbors in the chain. Only called by {@link
   * LSPUtils.LogisticChainBuilder#build()}, which connects the elements in the order in which they were added.
   */
  void connectWithNextElement(LogisticChainElementImpl element) {
    this.nextElement = element;
    element.previousElement = this;
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
    strb.append("LogisticChainElementImpl{")
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
