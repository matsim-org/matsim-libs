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

import java.util.Collection;

import lsp.shipment.LSPShipment;
import org.matsim.api.core.v01.population.BasicPlan;

/**
 * This interface has the following properties:<ul>
 * <li> As a {@link BasicPlan} it has a score, so it can be used for evolutionary learning.  kai, may'22 </li>
 * <li> An {@link LSPShipment} is added via lspPlan#getAssigner().assignToSolution(shipment).  The {@link ShipmentAssigner} assigns it deterministically to a {@link LogisticsSolution}. </li>
 * </ul>
 */
public interface LSPPlan extends BasicPlan, KnowsLSP {

	LSPPlan addSolution(LogisticsSolution solution);

	Collection<LogisticsSolution> getSolutions();

	/**
	 * yy My intuition would be to replace lspPlan#getAssigner().assignToSolution( shipment ) by lspPlan.addShipment( shipment ).  kai, may'22
	 */
	ShipmentAssigner getAssigner();

	LSPPlan setAssigner(ShipmentAssigner assigner);

}
