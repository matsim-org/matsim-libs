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

package playground.johannes.synpop.source.mid2008.processing;

import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.processing.PersonTask;

/**
 * @author johannes
 */
public class AdjustJourneyWeight implements PersonTask {

    @Override
    public void apply(Person person) {
        double weight = Double.parseDouble(person.getAttribute(CommonKeys.PERSON_WEIGHT));
        weight = weight / 75.0;
//        weight = weight / 45.0; // 3 month time frame
//        weight = weight / 30.0; // 3 month time frame
//        weight = weight / 365.0;
        person.setAttribute(CommonKeys.PERSON_WEIGHT, String.valueOf(weight));
    }
}
