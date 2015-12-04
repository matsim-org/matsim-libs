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

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.matsim.contrib.common.util.XORShiftRandom;
import playground.johannes.coopsim.mental.choice.ChoiceSet;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.processing.PersonTask;
import playground.johannes.synpop.source.mid2008.MiDValues;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 */
public class InputeDaysTask implements PersonTask {

    private Map<String, ChoiceSet<String>> map;


    public InputeDaysTask(Collection<? extends Person> persons) {
        Map<String, TObjectIntHashMap<String>> matrix = new HashMap<>();

        for(Person person : persons) {
            String day = person.getAttribute(CommonKeys.DAY);
            if(day != null) {
                for(Episode episode : person.getEpisodes()) {
                    for(Segment leg : episode.getLegs()) {
                        String mode = leg.getAttribute(CommonKeys.LEG_MODE);
//                        if(CommonValues.LEG_MODE_CAR.equalsIgnoreCase(mode)) {
                            String purpose = leg.getAttribute(CommonKeys.LEG_PURPOSE);
                            if (purpose != null) {
                                TObjectIntHashMap<String> days = matrix.get(purpose);
                                if (days == null) {
                                    days = new TObjectIntHashMap<>();
                                    matrix.put(purpose, days);
                                }
                                days.adjustOrPutValue(day, 1, 1);
//                            }
                        }
                    }
                }
            }
        }

        matrix.put(ActivityTypes.VACATIONS_LONG, matrix.get(ActivityTypes.VACATIONS_SHORT));

        map = new HashMap<>();

        for(Map.Entry<String, TObjectIntHashMap<String>> entry : matrix.entrySet()) {
            System.out.print(entry.getKey());
            System.out.print(": ");

            ChoiceSet<String> choiceSet = new ChoiceSet<>(new XORShiftRandom());

            TObjectIntHashMap<String> days = entry.getValue();
            TObjectIntIterator<String> it = days.iterator();
            for(int i = 0; i < days.size(); i++) {
                it.advance();
                choiceSet.addChoice(it.key(), it.value());

                System.out.print(it.key());
                System.out.print("=");
                System.out.print(String.valueOf(it.value()));
                System.out.print(" ");
            }
            System.out.println();

            map.put(entry.getKey(), choiceSet);
        }


    }

    @Override
    public void apply(Person person) {
        String day = person.getAttribute(CommonKeys.DAY);
        if(day == null) {
            Episode episode = person.getEpisodes().get(0);
            if(MiDValues.MID_JOUNREYS.equalsIgnoreCase(episode.getAttribute(CommonKeys.DATA_SOURCE))) {
                Segment leg = episode.getLegs().get(0);
                String purpose = leg.getAttribute(CommonKeys.LEG_PURPOSE);

                day = map.get(purpose).randomWeightedChoice();

                person.setAttribute(CommonKeys.DAY, day);
            }
        }
    }
}
