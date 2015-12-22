/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,       *
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
package playground.johannes.synpop.processing;

import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.CommonValues;
import playground.johannes.synpop.data.Person;

/**
 * @author jillenberger
 */
public class ValidateMissingAttribute implements PersonTask {

    private final String key;

    public ValidateMissingAttribute(String key) {
        this.key = key;
    }

    @Override
    public void apply(Person person) {
        if(person.getAttribute(key) == null) person.setAttribute(CommonKeys.DELETE, CommonValues.TRUE);
    }
}
