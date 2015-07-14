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

import java.util.Map;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPerson;

/**
 * @author johannes
 *
 */
public class PersonAgeHandler implements PersonAttributeHandler {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.mid.PersonAttributeHandler#handle(playground.johannes.gsv.synPop.ProxyPerson, java.util.Map)
	 */
	@Override
	public void handle(ProxyPerson person, Map<String, String> attributes) {
		String val = attributes.get(MIDKeys.PERSON_AGE);


		if(val != null) {
			Integer age = Integer.parseInt(val);
			if(age > 0 && age < 102) {
				person.setAttribute(CommonKeys.PERSON_AGE, String.valueOf(age));
			}
//			if(val.equalsIgnoreCase("0 - 9 Jahre")) person.setAttribute(CommonKeys.PERSON_AGE, "2");
//			else if(val.equalsIgnoreCase("5 - 10")) person.setAttribute(CommonKeys.PERSON_AGE, "7");
//			else if(val.equalsIgnoreCase("11 - 13")) person.setAttribute(CommonKeys.PERSON_AGE, "12");
//			else if(val.equalsIgnoreCase("14 - 17")) person.setAttribute(CommonKeys.PERSON_AGE, "15");
//			else if(val.equalsIgnoreCase("18 - 29")) person.setAttribute(CommonKeys.PERSON_AGE, "24");
//			else if(val.equalsIgnoreCase("30 - 39")) person.setAttribute(CommonKeys.PERSON_AGE, "35");
//			else if(val.equalsIgnoreCase("40 - 49")) person.setAttribute(CommonKeys.PERSON_AGE, "45");
//			else if(val.equalsIgnoreCase("50 - 59")) person.setAttribute(CommonKeys.PERSON_AGE, "55");
//			else if(val.equalsIgnoreCase("60 - 64")) person.setAttribute(CommonKeys.PERSON_AGE, "62");
//			else if(val.equalsIgnoreCase("65 - 74")) person.setAttribute(CommonKeys.PERSON_AGE, "70");
//			else if(val.equalsIgnoreCase("75 und älter")) person.setAttribute(CommonKeys.PERSON_AGE, "80");
		}
	}

}
