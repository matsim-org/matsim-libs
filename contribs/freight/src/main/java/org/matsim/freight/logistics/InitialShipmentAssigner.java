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

import org.matsim.freight.logistics.shipment.LspShipment;

/**
 * Takes an {@link LspShipment} and normally assigns it to something that belongs to an {@link LSP}.
 * <br>
 * After changes in fall 2023 (see master thesis of nrichter), the assignment is
 * there to be done one time initially.
 * <br>
 * If there are several {@link LogisticChain}s in a {@link LSPPlan}, the {@link LSP} has to assign each {@link
 * LspShipment} to the suitable {@link LogisticChain}. For this purpose, each {@link LSPPlan}
 * (or only the LSP? - kmt jan'24), contains a pluggable strategy
 * that is contained in classes implementing the interface {@link InitialShipmentAssigner}. <br>
 * <br>
 * During iterations, it can happen that the {@link LspShipment} should be moved to another
 * {@link LogisticChain} of the same {@link LSPPlan}. This is now (since fall 2023; see master
 * thesis of nrichter) part of the (innovative) **Replanning** strategies.
 */
public interface InitialShipmentAssigner {

  void assignToPlan(LSPPlan lspPlan, LspShipment lspShipment);
}
