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

package playground.johannes.gsv.popsim.analysis;

import playground.johannes.gsv.popsim.ActTypePredicate;
import playground.johannes.synpop.analysis.Predicate;
import playground.johannes.synpop.data.*;

import java.util.*;

/**
 * @author johannes
 */
public class Predicates {

    public static Map<String, Predicate<Segment>> actTypePredicates(Collection<? extends Person> persons) {
        return actTypePredicates(persons, false);
    }

    public static Map<String, Predicate<Segment>> actTypePredicates(Collection<? extends Person> persons, boolean
            ignoreHome) {
        Set<String> acttypes = new HashSet<>();

        for(Person person : persons) {
            for(Episode episode : person.getEpisodes()) {
                for(Segment act : episode.getActivities()) {
                    String type = act.getAttribute(CommonKeys.ACTIVITY_TYPE);
                    if(type != null ) {
                        if(ignoreHome) {
                            if(!ActivityTypes.HOME.equalsIgnoreCase(type))
                                acttypes.add(type);
                        } else
                            acttypes.add(type);
                    }
                }
            }
        }

        Map<String, Predicate<Segment>> predicates = new HashMap<>();
        for(String type : acttypes) {
            predicates.put(type, new ActTypePredicate(type));
        }

        return predicates;
    }

    public static Map<String, Predicate<Segment>> legPurposePredicates(Collection<? extends Person> persons) {
        Map<String, Predicate<Segment>> actPredicates = actTypePredicates(persons);
        Map<String, Predicate<Segment>> legPredicates = new HashMap<>();

        for(Map.Entry<String, Predicate<Segment>> entry : actPredicates.entrySet()) {
            legPredicates.put(entry.getKey(), new LegPurposePredicate(entry.getValue()));
        }

        return legPredicates;
    }

    public static Map<String, Predicate<Segment>> legModePredicates(Collection<? extends Person> persons) {
        Set<String> modes = new HashSet<>();

        for(Person person : persons) {
            for(Episode episode : person.getEpisodes()) {
                for(Segment leg : episode.getLegs()) {
                    String mode = leg.getAttribute(CommonKeys.LEG_MODE);
                    if(mode != null)
                        modes.add(mode);
                }
            }
        }

        Map<String, Predicate<Segment>> predicates = new HashMap<>();
        for(String mode : modes) {
            predicates.put(mode, new ModePredicate(mode));
        }

        return predicates;
    }

    public static Map<String, Predicate<Segment>> legPredicates(Collection<? extends Person> persons) {
        Map<String, Predicate<Segment>> actTypePredicates = legPurposePredicates(persons);
        Map<String, Predicate<Segment>> legModePredicates = legModePredicates(persons);

        actTypePredicates.put("all", TrueLegPredicate.getInstance());
        legModePredicates.put("all", TrueLegPredicate.getInstance());

        Map<String, Predicate<Segment>> predicates = new HashMap<>();

        for(Map.Entry<String, Predicate<Segment>> modeEntry : legModePredicates.entrySet()) {
            for(Map.Entry<String, Predicate<Segment>> actEntry : actTypePredicates.entrySet()) {
                LegPurposePredicate purposePredicate = new LegPurposePredicate(actEntry.getValue());

                PredicateAndComposite<Segment> composite = new PredicateAndComposite<>();
                composite.addComponent(modeEntry.getValue());
                composite.addComponent(purposePredicate);

                predicates.put(String.format("%s.%s", modeEntry.getKey(), actEntry.getKey()), composite);
            }

        }

        return predicates;
    }

    public static class TrueLegPredicate implements Predicate<Segment> {

        private static TrueLegPredicate instance;

        public static TrueLegPredicate getInstance() {
            if(instance == null) instance = new TrueLegPredicate();
            return instance;
        }

        @Override
        public boolean test(Segment segment) {
            return true;
        }
    }
}
