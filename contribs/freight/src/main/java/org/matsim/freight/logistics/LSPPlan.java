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

import java.util.Collection;
import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlan;

/**
 * This interface has the following properties:
 *
 * <ul>
 *   <li>As a {@link BasicPlan} it has a score, so it can be used for evolutionary learning. kai,
 *       may'22
 *   <li>An {@link LspShipment} is added via lspPlan#getAssigner().assignToSolution(shipment). The
 *       {@link InitialShipmentAssigner} assigns it deterministically to a {@link LogisticChain}.
 * </ul>
 */
public interface LSPPlan extends BasicPlan, KnowsLSP {

  LSPPlan addLogisticChain(LogisticChain solution);

  Collection<LogisticChain> getLogisticChains();

  /**
   * yy My intuition would be to replace lspPlan#getAssigner().assignToSolution( shipment ) by
   * lspPlan.addShipment( shipment ). kai, may'22
   */
  InitialShipmentAssigner getInitialShipmentAssigner();

  LSPPlan setInitialShipmentAssigner(InitialShipmentAssigner assigner);

  Collection<LspShipmentPlan> getShipmentPlans();

  void addShipmentPlan(LspShipmentPlan lspShipmentPlan);

  String getType();

  void setType(final String type);
}
