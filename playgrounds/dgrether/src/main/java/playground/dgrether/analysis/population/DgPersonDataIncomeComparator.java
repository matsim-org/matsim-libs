/* *********************************************************************** *
 * project: org.matsim.*
 * DgPersonDataIncomeComparator
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
package playground.dgrether.analysis.population;

import java.util.Comparator;


/**
 * Compares DgPersons by income if set for the same period.
 * @author dgrether
 *
 */
public class DgPersonDataIncomeComparator implements Comparator<DgPersonData> {

	public int compare(DgPersonData p1, DgPersonData p2) {
		if (p1.getIncome().getIncomePeriod() != p2.getIncome().getIncomePeriod()){
			throw new IllegalArgumentException("Cannot compare different income periods by default. Please normalize manually!");
		}
		if (p1.getIncome().getIncome() < p2.getIncome().getIncome()){
			return -1;
		}
		else if (p1.getIncome().getIncome() > p2.getIncome().getIncome()){
			return 1;
		}
		return 0;
	}

}
