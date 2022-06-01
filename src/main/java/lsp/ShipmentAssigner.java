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

package lsp;

import lsp.shipment.LSPShipment;

/**
 * Takes an {@link LSPShipment} and normally assigns it to something that belongs to an {@link LSP}.
 * <br/><br/>
 * If there are several {@link LogisticsSolution}s, the {@link LSP} has to assign each {@link LSPShipment} to
 * the suitable one. For this purpose, each LSPPlan contains a pluggable strategy that
 * is contained in classes implementing the interface {@link ShipmentAssigner}.
 * <br/><br/>
 * Discussion points:<ul>
 *         <li> yyyy Shipments are normally assigned to the selected plan only.  I am not sure if this is what I would
 *         expect from the outside. kai, may'22</li>
 * </ul>
 */
public interface ShipmentAssigner extends KnowsLSP {

	void assignToSolution(LSPShipment shipment);

}
