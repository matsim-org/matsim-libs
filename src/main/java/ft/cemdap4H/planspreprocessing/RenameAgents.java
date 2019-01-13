/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 *
 */
package ft.cemdap4H.planspreprocessing;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import playground.jbischoff.utils.JbUtils;

import java.util.Map;
import java.util.Map.Entry;

/**
 * @author jbischoff
 */

/**
 * renames the agents according to their home (and work) location, for an easier later filtering
 */
public class RenameAgents {
    Map<String, Geometry> locationShape;
    private CoordinateTransformation ct;

    public static void main(String args[]) {
//	new RenameAgents().run();
    }

    /**
     *
     */
    public void run(String shapeFile, String inputpopfile, String outputpopfile, String keyInShape, CoordinateTransformation ct) {
        locationShape = JbUtils.readShapeFileAndExtractGeometry(shapeFile, keyInShape);
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        this.ct = ct;
        new PopulationReader(scenario).readFile(inputpopfile);
        Population pop2 = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        for (Person p : scenario.getPopulation().getPersons().values()) {
            String homeLocation = null;
            String workLocation = null;
            Plan plan = p.getSelectedPlan();
            for (PlanElement pe : plan.getPlanElements()) {
                if (pe instanceof Activity) {
                    if (((Activity) pe).getType().startsWith("home")) {
                        if (homeLocation == null) {
                            homeLocation = findZone(((Activity) pe).getCoord());
                        }
                    }
                    if (((Activity) pe).getType().startsWith("work") || ((Activity) pe).getType().startsWith("education")) {
                        if (workLocation == null) {
                            workLocation = findZone(((Activity) pe).getCoord());
                        }
                    }
                }

            }
            if (homeLocation == null) homeLocation = "na";
            if (workLocation == null) workLocation = "na";
            Id<Person> newPersonId = Id.createPersonId(homeLocation + "_" + workLocation + "_" + p.getId());
            Person p2 = pop2.getFactory().createPerson(newPersonId);
            PersonUtils.setAge(p2, PersonUtils.getAge(p));
            //If person has license --> car is available
            //ToDo Share of car / license avail
            PersonUtils.setCarAvail(p2, PersonUtils.getLicense(p));
            PersonUtils.setEmployed(p2, PersonUtils.isEmployed(p));
            PersonUtils.setSex(p2, PersonUtils.getSex(p));
            PersonUtils.setLicence(p2, PersonUtils.getLicense(p));
            for (Plan plans : p.getPlans()) {
                p2.addPlan(plans);
            }
            pop2.addPerson(p2);
        }
        new PopulationWriter(pop2).write(outputpopfile);
    }

    /**
     * @param coord
     * @return
     */
    private String findZone(Coord coord) {
        Point p = MGC.coord2Point(ct.transform(coord));
        for (Entry<String, Geometry> e : this.locationShape.entrySet()) {
            if (e.getValue().contains(p)) {
                return e.getKey();
            }
        }

        return null;
    }

}
