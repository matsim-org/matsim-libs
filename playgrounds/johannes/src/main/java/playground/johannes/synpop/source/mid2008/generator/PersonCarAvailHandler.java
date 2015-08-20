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

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.mid.MIDKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.source.mid2008.generator.PersonAttributeHandler;

import java.util.Map;

/**
 * @author johannes
 */
public class PersonCarAvailHandler implements PersonAttributeHandler {

    @Override
    public void handle(Person person, Map<String, String> attributes) {
        String val = attributes.get(VariableNames.PERSON_CARAVAIL);

        if(val != null) {
            if(val.equalsIgnoreCase("1")) person.setAttribute(CommonKeys.PERSON_CARAVAIL, CommonKeys.ALWAYS);
            if(val.equalsIgnoreCase("2")) person.setAttribute(CommonKeys.PERSON_CARAVAIL, CommonKeys.SOMETIMES);
            if(val.equalsIgnoreCase("3")) person.setAttribute(CommonKeys.PERSON_CARAVAIL, CommonKeys.NEVER);
            if(val.equalsIgnoreCase("4")) person.setAttribute(CommonKeys.PERSON_CARAVAIL, CommonKeys.NEVER);
        }
    }
}
