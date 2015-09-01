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

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.run.VrpConfigUtils;
import org.matsim.core.network.*;
import org.matsim.core.scenario.ScenarioUtils;


public class BerlinNetworkChangeEvents
{
    private static final String DIR = "d:/svn-vsp/sustainability-w-michal-and-dlr/data/";
    private static final String BERLIN_BRB_NET_FILE = DIR + "network/berlin_brb.xml.gz";
    private static final String ONLY_BERLIN_NET_FILE = DIR + "network/berlin.xml.gz";


    public static void filterEventsWithinBerlin(String allChangeEventsFile,
            String berlinChangeEventsFile)
    {
        Scenario berlinBrbScenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        new MatsimNetworkReader(berlinBrbScenario).readFile(BERLIN_BRB_NET_FILE);

        Scenario onlyBerlinScenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        new MatsimNetworkReader(onlyBerlinScenario).readFile(ONLY_BERLIN_NET_FILE);

        List<NetworkChangeEvent> berlinBrbEvents = new NetworkChangeEventsParser(
                berlinBrbScenario.getNetwork()).parseEvents(DIR + allChangeEventsFile);

        List<NetworkChangeEvent> onlyBerlinEvents = new ArrayList<>();
        Map<Id<Link>, ? extends Link> onlyBerlinLinks = onlyBerlinScenario.getNetwork().getLinks();

        for (NetworkChangeEvent e : berlinBrbEvents) {
            if (e.getLinks().size() != 1) {
                throw new RuntimeException("Only 1 link per event supported");
            }

            Link l = e.getLinks().iterator().next();
            if (onlyBerlinLinks.containsKey(l.getId())) {
                onlyBerlinEvents.add(e);
            }
        }

        new NetworkChangeEventsWriter().write(DIR + berlinChangeEventsFile, onlyBerlinEvents);
    }


    public static void main(String[] args)
    {
        String[] files = { "changeevents", "changeevents_min" };
        for (String f : files) {
            String in = "scenarios/2014_10_basic_scenario_v4/" + f + ".xml.gz";
            String out = "scenarios/2015_08_only_berlin_v1/" + f + ".xml.gz";
            filterEventsWithinBerlin(in, out);
        }
    }
}
