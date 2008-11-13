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

package org.matsim.population;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.basic.v01.BasicHouseholdImpl;
import org.matsim.basic.v01.Id;

/**
 * @author dgrether
 */
public class HouseholdImpl extends BasicHouseholdImpl implements Household {

	private Map<Id, Person> members;
	
	public HouseholdImpl(Id id) {
		super(id);
	}
	
	public void addMember(Person member) {
		if (this.members == null) {
			this.members = new HashMap<Id, Person>();
		}
		this.members.put(member.getId(), member);
		member.setHousehold(this);
	}

	@Override
	public List<Id> getMemberIds() {
		if (this.members == null) {
			return null;
		}
		return new ArrayList<Id>(this.members.keySet());
	}
	
	@Override
	public void setMemberIds(List<Id> members) {
		throw new UnsupportedOperationException("Do not set only Ids on this level in inheritance hierarchy!" +
				"Use method addMember(Person p) instead!");
	}
	
	public Map<Id, Person> getMembers() {
		return this.members;
	}

}
