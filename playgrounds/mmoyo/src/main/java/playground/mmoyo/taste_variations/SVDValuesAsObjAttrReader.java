/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.mmoyo.taste_variations;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

/**
 * Parses a ObjectAttributes file with svd values and returns a tree map with them
 */
public class SVDValuesAsObjAttrReader {
	 private Set<Id<Person>> personIds;
	
	public SVDValuesAsObjAttrReader(final Set<Id<Person>> personIds) {
		this.personIds = personIds;
	}
	
	protected Map <Id, IndividualPreferences> readFile(final String svdSolutionFile){
		Map <Id, IndividualPreferences> svdMap = new TreeMap <Id, IndividualPreferences>();
		
		ObjectAttributes attributes = new ObjectAttributes();
		ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(attributes);
		reader.parse(svdSolutionFile);
		final String STR_wWalk="wWalk";
		final String STR_wTime="wTime";
		final String STR_wDista="wDista";
		final String STR_wChng="wChng";
		for(Id id : personIds){
			String strId = id.toString();
			double wWalk = ((Double) attributes.getAttribute(strId, STR_wWalk)).doubleValue();
			double wTime = ((Double) attributes.getAttribute(strId, STR_wTime)).doubleValue();
			double wDista = ((Double) attributes.getAttribute(strId, STR_wDista)).doubleValue();
			double wChng = ((Double) attributes.getAttribute(strId, STR_wChng)).doubleValue();
			IndividualPreferences svdValues = new IndividualPreferences(id, wWalk, wTime, wDista, wChng); 
			svdMap.put(id, svdValues);
		}
		return svdMap;
	}

}