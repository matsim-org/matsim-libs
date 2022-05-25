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

import org.matsim.api.core.v01.population.BasicPlan;

/**
 * This interface has the following properties:<ul>
 * <li> As a {@link BasicPlan} it has a score, so it can be used for evolutionary learning.  kai, may'22 </li>
 * </ul>
 *
 * Design questions:<ul>
 *         <li> yyyy I do not understand why it makes sense to give a plan multiple solutions.  kai, may'22 </li>
 * </ul>
 */
public interface LSPPlan extends BasicPlan, KnowsLSP {

	LSPPlan addSolution( LogisticsSolution solution );
	
	Collection<LogisticsSolution> getSolutions();

	ShipmentAssigner getAssigner();

	LSPPlan setAssigner( ShipmentAssigner assigner );

}
