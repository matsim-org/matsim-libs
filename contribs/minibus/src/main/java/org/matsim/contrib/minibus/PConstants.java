/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.minibus;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.operator.PPlan;

/**
 * @author aneumann
 */
public final class PConstants {

	public static final String statsOutputFolder = "/pStats/";
	
	public static final Id<PPlan> founderPlanId = Id.create("none", PPlan.class);
	
	public enum OperatorState {
	    PROSPECTING, INBUSINESS, BANKRUPT
	}
	
	private PConstants() {
		
	}
}