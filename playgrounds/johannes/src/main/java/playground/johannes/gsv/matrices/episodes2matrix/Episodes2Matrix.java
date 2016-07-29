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
import playground.johannes.gsv.synPop.sim3.RestoreActTypes;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.XMLHandler;
import playground.johannes.synpop.data.io.XMLWriter;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixTxtIO;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.synpop.source.mid2008.MiDKeys;
import playground.johannes.synpop.source.mid2008.MiDValues;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


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
        XMLHandler reader = new XMLHandler(new PlainFactory());
        reader.setValidating(false);
        reader.readFile(in);
        Collection<PlainPerson> persons = (Set<PlainPerson>)reader.getPersons();
        logger.info(String.format("Loaded %s persons.", persons.size()));

        logger.info("Restoring original activity types...");
        TaskRunner.run(new RestoreActTypes(), persons, true);
        logger.info("Replacing misc types...");
        new ReplaceMiscType().apply(persons);
        logger.info("Imputing month attributes...");
        new ImputeMonth().apply(persons);
        logger.info("Assigning leg purposes...");
        TaskRunner.run(new SetLegPurposes(), persons);
        logger.info("Inferring wecommuter purpose...");
        TaskRunner.run(new InfereWeCommuter(100000), persons);

        Map<String, LegPredicate> modePreds = new LinkedHashMap<>();
        modePreds.put("car", new LegKeyValuePredicate(CommonKeys.LEG_MODE, "car"));

        Map<String, LegPredicate> purposePreds = new LinkedHashMap<>();
        purposePreds.put(ActivityTypes.BUSINESS, new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, ActivityTypes.BUSINESS));
        purposePreds.put(ActivityTypes.EDUCATION, new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, ActivityTypes.EDUCATION));

        PredicateORComposite leisurePred = new PredicateORComposite();
        leisurePred.addComponent(new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, ActivityTypes.LEISURE));
        leisurePred.addComponent(new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, "visit"));
        leisurePred.addComponent(new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, "gastro"));
        leisurePred.addComponent(new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, "culture"));
        leisurePred.addComponent(new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, "private"));
        leisurePred.addComponent(new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, "pickdrop"));
        leisurePred.addComponent(new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, "sport"));

        purposePreds.put(ActivityTypes.LEISURE, leisurePred);
        purposePreds.put(ActivityTypes.SHOP, new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, ActivityTypes.SHOP));
        purposePreds.put(ActivityTypes.WORK, new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, ActivityTypes.WORK));
        purposePreds.put(ActivityTypes.VACATIONS_SHORT, new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, ActivityTypes
                .VACATIONS_SHORT));
        purposePreds.put(ActivityTypes.VACATIONS_LONG, new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, ActivityTypes.VACATIONS_LONG));
        purposePreds.put(InfereWeCommuter.WECOMMUTER, new LegKeyValuePredicate(CommonKeys.LEG_PURPOSE, InfereWeCommuter
                .WECOMMUTER));

        Map<String, LegPredicate> dayPreds = new LinkedHashMap<>();
        dayPreds.put(CommonValues.MONDAY, new PersonKeyValuePredicate(CommonKeys.DAY, CommonValues.MONDAY));
        dayPreds.put(CommonValues.FRIDAY, new PersonKeyValuePredicate(CommonKeys.DAY, CommonValues.FRIDAY));
        dayPreds.put(CommonValues.SATURDAY, new PersonKeyValuePredicate(CommonKeys.DAY, CommonValues.SATURDAY));
        dayPreds.put(CommonValues.SUNDAY, new PersonKeyValuePredicate(CommonKeys.DAY, CommonValues.SUNDAY));
        PredicateORComposite dimidoPred = new PredicateORComposite();
        dimidoPred.addComponent(new PersonKeyValuePredicate(CommonKeys.DAY, CommonValues.TUESDAY));
        dimidoPred.addComponent(new PersonKeyValuePredicate(CommonKeys.DAY, CommonValues.WEDNESDAY));
        dimidoPred.addComponent(new PersonKeyValuePredicate(CommonKeys.DAY, CommonValues.THURSDAY));
        dayPreds.put(DIMIDO, dimidoPred);

        Map<String, LegPredicate> seasonPreds = new LinkedHashMap<>();
        PredicateORComposite summerPred = new PredicateORComposite();
        summerPred.addComponent(new PersonKeyValuePredicate(MiDKeys.PERSON_MONTH, MiDValues.APRIL));
        summerPred.addComponent(new PersonKeyValuePredicate(MiDKeys.PERSON_MONTH, MiDValues.MAY));
        summerPred.addComponent(new PersonKeyValuePredicate(MiDKeys.PERSON_MONTH, MiDValues.JUNE));
        summerPred.addComponent(new PersonKeyValuePredicate(MiDKeys.PERSON_MONTH, MiDValues.JULY));
        summerPred.addComponent(new PersonKeyValuePredicate(MiDKeys.PERSON_MONTH, MiDValues.AUGUST));
        summerPred.addComponent(new PersonKeyValuePredicate(MiDKeys.PERSON_MONTH, MiDValues.SEPTEMBER));
        summerPred.addComponent(new PersonKeyValuePredicate(MiDKeys.PERSON_MONTH, MiDValues.OCTOBER));
        seasonPreds.put(SUMMER, summerPred);
        PredicateORComposite winterPred = new PredicateORComposite();
        winterPred.addComponent(new PersonKeyValuePredicate(MiDKeys.PERSON_MONTH, MiDValues.NOVEMBER));
        winterPred.addComponent(new PersonKeyValuePredicate(MiDKeys.PERSON_MONTH, MiDValues.DECEMBER));
        winterPred.addComponent(new PersonKeyValuePredicate(MiDKeys.PERSON_MONTH, MiDValues.JANUARY));
        winterPred.addComponent(new PersonKeyValuePredicate(MiDKeys.PERSON_MONTH, MiDValues.FEBRUARY));
        winterPred.addComponent(new PersonKeyValuePredicate(MiDKeys.PERSON_MONTH, MiDValues.MARCH));
        seasonPreds.put(WINTER, winterPred);

        Map<String, LegPredicate> directionPreds = new LinkedHashMap<>();
        directionPreds.put(DirectionPredicate.OUTWARD, new DirectionPredicate(DirectionPredicate.OUTWARD));
        directionPreds.put(DirectionPredicate.RETURN, new DirectionPredicate(DirectionPredicate.RETURN));
        directionPreds.put(DirectionPredicate.INTERMEDIATE, new DirectionPredicate(DirectionPredicate.INTERMEDIATE));

        logger.info("Extracting full matrix...");
        NumericMatrixTxtIO.write(getMatrix(persons, modePreds.get("car")), String.format("%s/car.txt.gz", rootDir));

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
                            NumericMatrix m = getMatrix(persons, comp);

                            String out = String.format("%s/%s.txt.gz", rootDir, builder.toString());
                            NumericMatrixTxtIO.write(m, out);
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

    private static NumericMatrix getMatrix(Collection<PlainPerson> persons, LegPredicate pred) {
        NumericMatrix m = new NumericMatrix();

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
