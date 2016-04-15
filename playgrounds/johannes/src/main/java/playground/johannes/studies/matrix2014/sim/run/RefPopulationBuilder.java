/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,       *
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
package playground.johannes.studies.matrix2014.sim.run;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.collections.ChoiceSet;
import org.matsim.core.config.Config;
import playground.johannes.gsv.synPop.mid.Route2GeoDistance;
import playground.johannes.gsv.synPop.sim3.ReplaceActTypes;
import playground.johannes.studies.matrix2014.analysis.LabeledLegHistogramBuilder;
import playground.johannes.studies.matrix2014.sim.ValidatePersonWeight;
import playground.johannes.synpop.analysis.AttributeProvider;
import playground.johannes.synpop.analysis.NotPredicate;
import playground.johannes.synpop.analysis.Predicate;
import playground.johannes.synpop.analysis.PredicateAndComposite;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.PopulationIO;
import playground.johannes.synpop.processing.EpisodeTask;
import playground.johannes.synpop.processing.GuessMissingActTypes;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.synpop.processing.ValidateMissingAttribute;

import java.util.*;

/**
 * @author jillenberger
 */
public class RefPopulationBuilder {

    private static final Logger logger = Logger.getLogger(RefPopulationBuilder.class);

    public static Set<? extends Person> build(Simulator engine, Config config) {
        logger.info("Loading persons...");
        Set<Person> refPersons = PopulationIO.loadFromXML(config.findParam(engine.MODULE_NAME, "popInputFile"), new PlainFactory());
        logger.info(String.format("Loaded %s persons.", refPersons.size()));

        logger.info("Preparing reference simulation...");
        TaskRunner.validatePersons(new ValidateMissingAttribute(CommonKeys.PERSON_WEIGHT), refPersons);
        TaskRunner.validatePersons(new ValidatePersonWeight(), refPersons);

        TaskRunner.run(new SetVacationsPurpose(), refPersons);
        TaskRunner.run(new ReplaceHomePurpose(), refPersons);
        TaskRunner.run(new NullifyPurpose(ActivityTypes.HOME), refPersons);
        TaskRunner.run(new NullifyPurpose(ActivityTypes.MISC), refPersons);
        TaskRunner.run(new ReplaceLegPurposes(), refPersons);
        TaskRunner.run(new GuessMissingPurposes(refPersons, engine.getLegPredicate(), engine.getRandom()), refPersons);

        TaskRunner.run(new ReplaceActTypes(), refPersons);
        new GuessMissingActTypes(engine.getRandom()).apply(refPersons);
        TaskRunner.run(new Route2GeoDistance(new playground.johannes.studies.matrix2014.sim.Simulator.Route2GeoDistFunction()), refPersons);

        return refPersons;
    }

    public static class SetVacationsPurpose implements EpisodeTask {

        @Override
        public void apply(Episode episode) {
            for(Segment act : episode.getActivities()) {
                if(ActivityTypes.VACATIONS_LONG.equalsIgnoreCase(act.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
                    replace(act, ActivityTypes.VACATIONS_LONG);
                }

                if(ActivityTypes.VACATIONS_SHORT.equalsIgnoreCase(act.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
                    replace(act, ActivityTypes.VACATIONS_SHORT);
                }
            }
        }

        private void replace(Segment act, String type) {
            Segment prev = act.previous();
            Segment next = act.next();

            if(prev != null) {
                prev.setAttribute(CommonKeys.LEG_PURPOSE, type);
            }

            if(next != null) {
                next.setAttribute(CommonKeys.LEG_PURPOSE, type);
            }
        }
    }

    public static class ReplaceHomePurpose implements EpisodeTask {

        @Override
        public void apply(Episode episode) {
            for(Segment leg : episode.getLegs()) {
                if(ActivityTypes.HOME.equalsIgnoreCase(leg.getAttribute(CommonKeys.LEG_PURPOSE))) {
                    Segment prev = leg.previous();
                    leg.setAttribute(CommonKeys.LEG_PURPOSE, prev.getAttribute(CommonKeys.ACTIVITY_TYPE));
                }
            }
        }
    }

    public static class ReplaceLegPurposes implements EpisodeTask {

        private Map<String, String> mapping;

        private Map<String, String> getMapping() {
            if(mapping == null) {
                mapping = new HashMap<>();

                mapping.put(ActivityTypes.PRIVATE, ActivityTypes.LEISURE);
                mapping.put(ActivityTypes.PICKDROP, ActivityTypes.LEISURE);
                mapping.put(ActivityTypes.GASTRO, ActivityTypes.LEISURE);
                mapping.put(ActivityTypes.CULTURE, ActivityTypes.LEISURE);
                mapping.put(ActivityTypes.VISIT, ActivityTypes.LEISURE);
            }

            return mapping;
        }

        @Override
        public void apply(Episode episode) {
            for(Segment leg : episode.getLegs()) {
                String purpose = leg.getAttribute(CommonKeys.LEG_PURPOSE);
                String replace = getMapping().get(purpose);
                if(replace != null) leg.setAttribute(CommonKeys.LEG_PURPOSE, replace);
            }
        }
    }

    public static class NullifyPurpose implements EpisodeTask {

        private final String purpose;

        public NullifyPurpose(String purpose) {
            this.purpose = purpose;
        }

        @Override
        public void apply(Episode episode) {
            for(Segment leg : episode.getLegs()) {
                if(purpose.equalsIgnoreCase(leg.getAttribute(CommonKeys.LEG_PURPOSE))) {
                    leg.setAttribute(CommonKeys.LEG_PURPOSE, null);
                }
            }
        }
    }

    public static class GuessMissingPurposes implements EpisodeTask {

        private ChoiceSet<String> shortChoiceSet;

        private ChoiceSet<String> longChoiceSet;

        private Predicate<Segment> distancePredicate;

        public GuessMissingPurposes(Collection<? extends Person> refPersons, Predicate<Segment> predicate, Random random) {
            LabeledLegHistogramBuilder builder = new LabeledLegHistogramBuilder(new AttributeProvider<Segment>(CommonKeys.LEG_PURPOSE));
            /*
            short distances
             */
            distancePredicate = new ShortDistancePredicate();
            builder.setPredicate(PredicateAndComposite.create(predicate, distancePredicate));
            TObjectDoubleMap<String> hist = builder.build(refPersons);

            shortChoiceSet = new ChoiceSet<>(random);
            TObjectDoubleIterator<String> it = hist.iterator();
            for(int i = 0; i < hist.size(); i++) {
                it.advance();
                shortChoiceSet.addOption(it.key(), it.value());
            }
            /*
            long distances
             */
            builder.setPredicate(PredicateAndComposite.create(predicate, new NotPredicate<>(distancePredicate)));
            hist = builder.build(refPersons);

            longChoiceSet = new ChoiceSet<>(random);
            it = hist.iterator();
            for(int i = 0; i < hist.size(); i++) {
                it.advance();
                longChoiceSet.addOption(it.key(), it.value());
            }
        }

        @Override
        public void apply(Episode episode) {
            for(Segment leg : episode.getLegs()) {
                if(leg.getAttribute(CommonKeys.LEG_PURPOSE) == null) {
                    String purpose;
                    if(distancePredicate.test(leg))
                        purpose = shortChoiceSet.randomWeightedChoice();
                    else
                        purpose = longChoiceSet.randomWeightedChoice();

                    leg.setAttribute(CommonKeys.LEG_PURPOSE, purpose);
                }
            }
        }

        public static class ShortDistancePredicate implements Predicate<Segment> {

            @Override
            public boolean test(Segment segment) {
                String val = segment.getAttribute(CommonKeys.LEG_GEO_DISTANCE);
                if(val != null) {
                    double dist = Double.parseDouble(val);
                    if(dist < 100000) return true;
                    else return false;
                }

                return true;
            }
        }
    }
}
