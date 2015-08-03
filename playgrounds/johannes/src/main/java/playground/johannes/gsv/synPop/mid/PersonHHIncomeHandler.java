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

package playground.johannes.gsv.synPop.mid;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.synpop.data.PlainPerson;

import java.util.Map;

/**
 * @author johannes
 *
 */
public class PersonHHIncomeHandler implements PersonAttributeHandler {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.mid.PersonAttributeHandler#handle(playground.johannes.synpop.data.PlainPerson, java.util.Map)
	 */
	@Override
	public void handle(PlainPerson person, Map<String, String> attributes) {
		String val = attributes.get(MIDKeys.HH_INCOME);
		
		if(val != null) {
			if(val.equalsIgnoreCase("bis unter 500 Euro")) person.setAttribute(CommonKeys.HH_INCOME, "250");
			else if(val.equalsIgnoreCase("500 Euro bis unter 900 Euro")) person.setAttribute(CommonKeys.HH_INCOME, "700");
			else if(val.equalsIgnoreCase("900 Euro bis unter 1.500 Euro")) person.setAttribute(CommonKeys.HH_INCOME, "1200");
			else if(val.equalsIgnoreCase("1.500 Euro bis unter 2.000 Euro")) person.setAttribute(CommonKeys.HH_INCOME, "1750");
			else if(val.equalsIgnoreCase("2.000 Euro bis unter 2.600 Euro")) person.setAttribute(CommonKeys.HH_INCOME, "2300");
			else if(val.equalsIgnoreCase("2.600 Euro bis unter 3.000 Euro")) person.setAttribute(CommonKeys.HH_INCOME, "2800");
			else if(val.equalsIgnoreCase("3.000 Euro bis unter 3.600 Euro")) person.setAttribute(CommonKeys.HH_INCOME, "3300");
			else if(val.equalsIgnoreCase("3.600 Euro bis unter 4.000 Euro")) person.setAttribute(CommonKeys.HH_INCOME, "3800");
			else if(val.equalsIgnoreCase("4.000 Euro bis unter 4.600 Euro")) person.setAttribute(CommonKeys.HH_INCOME, "4300");
			else if(val.equalsIgnoreCase("4.600 Euro bis unter 5.000 Euro")) person.setAttribute(CommonKeys.HH_INCOME, "4800");
			else if(val.equalsIgnoreCase("5.000 Euro bis unter 5.600 Euro")) person.setAttribute(CommonKeys.HH_INCOME, "5300");
			else if(val.equalsIgnoreCase("5.600 Euro bis unter 6.000 Euro")) person.setAttribute(CommonKeys.HH_INCOME, "5800");
			else if(val.equalsIgnoreCase("6.000 Euro bis unter 6.600 Euro")) person.setAttribute(CommonKeys.HH_INCOME, "6300");
			else if(val.equalsIgnoreCase("6.600 Euro bis 7.000 Euro")) person.setAttribute(CommonKeys.HH_INCOME, "6800");
			else if(val.equalsIgnoreCase("mehr als 7.000 Euro")) person.setAttribute(CommonKeys.HH_INCOME, "7300");
		}

	}

}
