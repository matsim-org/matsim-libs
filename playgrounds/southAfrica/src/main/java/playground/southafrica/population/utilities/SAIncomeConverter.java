/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.southafrica.population.utilities;

import org.apache.log4j.Logger;
import org.matsim.households.Income;
import org.matsim.households.IncomeImpl;
import org.matsim.households.Income.IncomePeriod;
import org.matsim.utils.objectattributes.AttributeConverter;

public class SAIncomeConverter implements AttributeConverter<IncomeImpl>{
	private final Logger log = Logger.getLogger(SAIncomeConverter.class);
	
	@Override
	public IncomeImpl convert(String value) {
		if(value.equalsIgnoreCase("")){
			/* Unknown income. */
			return null;
		}
		
		String[] sa = value.split("_");
		String currency = sa[0];
		String incomeString = sa[1].substring(0, sa[1].indexOf("("));
		double incomeValue = Double.parseDouble(incomeString);
		String incomePeriodString = sa[1].substring(sa[1].indexOf("(")+1, sa[1].indexOf(")"));
		IncomePeriod incomePeriod = null;
		if(incomePeriodString.equalsIgnoreCase("year")){
			incomePeriod = IncomePeriod.year;
		} else if(incomePeriodString.equalsIgnoreCase("month")){
			incomePeriod = IncomePeriod.month;
		}
		
		Income income = new IncomeImpl(incomeValue, incomePeriod);
		income.setCurrency(currency);
		
		return (IncomeImpl)income;
	}
	
	@Override
	public String convertToString(Object o) {
		if(o == null){
			return "";
		} else if(o instanceof IncomeImpl){
			Income income = (Income)o;
			String s = String.format("%s_%.2f(%s)", income.getCurrency(), income.getIncome(), income.getIncomePeriod().toString());
			return s;
		}
		log.warn("Couldn't convert Income: " + o.toString() + "; returning empty string.");
		return "";
	}
	
}