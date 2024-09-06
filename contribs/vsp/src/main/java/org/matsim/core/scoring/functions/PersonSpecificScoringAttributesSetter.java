/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.core.scoring.functions;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonUtils;
import org.matsim.utils.math.RandomFromDistribution;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SplittableRandom;

public class PersonSpecificScoringAttributesSetter {

    public static void setLogNormalModeConstant(Collection<Person> persons, String mode, double mean, double sigma,
                                                SplittableRandom splittableRandom) {
        persons.forEach(person -> {
            Map<String, String> modeConstants = PersonUtils.getModeConstants(person);
            if (modeConstants == null) {
                modeConstants = new HashMap<>();
            }
            modeConstants.put(mode, Double.toString(RandomFromDistribution.nextLogNormalFromMeanAndSigma(splittableRandom, mean, sigma)));
            PersonUtils.setModeConstants(person, modeConstants);
        });
    }
}
