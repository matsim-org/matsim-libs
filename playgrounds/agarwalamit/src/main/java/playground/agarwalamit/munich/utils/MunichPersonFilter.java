/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.munich.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */

public class MunichPersonFilter extends PersonFilter implements playground.agarwalamit.utils.PersonFilter{

	public enum MunichUserGroup {Urban, Rev_Commuter, Freight}

	private final PersonFilter pf = new PersonFilter();

	public MunichPersonFilter (){}

    @Override
	public String getUserGroupAsStringFromPersonId (final Id<Person> personId) {
		return getMunichUserGroupFromPersonId(personId).toString();
	}

	@Override
	public List<String> getUserGroupsAsStrings() {
		return Arrays.stream(MunichUserGroup.values()).map(Enum::toString).collect(Collectors.toList());
	}

	/**
	 * @return Urban or (Rev) commuter or Freight from person id.
	 */
	public MunichUserGroup getMunichUserGroupFromPersonId(final Id<Person> personId) {
		if (pf.isPersonFreight(personId) ) return MunichUserGroup.Freight;
		else if (pf.isPersonFromMID(personId)) return MunichUserGroup.Urban;
		else return MunichUserGroup.Rev_Commuter;
	}
	
	/**
	 * A translation between UserGroup and MunichUserGroup 
	 * Helpful for writing data to files.
	 */
	public MunichUserGroup getMunichUserGroup(final UserGroup ug){
		switch(ug){
			case REV_COMMUTER:
			case COMMUTER: return MunichUserGroup.Rev_Commuter;
			case FREIGHT: return MunichUserGroup.Freight;
			case URBAN: return MunichUserGroup.Urban;
			default: throw new RuntimeException("User group "+ug+" is not recongnised. Aborting ...");
		}
	}
}