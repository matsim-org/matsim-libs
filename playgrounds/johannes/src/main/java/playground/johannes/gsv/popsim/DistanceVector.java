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

import java.util.Collection;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.sim3.Hamiltonian;

/**
 * @author johannes
 * 
 */
public class DistanceVector implements Hamiltonian {

	private static final Object AGE_KEY = new Object();

	private static final Object INCOME_KEY = new Object();
	
	private final Collection<ProxyPerson> referencePop;

	private double ageMin = Double.MAX_VALUE;
	
	private double incomeMin = Double.MAX_VALUE;
	
	private double ageMax = 0;
	
	private double incomeMax = 0;
	
	private double ageMaxDelta = 0;
	
	private double incomeMaxDelta = 0;
	
	public DistanceVector(Collection<ProxyPerson> referencePop) {
		this.referencePop = referencePop;
		
		int cnt = 0;
		
		for(ProxyPerson person : referencePop) {
			Double age = getAttribute(person, CommonKeys.PERSON_AGE, AGE_KEY);
			Double income = getAttribute(person, CommonKeys.HH_INCOME, INCOME_KEY);
			
			if(age != null && income != null) {
				ageMin = Math.min(ageMin, age);
				incomeMin = Math.min(incomeMin, income);
				
				ageMax = Math.max(ageMax, age);
				incomeMax = Math.max(incomeMax, income);
				
				cnt++;
			}
		}
		
		ageMaxDelta = ageMax - ageMin;
		incomeMaxDelta = incomeMax - incomeMin;
	}
	
	@Override
	public double evaluate(ProxyPerson person) {
		Double age = getAttribute(person, CommonKeys.PERSON_AGE, AGE_KEY);
		Double income = getAttribute(person, CommonKeys.HH_INCOME, INCOME_KEY);
		
		double totalDelta = 0;
		
		for (ProxyPerson ref : referencePop) {
			Double refAge = getAttribute(ref, CommonKeys.PERSON_AGE, AGE_KEY);
			Double refIncome = getAttribute(ref, CommonKeys.HH_INCOME, INCOME_KEY);
			
			if(refAge != null && refIncome != null) {
				double deltaAge = (refAge - age) / ageMaxDelta;
				double deltaIncome = (refIncome - income) / incomeMaxDelta;
				
				double delta = Math.sqrt(deltaAge * deltaAge + deltaIncome * deltaIncome);
				
				totalDelta += delta;
			}
		}
		
		return totalDelta;
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
}
