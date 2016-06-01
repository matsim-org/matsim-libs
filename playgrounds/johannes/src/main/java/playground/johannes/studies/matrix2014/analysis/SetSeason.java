/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.johannes.studies.matrix2014.analysis;

import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.processing.PersonTask;
import playground.johannes.synpop.source.mid2008.MiDKeys;
import playground.johannes.synpop.source.mid2008.MiDValues;

/**
 * @author johannes
 */
public class SetSeason implements PersonTask {

    public static final String SEASON_KEY = "season";

    public static final String WINTER = "winter";

    public static final String SUMMER = "summer";

    @Override
    public void apply(Person person) {
        String month = person.getAttribute(MiDKeys.PERSON_MONTH);
        if(month != null) {
            String season = SUMMER;
            if (MiDValues.NOVEMBER.equalsIgnoreCase(month) ||
                    MiDValues.DECEMBER.equalsIgnoreCase(month) ||
                    MiDValues.JANUARY.equalsIgnoreCase(month) ||
                    MiDValues.FEBRUARY.equalsIgnoreCase(month) ||
                    MiDValues.MARCH.equalsIgnoreCase(month)) {
                season = WINTER;
            }

            person.setAttribute(SEASON_KEY, season);
        }
    }
}
