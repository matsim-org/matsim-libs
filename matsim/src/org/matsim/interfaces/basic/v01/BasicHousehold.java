/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.interfaces.basic.v01;

import java.util.List;

import org.matsim.basic.v01.BasicLocationImpl;
import org.matsim.basic.v01.Id;


/**
 * @author dgrether
 *
 */
public interface BasicHousehold {

	public Id getId();
	
	public List<Id> getMemberIds();
	
	public BasicLocation getBasicLocation();
	
	public BasicIncome getIncome();
	
	public String getLanguage();

	public List<Id> getVehicleDefinitionIds();

	public void setLanguage(String language);

	public void setIncome(BasicIncome income);

	public void setLocation(BasicLocation location);

	
	
}
