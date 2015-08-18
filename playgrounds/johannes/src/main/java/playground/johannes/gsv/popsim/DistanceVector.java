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

import org.apache.log4j.Logger;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.sim3.Hamiltonian;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainPerson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * @author johannes
 * 
 */
public class DistanceVector implements Hamiltonian {

	public static final Object AGE_KEY = new Object();

	public static final Object INCOME_KEY = new Object();

	private final List<PlainPerson> referencePop;

	private double ageMin = Double.MAX_VALUE;

	private double incomeMin = Double.MAX_VALUE;

	private double ageMax = 0;

	private double incomeMax = 0;

	private double ageMaxDelta = 0;

	private double incomeMaxDelta = 0;

	private static final Logger logger = Logger.getLogger(DistanceVector.class);

	private final Random random;

	public DistanceVector(Collection<PlainPerson> referencePop, Random random) {
		this.referencePop = new ArrayList<>(referencePop);
		this.random = random;

		int cnt = 0;

		for (PlainPerson person : referencePop) {
			Double age = getAttribute(person, CommonKeys.PERSON_AGE, AGE_KEY);
			Double income = getAttribute(person, CommonKeys.HH_INCOME, INCOME_KEY);

			if (age != null && income != null) {
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
	public double evaluate(Person person) {
		Double age = getAttribute((PlainPerson)person, CommonKeys.PERSON_AGE, AGE_KEY);
//		Double income = getAttribute(person, CommonKeys.HH_INCOME, INCOME_KEY);

		double totalDelta = 0;

		// for (PlainPerson ref : referencePop) {
		for (int i = 0; i < 100; i++) {
			PlainPerson ref = referencePop.get(random.nextInt(referencePop.size()));

			Double refAge = getAttribute(ref, CommonKeys.PERSON_AGE, AGE_KEY);
//			Double refIncome = getAttribute(ref, CommonKeys.HH_INCOME, INCOME_KEY);

//			if (refAge != null && refIncome != null) {
			if (refAge != null) {
				double deltaAge = (refAge - age) / ageMaxDelta;
//				double deltaIncome = (refIncome - income) / incomeMaxDelta;

//				double delta = Math.sqrt(deltaAge * deltaAge + deltaIncome * deltaIncome);
				double delta = Math.abs(deltaAge);
				delta += 1;
				if (delta == 0) {
					delta = -Double.MAX_VALUE;
				} else {
					delta = delta / Math.sqrt(2);
					delta = Math.log(delta);
				}
				totalDelta += delta;
			}
		}

		// totalDelta = Math.log(totalDelta);
		// return totalDelta / (double)referencePop.size();
		return totalDelta / 100.0;
	}

	private Double getAttribute(PlainPerson person, String strKey, Object objKey) {
		Double doubleVal = (Double) person.getUserData(objKey);
		if (doubleVal == null) {
			String stringVal = person.getAttribute(strKey);

			if (stringVal != null) {
				doubleVal = new Double(stringVal);
				person.setUserData(objKey, doubleVal);
			}
		}

		return doubleVal;
	}
}
