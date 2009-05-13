/* *********************************************************************** *
 * project: org.matsim.*
 * BasicHouseholdsImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.basic.v01.Id;


/**
 * Basic implementation of the BasicHousehold container
 * @author dgrether
 *
 */
public class BasicHouseholdsImpl implements BasicHouseholds<BasicHousehold> {

	private BasicHouseholdBuilder builder;

	private Map<Id, BasicHousehold> households;
	
	public BasicHouseholdsImpl(){
		this.households = new HashMap<Id, BasicHousehold>();
		this.builder = new BasicHouseholdBuilderImpl();
	}
	
	public BasicHouseholdBuilder getHouseholdBuilder() {
		return this.builder;
	}

	public Map<Id, BasicHousehold> getHouseholds() {
		return this.households;
	}
	
	

}
