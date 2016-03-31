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

import java.util.Map;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.*;
import org.matsim.core.scenario.ScenarioUtils;


public class BerlinTaxiRequests
{
    private static final String DIR = "d:/svn-vsp/sustainability-w-michal-and-dlr/data/";
    private static final String BERLIN_BRB_NET_FILE = DIR + "network/berlin_brb.xml.gz";
    private static final String ONLY_BERLIN_NET_FILE = DIR + "network/only_berlin.xml.gz";


    public static void filterRequestsWithinBerlin(String allPlansFile, String berlinPlansFile)
    {
        Scenario berlinBrbScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(berlinBrbScenario.getNetwork()).readFile(BERLIN_BRB_NET_FILE);
        new MatsimPopulationReader(berlinBrbScenario).readFile(DIR + allPlansFile);

        Scenario onlyBerlinScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(onlyBerlinScenario.getNetwork()).readFile(ONLY_BERLIN_NET_FILE);
        Population onlyBerlinPop = PopulationUtils.createPopulation(onlyBerlinScenario.getConfig(),
                onlyBerlinScenario.getNetwork());
        Map<Id<Link>, ? extends Link> onlyBerlinLinks = onlyBerlinScenario.getNetwork().getLinks();

        for (Person p : berlinBrbScenario.getPopulation().getPersons().values()) {
            Plan plan = p.getPlans().get(0);
            Activity fromActivity = (Activity)plan.getPlanElements().get(0);
            Activity toActivity = (Activity)plan.getPlanElements().get(2);

            if (onlyBerlinLinks.containsKey(fromActivity.getLinkId())
                    && onlyBerlinLinks.containsKey(toActivity.getLinkId())) {
                onlyBerlinPop.addPerson(p);
            }
        }

        new PopulationWriter(onlyBerlinPop, onlyBerlinScenario.getNetwork())
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
