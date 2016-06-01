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

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import playground.johannes.studies.matrix2014.analysis.ZoneMobilityRate;
import playground.johannes.studies.matrix2014.gis.TransferZoneAttribute;
import playground.johannes.studies.matrix2014.sim.SetLAU2Attribute;
import playground.johannes.synpop.analysis.Predicate;
import playground.johannes.synpop.analysis.TripsCounter;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.PopulationIO;
import playground.johannes.synpop.gis.*;
import playground.johannes.synpop.processing.CalculateGeoDistance;
import playground.johannes.synpop.processing.LegAttributeRemover;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.synpop.sim.SetActivityFacilities;
import playground.johannes.synpop.sim.SetHomeFacilities;
import playground.johannes.synpop.source.mid2008.MiDKeys;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

/**
 * @author jillenberger
 */
public class SimPopulationBuilder {

    private static final Logger logger = Logger.getLogger(SimPopulationBuilder.class);

    public static Set<? extends Person> build(Simulator engine, Config config) {
        String simPopFile = config.findParam(Simulator.MODULE_NAME, "simPopulation");
        DataPool dataPool = engine.getDataPool();

        Set<Person> simPersons;
        if (simPopFile == null) {

            int size = (int) Double.parseDouble(config.getParam(Simulator.MODULE_NAME, "populationSize"));
            simPersons = (Set<Person>) clonePersons(engine.getRefPersons(),
                    size,
                    0.5,
                    engine.getLegPredicate(),
                    engine.getRandom());
            logger.info(String.format("Generated %s persons.", simPersons.size()));
            /*
            Initializing simulation population...
            */
            logger.info("Assigning home locations...");
            boolean useZoneWeights = true;
            String val = config.findParam(Simulator.MODULE_NAME, "useZoneWeights");
            if (val != null) {
                useZoneWeights = Boolean.parseBoolean(val);
            }

            SetHomeFacilities setHomeFacilities = new SetHomeFacilities(dataPool, "modena", engine.getRandom());

            if (useZoneWeights) {
                ZoneCollection lau2Zones = ((ZoneData) dataPool.get(ZoneDataLoader.KEY)).getLayer("lau2");
                ZoneCollection modenaZones = ((ZoneData) dataPool.get(ZoneDataLoader.KEY)).getLayer("modena");

                ZoneMobilityRate zoneMobilityRate = new ZoneMobilityRate(
                        MiDKeys.PERSON_LAU2_CLASS,
                        lau2Zones,
                        engine.getLegPredicate());
                zoneMobilityRate.analyze(engine.getRefPersons(), null);

                new TransferZoneAttribute().apply(lau2Zones, modenaZones, MiDKeys.PERSON_LAU2_CLASS);
                setHomeFacilities.setZoneWeights(zoneMobilityRate.getMobilityRatePerZone(modenaZones));
            }

            setHomeFacilities.apply(simPersons);

            logger.info("Assigning random activity locations...");
            TaskRunner.run(new SetActivityFacilities((FacilityData) dataPool.get(FacilityDataLoader.KEY)), simPersons);
        } else {
            logger.info("Loading sim population from file...");
            simPersons = PopulationIO.loadFromXML(simPopFile, new PlainFactory());
            //TODO: temp fix!
            TaskRunner.run(new RefPopulationBuilder.SetVacationsPurpose(), simPersons);
            TaskRunner.run(new RefPopulationBuilder.ReplaceHomePurpose(), simPersons);
            TaskRunner.run(new RefPopulationBuilder.NullifyPurpose(ActivityTypes.HOME), simPersons);
            TaskRunner.run(new RefPopulationBuilder.NullifyPurpose(ActivityTypes.MISC), simPersons);
            TaskRunner.run(new RefPopulationBuilder.ReplaceLegPurposes(), simPersons);
            TaskRunner.run(new RefPopulationBuilder.GuessMissingPurposes(simPersons, engine.getLegPredicate(), engine.getRandom()), simPersons);
        }

        logger.info("Recalculate geo distances...");
        TaskRunner.run(new LegAttributeRemover(CommonKeys.LEG_GEO_DISTANCE), simPersons);
        TaskRunner.run(new CalculateGeoDistance((FacilityData) dataPool.get(FacilityDataLoader.KEY)), simPersons);

        logger.info("Resetting LAU2Class attributes...");
        SetLAU2Attribute lTask = new SetLAU2Attribute(dataPool, "lau2");
        TaskRunner.run(lTask, simPersons);
        if (lTask.getErrors() > 0)
            logger.warn(String.format("Cannot set LAU2Class attribute for %s persons.", lTask.getErrors()));

        return simPersons;
    }

    private static Collection<Person> clonePersons(Collection<? extends Person> refPersons,
                                                   int size,
                                                   double proba,
                                                   Predicate<Segment> legPredicate,
                                                   Random random) {
        double wsum1 = 0;
        double wsum2 = 0;

        Set<Person> persons1 = new LinkedHashSet<>();
        Set<Person> persons2 = new LinkedHashSet<>();

        TripsCounter counter = new TripsCounter(legPredicate);
        for (Person p : refPersons) {
            double w = Double.parseDouble(p.getAttribute(CommonKeys.PERSON_WEIGHT));
            if (counter.get(p.getEpisodes().get(0)) == 1) {
                wsum2 += w;
                persons2.add(p);
            } else {
                wsum1 += w;
                persons1.add(p);
            }
        }

        int n1 = (int) (size * proba);
        int n2 = size - n1;

        double w1 = (wsum1 * n2) / (wsum2 * n1);
        double w2 = 1.0;

        logger.info(String.format("Cloning sim persons with weight %s...", w1));
        Set<? extends Person> simPersons1 = PersonUtils.weightedCopy(persons1, new PlainFactory(), n1, random);
        logger.info(String.format("Cloning sim persons with weight %s...", w2));
        Set<? extends Person> simPersons2 = PersonUtils.weightedCopy(persons2, new PlainFactory(), n2, random);

        for (Person p : simPersons1) p.setAttribute(CommonKeys.PERSON_WEIGHT, String.valueOf(w1));
        for (Person p : simPersons2) p.setAttribute(CommonKeys.PERSON_WEIGHT, String.valueOf(w2));

        Set<Person> all = new LinkedHashSet<>();
        all.addAll(simPersons1);
        all.addAll(simPersons2);

        return all;
    }
}
