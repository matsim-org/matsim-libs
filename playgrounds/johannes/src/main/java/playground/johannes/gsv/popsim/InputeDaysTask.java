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

package playground.johannes.gsv.popsim;

import playground.johannes.coopsim.mental.choice.ChoiceSet;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.processing.PersonTask;

import java.util.Collection;
import java.util.Map;

/**
 * @author johannes
 */
public class InputeDaysTask implements PersonTask {

    private Map<String, ChoiceSet<String>> map;


    public InputeDaysTask(Collection<? extends Person> persons) {
        KeyMatrix matrix = new KeyMatrix();

        for(Person person : persons) {
            String day = person.getAttribute(CommonKeys.DAY);
            if(day != null) {
                for(Episode episode : person.getEpisodes()) {
                    for(Segment leg : episode.getLegs()) {
                        String purpose = leg.getAttribute(CommonKeys.LEG_PURPOSE);
                        if(purpose != null) {
                            matrix.add(day, purpose, 1.0);
                        }
                    }
                }
            }
        }


    }

    @Override
    public void apply(Person person) {

    }
}
