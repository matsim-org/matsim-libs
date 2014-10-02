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

package playground.johannes.gsv.synPop.invermo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPersonsTask;

/**
 * @author johannes
 *
 */
public class DeleteNoWeight implements ProxyPersonsTask {

	private static final Logger logger = Logger.getLogger(DeleteNoWeight.class);
	
	@Override
	public void apply(Collection<ProxyPerson> persons) {
		Set<ProxyPerson> remove = new HashSet<>();
		for(ProxyPerson person : persons) {
			if(!person.getAttributes().containsKey(CommonKeys.PERSON_WEIGHT)) {
				remove.add(person);
			}
		}
		
		for(ProxyPerson person : remove) {
			persons.remove(person);
		}

		logger.warn(String.format("Removed %s out of %s persons because of missing weight attribute.", remove.size(), remove.size() + persons.size()));
	}

}
