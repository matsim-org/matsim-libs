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
package playground.johannes.studies.matrix2014.sim;

import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.CommonValues;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.processing.PersonTask;

/**
 * @author jillenberger
 */
public class ValidatePersonWeight implements PersonTask {

    @Override
    public void apply(Person person) {
        double w = Double.parseDouble(person.getAttribute(CommonKeys.PERSON_WEIGHT));
        boolean valid = true;
        if(Double.isInfinite(w)) valid = false;
        else if(Double.isNaN(w)) valid = false;
        else if(w == 0) valid = false;

        if(!valid) {
            person.setAttribute(CommonKeys.DELETE, CommonValues.TRUE);
        }
    }
}
