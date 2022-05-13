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

import lsp.LSPInfo;
import lsp.LogisticsSolution;
import lsp.shipment.Requirement;

public class BlueRequirement implements Requirement{

	@Override
	public boolean checkRequirement(LogisticsSolution solution) {
		for(LSPInfo info : solution.getInfos()) {
			if(info instanceof BlueInfo) {
				return true;
			}
		}	
		return false;
	}

}
