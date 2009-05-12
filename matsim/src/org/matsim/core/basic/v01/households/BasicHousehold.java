/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.basic.v01.households;

import java.util.List;

import org.matsim.api.basic.v01.Id;

/**
 * @author dgrether
 */
public interface BasicHousehold {

	public Id getId();
	
	public List<Id> getMemberIds();
	
	public BasicIncome getIncome();
	
	public List<Id> getVehicleIds();

	public void setIncome(BasicIncome income);

}
