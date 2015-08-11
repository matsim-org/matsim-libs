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

package playground.johannes.gsv.matrices.episodes2matrix;

import org.apache.log4j.Logger;
import playground.johannes.gsv.matrices.plans2matrix.ReplaceMiscType;
import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.gsv.synPop.mid.MIDKeys;
import playground.johannes.gsv.synPop.mid.run.ProxyTaskRunner;
import playground.johannes.gsv.synPop.sim3.RestoreActTypes;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.io.KeyMatrixTxtIO;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.data.Segment;

import java.io.IOException;
import java.util.*;


/**
 * @author johannes
 */
public class Episodes2Matrix {

    private static final Logger logger = Logger.getLogger(Episodes2Matrix.class);

    public static final String DIMIDO = "dimido";

    public static final String WINTER = "win";

    public static final String SUMMER = "sum";

    private static final String DIM_SEPARATOR = ".";

    private static final String DBG_TOUCHED = "dbg_touched";

    public static void main(String[] args) throws IOException {
        String in = args[0];
        String rootDir = args[1];

        logger.info("Loading persons...");
        XMLParser reader = new XMLParser();
        reader.setValidating(false);
        reader.parse(in);
        Collection<PlainPerson> persons = reader.getPersons();
        logger.info(String.format("Loaded %s persons.", persons.size()));

        logger.info("Restoring original activity types...");
        ProxyTaskRunner.run(new RestoreActTypes(), persons, true);
        logger.info("Replacing misc types...");
        new ReplaceMiscType().apply(persons);
        logger.info("Imputing month attributes...");
        new ImputeMonth().apply(persons);
        logger.info("Assigning leg purposes...");
        ProxyTaskRunner.run(new SetLegPurposes(), persons);
        logger.info("Inferring wecommuter purpose...");
        ProxyTaskRunner.run(new InfereWeCommuter(100000), persons);

        Map<String, LegPredicate> modePreds = new LinkedHashMap<>();
        modePreds.put("car", new LegKeyValuePredicate(CommonKeys.LEG_MODE, "car"));

        Map<String, LegPredicate> purposePreds = new LinkedHashMap<>();
        purposePreds.put(ActivityType.BUSINESS, new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, ActivityType.BUSINESS));
        purposePreds.put(ActivityType.EDUCATION, new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, ActivityType.EDUCATION));

        PredicateORComposite leisurePred = new PredicateORComposite();
        leisurePred.addComponent(new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, ActivityType.LEISURE));
        leisurePred.addComponent(new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, "visit"));
        leisurePred.addComponent(new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, "gastro"));
        leisurePred.addComponent(new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, "culture"));
        leisurePred.addComponent(new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, "private"));
        leisurePred.addComponent(new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, "pickdrop"));
        leisurePred.addComponent(new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, "sport"));

        purposePreds.put(ActivityType.LEISURE, leisurePred);
        purposePreds.put(ActivityType.SHOP, new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, ActivityType.SHOP));
        purposePreds.put(ActivityType.WORK, new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, ActivityType.WORK));
        purposePreds.put(ActivityType.VACATIONS_SHORT, new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, ActivityType
                .VACATIONS_SHORT));
        purposePreds.put(ActivityType.VACATIONS_LONG, new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, ActivityType.VACATIONS_LONG));
        purposePreds.put(InfereWeCommuter.WECOMMUTER, new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, InfereWeCommuter
                .WECOMMUTER));

        Map<String, LegPredicate> dayPreds = new LinkedHashMap<>();
        dayPreds.put(CommonKeys.MONDAY, new PersonKeyValuePredicate(CommonKeys.DAY, CommonKeys.MONDAY));
        dayPreds.put(CommonKeys.FRIDAY, new PersonKeyValuePredicate(CommonKeys.DAY, CommonKeys.FRIDAY));
        dayPreds.put(CommonKeys.SATURDAY, new PersonKeyValuePredicate(CommonKeys.DAY, CommonKeys.SATURDAY));
        dayPreds.put(CommonKeys.SUNDAY, new PersonKeyValuePredicate(CommonKeys.DAY, CommonKeys.SUNDAY));
        PredicateORComposite dimidoPred = new PredicateORComposite();
        dimidoPred.addComponent(new PersonKeyValuePredicate(CommonKeys.DAY, CommonKeys.TUESDAY));
        dimidoPred.addComponent(new PersonKeyValuePredicate(CommonKeys.DAY, CommonKeys.WEDNESDAY));
        dimidoPred.addComponent(new PersonKeyValuePredicate(CommonKeys.DAY, CommonKeys.THURSDAY));
        dayPreds.put(DIMIDO, dimidoPred);

        Map<String, LegPredicate> seasonPreds = new LinkedHashMap<>();
        PredicateORComposite summerPred = new PredicateORComposite();
        summerPred.addComponent(new PersonKeyValuePredicate(MIDKeys.PERSON_MONTH, MIDKeys.APRIL));
        summerPred.addComponent(new PersonKeyValuePredicate(MIDKeys.PERSON_MONTH, MIDKeys.MAY));
        summerPred.addComponent(new PersonKeyValuePredicate(MIDKeys.PERSON_MONTH, MIDKeys.JUNE));
        summerPred.addComponent(new PersonKeyValuePredicate(MIDKeys.PERSON_MONTH, MIDKeys.JULY));
        summerPred.addComponent(new PersonKeyValuePredicate(MIDKeys.PERSON_MONTH, MIDKeys.AUGUST));
        summerPred.addComponent(new PersonKeyValuePredicate(MIDKeys.PERSON_MONTH, MIDKeys.SEPTEMBER));
        summerPred.addComponent(new PersonKeyValuePredicate(MIDKeys.PERSON_MONTH, MIDKeys.OCTOBER));
        seasonPreds.put(SUMMER, summerPred);
        PredicateORComposite winterPred = new PredicateORComposite();
        winterPred.addComponent(new PersonKeyValuePredicate(MIDKeys.PERSON_MONTH, MIDKeys.NOVEMBER));
        winterPred.addComponent(new PersonKeyValuePredicate(MIDKeys.PERSON_MONTH, MIDKeys.DECEMBER));
        winterPred.addComponent(new PersonKeyValuePredicate(MIDKeys.PERSON_MONTH, MIDKeys.JANUARY));
        winterPred.addComponent(new PersonKeyValuePredicate(MIDKeys.PERSON_MONTH, MIDKeys.FEBRUARY));
        winterPred.addComponent(new PersonKeyValuePredicate(MIDKeys.PERSON_MONTH, MIDKeys.MARCH));
        seasonPreds.put(WINTER, winterPred);

        Map<String, LegPredicate> directionPreds = new LinkedHashMap<>();
        directionPreds.put(DirectionPredicate.OUTWARD, new DirectionPredicate(DirectionPredicate.OUTWARD));
        directionPreds.put(DirectionPredicate.RETURN, new DirectionPredicate(DirectionPredicate.RETURN));
        directionPreds.put(DirectionPredicate.INTERMEDIATE, new DirectionPredicate(DirectionPredicate.INTERMEDIATE));

        logger.info("Extracting full matrix...");
        KeyMatrixTxtIO.write(getMatrix(persons, modePreds.get("car")), String.format("%s/car.txt.gz", rootDir));

        for (Map.Entry<String, LegPredicate> mode : modePreds.entrySet()) {
            for (Map.Entry<String, LegPredicate> purpose : purposePreds.entrySet()) {
                for (Map.Entry<String, LegPredicate> day : dayPreds.entrySet()) {
                    for (Map.Entry<String, LegPredicate> season : seasonPreds.entrySet()) {
                        for (Map.Entry<String, LegPredicate> direction : directionPreds.entrySet()) {

                            PredicateANDComposite comp = new PredicateANDComposite();
                            comp.addComponent(mode.getValue());
                            comp.addComponent(purpose.getValue());
                            comp.addComponent(day.getValue());
                            comp.addComponent(season.getValue());
                            comp.addComponent(direction.getValue());

                            StringBuilder builder = new StringBuilder();
                            builder.append(mode.getKey());
                            builder.append(DIM_SEPARATOR);
                            builder.append(purpose.getKey());
                            builder.append(DIM_SEPARATOR);
                            builder.append(day.getKey());
                            builder.append(DIM_SEPARATOR);
                            builder.append(season.getKey());
                            builder.append(DIM_SEPARATOR);
                            builder.append(direction.getKey());

                            logger.info(String.format("Extracting matrix %s...", builder.toString()));
                            KeyMatrix m = getMatrix(persons, comp);

                            String out = String.format("%s/%s.txt.gz", rootDir, builder.toString());
                            KeyMatrixTxtIO.write(m, out);
                        }
                    }
                }
            }
        }

        int cnt = 0;
        for(Person person : persons) {
            for(Episode episode : person.getEpisodes()) {
                for(Segment leg : episode.getLegs()) {
                    String touched = leg.getAttribute(DBG_TOUCHED);
                    if(touched == null) {
                        leg.setAttribute(DBG_TOUCHED, "0");
                        cnt++;
                    }
                }
            }
        }
        logger.info(String.format("%s trips are untouched.", cnt));

        XMLWriter writer = new XMLWriter();
        writer.write(rootDir + "/plans.xml.gz", persons);
        logger.info("Done.");
    }

    private static KeyMatrix getMatrix(Collection<PlainPerson> persons, LegPredicate pred) {
        KeyMatrix m = new KeyMatrix();

        for (Person person : persons) {
            for (Episode episode : person.getEpisodes()) {
                for (int i = 0; i < episode.getLegs().size(); i++) {
                    Segment leg = episode.getLegs().get(i);
                    if (pred.test(leg)) {
                        Segment prev = episode.getActivities().get(i);
                        Segment next = episode.getActivities().get(i + 1);

                        String origin = prev.getAttribute(SetZones.ZONE_KEY);
                        String dest = next.getAttribute(SetZones.ZONE_KEY);

                        if(origin != null && dest != null) {
                            m.add(origin, dest, 1);

                            String touched = leg.getAttribute(DBG_TOUCHED);
                            int cnt = 0;
                            if(touched != null) cnt = Integer.parseInt(touched);
                            leg.setAttribute(DBG_TOUCHED, String.valueOf(cnt + 1));
                        }
                    }
                }
            }
        }

        return m;
    }
}
