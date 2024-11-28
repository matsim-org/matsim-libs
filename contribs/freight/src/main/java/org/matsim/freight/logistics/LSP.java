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

  /** yyyy does this have to be exposed? */
  Collection<LspShipment> getLspShipments();

  /** ok (behavioral method) */
  void scheduleLogisticChains();

  /** yyyy does this have to be exposed? */
  Collection<LSPResource> getResources();

  /** ok (behavioral method) */
  void scoreSelectedPlan();

  /**
   * @param lspShipment ok (LSP needs to be told that it is responsible for lspShipment)
   */
  void assignShipmentToLSP(LspShipment lspShipment);

}
