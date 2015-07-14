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
import playground.johannes.gsv.synPop.ProxyPerson;

import java.util.Map;

/**
 * @author johannes
 */
public class PersonCarAvailHandler implements PersonAttributeHandler {
    @Override
    public void handle(ProxyPerson person, Map<String, String> attributes) {
        String val = attributes.get(MIDKeys.PERSON_CARAVAIL);

        if(val != null) {
            if(val.equalsIgnoreCase("jederzeit")) person.setAttribute(CommonKeys.PERSON_CARAVAIL, CommonKeys.ALWAYS);
            if(val.equalsIgnoreCase("gelegentlich")) person.setAttribute(CommonKeys.PERSON_CARAVAIL, CommonKeys.SOMETIMES);
            if(val.equalsIgnoreCase("gar nicht")) person.setAttribute(CommonKeys.PERSON_CARAVAIL, CommonKeys.NEVER);
            if(val.equalsIgnoreCase("habe keinen Führerschein")) person.setAttribute(CommonKeys.PERSON_CARAVAIL, CommonKeys.NEVER);
        }
    }
}
