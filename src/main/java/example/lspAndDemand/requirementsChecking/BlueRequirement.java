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

package example.lspAndDemand.requirementsChecking;

import lsp.LogisticsSolution;
import lsp.shipment.Requirement;

import static example.lspAndDemand.requirementsChecking.ExampleCheckRequirementsOfAssigner.ATTRIBUTE_COLOR;

/*package-private*/ class BlueRequirement implements Requirement {

	static final String BLUE = "blue";

	@Override
	public boolean checkRequirement(LogisticsSolution solution) {
		return solution.getAttributes().getAttribute(ATTRIBUTE_COLOR).equals(BLUE);
	}

}
