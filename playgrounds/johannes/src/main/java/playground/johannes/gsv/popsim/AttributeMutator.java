/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.popsim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.sim3.Mutator;

/**
 * @author johannes
 *
 */
public abstract class AttributeMutator implements Mutator {

	private final Random random;
	
	private final ArrayList<ProxyPerson> mutations;
	
	private Double prevValue;
	
	private final String strKey;
	
	private final Object objKey;
	
	public AttributeMutator(Random random, String strKey, Object objKey) {
		this.random = random;
		this.strKey = strKey;
		this.objKey = objKey;
		
		mutations = new ArrayList<>(1);
		mutations.add(null);
	}
	
	@Override
	public List<ProxyPerson> select(List<ProxyPerson> persons) {
		mutations.set(0, persons.get(random.nextInt(persons.size())));
		return mutations;
	}

	@Override
	public boolean modify(List<ProxyPerson> persons) {
		ProxyPerson person = persons.get(0);
		prevValue = getAttribute(person, strKey, objKey);
		double newValue = newValue(person);
		person.setUserData(objKey, newValue);
		
		return true;
	}

	@Override
	public void revert(List<ProxyPerson> persons) {
		persons.get(0).setUserData(objKey, prevValue);
		prevValue = null;
	}

	private Double getAttribute(ProxyPerson person, String strKey, Object objKey) {
		Double doubleVal = (Double) person.getUserData(objKey);
		if (doubleVal == null) {
			String stringVal = person.getAttribute(strKey);
			
			if(stringVal != null) {
				doubleVal = new Double(stringVal);
				person.setUserData(objKey, doubleVal);
			}
		}

		return doubleVal;
	}
	
	protected abstract Double newValue(ProxyPerson person);
}
