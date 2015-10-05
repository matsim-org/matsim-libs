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

package playground.johannes.synpop.processing;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;
import playground.johannes.coopsim.mental.choice.ChoiceSet;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * @author johannes
 */
public class GuessMissingActTypes implements PersonsTask {

    private final Random random;

    public GuessMissingActTypes(Random random) {
        this.random = random;
    }

    @Override
    public void apply(Collection<? extends Person> persons) {
        TObjectIntHashMap<String> counts = new TObjectIntHashMap<>();
        List<Segment> activities = new LinkedList<>();

        for (Person p : persons) {
            for (Episode e : p.getEpisodes()) {
                for (Segment s : e.getActivities()) {
                    String type = s.getAttribute(CommonKeys.ACTIVITY_TYPE);
                    if (type != null) counts.adjustOrPutValue(type, 1, 1);
                    else activities.add(s);
                }
            }
        }

        ChoiceSet<String> set = new ChoiceSet<>(random);

        TObjectIntIterator<String> it = counts.iterator();
        for (int i = 0; i < counts.size(); i++) {
            it.advance();
            set.addChoice(it.key(), it.value());
        }

        for (Segment s : activities) {
            String type = s.getAttribute(CommonKeys.ACTIVITY_TYPE);
            if (type == null) {
                type = set.randomWeightedChoice();
                s.setAttribute(CommonKeys.ACTIVITY_TYPE, type);
            }
        }
    }
}
