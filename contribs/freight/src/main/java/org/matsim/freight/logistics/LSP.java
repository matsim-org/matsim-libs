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
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.freight.logistics.shipment.LspShipment;

/**
 * In the class library, the interface LSP has the following tasks: 1. Maintain one or several
 * transport chains through which {@link LspShipment}s are routed. 2. Assign {@link LspShipment}s to
 * the suitable transport chain. --> {@link InitialShipmentAssigner}. 3. Interact with the agents that
 * embody the demand side of the freight transport market, if they are specified in the setting. 4.
 * Coordinate carriers that are in charge of the physical transport.
 */
public interface LSP extends HasPlansAndId<LSPPlan, LSP>, HasSimulationTrackers<LSP> {

  /** Returns the shipments this {@link LSP} is responsible for.
   * yyyy does this have to be exposed? */
  Collection<LspShipment> getLspShipments();

  /** Schedules the logistic chains of all plans of this {@link LSP}.
   * ok (behavioral method)*/
  void scheduleLogisticChains();

  /** Returns the resources operated by this {@link LSP}.
   * yyyy does this have to be exposed?  */
  Collection<LSPResource> getResources();

  /** Scores the currently selected plan of this {@link LSP}.
   * ok (behavioral method)  */
  void scoreSelectedPlan();

  /**
   * Tells this {@link LSP} that it becomes responsible for the given shipment, assigning it to each
   * plan of the {@link LSP}.
   *
   * @param lspShipment the shipment this {@link LSP} should now be responsible for
   */
  void assignShipmentToLspPlan(LspShipment lspShipment);

}
