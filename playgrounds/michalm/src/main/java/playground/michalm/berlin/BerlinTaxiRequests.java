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

package playground.michalm.berlin;

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.google.common.base.Predicate;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.prep.PreparedPolygon;


public class BerlinTaxiRequests
{
    public static boolean isWithinArea(Person person, PreparedPolygon preparedPolygon)
    {
        return isWithinArea(person.getPlans().get(0), preparedPolygon);
    }


    public static boolean isWithinArea(Plan plan, PreparedPolygon preparedPolygon)
    {
        Activity fromActivity = (Activity)plan.getPlanElements().get(0);
        Activity toActivity = (Activity)plan.getPlanElements().get(2);

        Point from = MGC.coord2Point(fromActivity.getCoord());
        Point to = MGC.coord2Point(toActivity.getCoord());
        return preparedPolygon.contains(from) && preparedPolygon.contains(to);
    }


    public static Predicate<Plan> createWithinAreaPredicate(MultiPolygon area)
    {
        final PreparedPolygon preparedPolygon = new PreparedPolygon(area);
        return new Predicate<Plan>() {
            public boolean apply(Plan plan)
            {
                return isWithinArea(plan, preparedPolygon);
            }
        };
    }


    private static final String DIR = "d:/svn-vsp/sustainability-w-michal-and-dlr/data/";
    private static final String BERLIN_SHP_FILE = DIR + "shp_merged/berlin_DHDN_GK4.shp";
    private static final String BERLIN_BRB_NET_FILE = DIR + "network/berlin_brb.xml.gz";


    public static MultiPolygon readBerlinArea()
    {
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(BERLIN_SHP_FILE);
        if (features.size() != 1) {
            throw new RuntimeException();
        }

        return (MultiPolygon)features.iterator().next().getDefaultGeometry();
    }


    public static void filterRequestsWithinBerlin(String allPlansFile, String berlinPlansFile)
    {
        final PreparedPolygon preparedPolygon = new PreparedPolygon(readBerlinArea());

        Scenario scenario = VrpLauncherUtils.initScenario(BERLIN_BRB_NET_FILE, DIR + allPlansFile);
        Population populationWithinBerlin = PopulationUtils.createPopulation(scenario.getConfig(),
                scenario.getNetwork());

        for (Person p : scenario.getPopulation().getPersons().values()) {
            if (isWithinArea(p.getPlans().get(0), preparedPolygon)) {
                populationWithinBerlin.addPerson(p);
            }
        }

        new PopulationWriter(populationWithinBerlin, scenario.getNetwork())
                .write(DIR + berlinPlansFile);
    }


    public static void main(String[] args)
    {
        for (double i = 10; i < 51; i++) {
            String suffix = "/plans/plans4to3_" + (i / 10) + ".xml.gz";
            String in = "scenarios/2014_10_basic_scenario_v4" + suffix;
            String out = "scenarios/2015_08_only_berlin_v1" + suffix;
            filterRequestsWithinBerlin(in, out);
        }
    }
}
