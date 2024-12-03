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
import org.matsim.freight.logistics.shipment.LspShipmentPlan;

public class LSPPlanImpl implements LSPPlan {

  private final Collection<LogisticChain> logisticChains;
  private final Collection<LspShipmentPlan> lspShipmentPlans;
  private LSP lsp;
  private Double score = null;
  private InitialShipmentAssigner assigner;
  private String type = null;

  public LSPPlanImpl() {
    this.logisticChains = new ArrayList<>();
    this.lspShipmentPlans = new ArrayList<>();
  }

  @Override
  public LSPPlan addLogisticChain(LogisticChain solution) {
    this.logisticChains.add(solution);
    solution.setLSP(this.lsp);
    return this;
  }

  @Override
  public Collection<LogisticChain> getLogisticChains() {
    return logisticChains;
  }

  @Override
  public InitialShipmentAssigner getInitialShipmentAssigner() {
    return assigner;
  }

  @Override
  public LSPPlan setInitialShipmentAssigner(InitialShipmentAssigner assigner) {
    this.assigner = assigner;
    return this;
  }

  @Override
  public Collection<LspShipmentPlan> getShipmentPlans() {
    return this.lspShipmentPlans;
  }

  @Override
  public void addShipmentPlan(LspShipmentPlan lspShipmentPlan) {
    this.lspShipmentPlans.add(lspShipmentPlan);
  }

  @Override
  public Double getScore() {
    return score;
  }

  @Override
  public void setScore(Double score) {
    this.score = score;
  }

  @Override
  public String getType() {
    return this.type;
  }

  @Override
  public void setType(final String type) {
    this.type = type;
  }

  @Override
  public LSP getLSP() {
    return lsp;
  }

  @Override
  public void setLSP(LSP lsp) {
    this.lsp = lsp;
    for (LogisticChain solution : logisticChains) {
      solution.setLSP(lsp);
    }
  }

  @Override
  public String toString() {
    StringBuilder strb = new StringBuilder();
    strb.append("[score=").append(this.score).append("]");
    strb.append(", [type=").append(this.type).append("]");
    for (LogisticChain logisticChain : this.logisticChains) {
      strb.append(", [LogisticChainId=")
          .append(logisticChain.getId())
          .append("], [No of LogisticChainElements=")
          .append(logisticChain.getLogisticChainElements().size())
          .append("] \n");
      if (!logisticChain.getLogisticChainElements().isEmpty()) {
        for (LogisticChainElement solutionElement : logisticChain.getLogisticChainElements()) {
          strb.append("\t \t").append(solutionElement.toString()).append("\n");
        }
      }
    }
    return strb.toString();
  }
}
