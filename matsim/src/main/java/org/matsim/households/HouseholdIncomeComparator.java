/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdIncomeComparator
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
package org.matsim.households;

import java.io.Serializable;
import java.util.Comparator;


/**
 * Simple comparator for households to compare them by income
 *
 * @author dgrether
 */
public class HouseholdIncomeComparator implements Comparator<Household>, Serializable {

	private static final long serialVersionUID = 1L;

	@Override
	public int compare(Household o1, Household o2) {
		if (o1.getIncome().getIncomePeriod() != o2.getIncome().getIncomePeriod()){
			throw new IllegalArgumentException("Can only compare Households with incomes in "
					+ " same income period");
		}
		if (o1.getIncome().getIncome() < o2.getIncome().getIncome()){
			return -1;
		}
		else if (o1.getIncome().getIncome() > o2.getIncome().getIncome()){
			return 1;
		}
		return 0;
	}

}
