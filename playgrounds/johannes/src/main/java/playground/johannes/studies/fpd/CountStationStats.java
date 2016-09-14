/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.johannes.studies.fpd;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author johannes
 */
public class CountStationStats {

    public  static void main(String args[]) throws IOException {
        String popFile = args[0];
        String countsFile = args[1];
        String txtFile = args[2];

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        PopulationReader popReader = new PopulationReader(scenario);
        popReader.readFile(popFile);

        Counts<Link> counts = new Counts<>();
        MatsimCountsReader cReader = new MatsimCountsReader(counts);
        cReader.readFile(countsFile);

        TObjectDoubleMap<Id> distances = new TObjectDoubleHashMap<>();
        TObjectIntMap<Id> volumes = new TObjectIntHashMap<>();

        for(Person person : scenario.getPopulation().getPersons().values()) {
            for(Plan plan : person.getPlans()) {
                for(int i = 1; i < plan.getPlanElements().size(); i+=2) {
                    Leg leg = (Leg) plan.getPlanElements().get(i);
                    NetworkRoute route = (NetworkRoute) leg.getRoute();
                    double d = route.getDistance();

                    for(Id<Link> id : route.getLinkIds()) {
                        distances.adjustOrPutValue(id, d, d);
                        volumes.adjustOrPutValue(id, 1, 1);
                    }
                }
            }
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(txtFile));
        writer.write("id\tavrDist");
        writer.newLine();

        for(Count<Link> count : counts.getCounts().values()) {
            Id<Link> id = Id.createLinkId(count.getLocId());
            double dSum = distances.get(id);
            int volume = volumes.get(id);
            double dAvr = dSum/(double)volume;

            writer.write(count.getCsId());
            writer.write("\t");
            writer.write(String.valueOf(dAvr));
            writer.newLine();
        }
        writer.close();
    }
}
