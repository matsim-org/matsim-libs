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

package playground.johannes.synpop.source.mid2008.generator;

import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;

import java.util.Map;

/**
 * @author johannes
 *
 */
public class PersonHHIncomeHandler implements PersonAttributeHandler {

	@Override
	public void handle(Person person, Map<String, String> attributes) {
		String val = attributes.get(VariableNames.HH_INCOME);
		
		if(val != null) {
			if(val.equalsIgnoreCase("1")) person.setAttribute(CommonKeys.HH_INCOME, "250");
			else if(val.equalsIgnoreCase("2")) person.setAttribute(CommonKeys.HH_INCOME, "700");
			else if(val.equalsIgnoreCase("3")) person.setAttribute(CommonKeys.HH_INCOME, "1200");
			else if(val.equalsIgnoreCase("4")) person.setAttribute(CommonKeys.HH_INCOME, "1750");
			else if(val.equalsIgnoreCase("5")) person.setAttribute(CommonKeys.HH_INCOME, "2300");
			else if(val.equalsIgnoreCase("6")) person.setAttribute(CommonKeys.HH_INCOME, "2800");
			else if(val.equalsIgnoreCase("7")) person.setAttribute(CommonKeys.HH_INCOME, "3300");
			else if(val.equalsIgnoreCase("8")) person.setAttribute(CommonKeys.HH_INCOME, "3800");
			else if(val.equalsIgnoreCase("9")) person.setAttribute(CommonKeys.HH_INCOME, "4300");
			else if(val.equalsIgnoreCase("10")) person.setAttribute(CommonKeys.HH_INCOME, "4800");
			else if(val.equalsIgnoreCase("11")) person.setAttribute(CommonKeys.HH_INCOME, "5300");
			else if(val.equalsIgnoreCase("12")) person.setAttribute(CommonKeys.HH_INCOME, "5800");
			else if(val.equalsIgnoreCase("13")) person.setAttribute(CommonKeys.HH_INCOME, "6300");
			else if(val.equalsIgnoreCase("14")) person.setAttribute(CommonKeys.HH_INCOME, "6800");
			else if(val.equalsIgnoreCase("15")) person.setAttribute(CommonKeys.HH_INCOME, "7300");
		}
	}
}
