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

package playground.michalm.barcelona;

import java.util.*;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.*;
import org.matsim.core.network.NetworkChangeEvent.*;
import org.matsim.core.scenario.ScenarioUtils;


public class BarcelonaNetworkChangeEventsGenerator
{
    //reduced speed between 7:00 and 22:59:59
    private final static double[] SCALING_FACTORS = { 1, 1, 1, 1, 1, 1, 1, .8, .5, .5, .65, .8, .85,
            .8, .65, .6, .5, .5, .5, .65, .7, .8, .95, 1 };

    private final NetworkChangeEventFactory factory = new NetworkChangeEventFactoryImpl();
    private final List<NetworkChangeEvent> networkChangeEvents = new ArrayList<NetworkChangeEvent>();
    private final Network network;


    public BarcelonaNetworkChangeEventsGenerator(Network network)
    {
        this.network = network;
    }


    public void generateChangeEvents()
    {
        double prevFactor = 1;
        for (int i = 0; i < 24; i++) {
            double relativeFactor = SCALING_FACTORS[i] / prevFactor;
            prevFactor = SCALING_FACTORS[i];

            if (relativeFactor != 1) {
                NetworkChangeEvent e = factory.createNetworkChangeEvent(i * 3600);
                e.setFreespeedChange(new ChangeValue(ChangeType.FACTOR, relativeFactor));
                e.addLinks(network.getLinks().values());
                this.networkChangeEvents.add(e);
            }
        }
    }


    public List<NetworkChangeEvent> getNetworkChangeEvents()
    {
        return networkChangeEvents;
    }


    public static void main(String[] args)
    {
        String dir = "d:/PP-rad/Barcelona/data/";
        String networkFile = dir + "network/barcelona_network.xml";
        String changeEventsFile = dir + "network/barcelona_change_events.xml";

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
        BarcelonaNetworkChangeEventsGenerator gen = new BarcelonaNetworkChangeEventsGenerator(
                scenario.getNetwork());
        gen.generateChangeEvents();
        new NetworkChangeEventsWriter().write(changeEventsFile, gen.getNetworkChangeEvents());
    }
}
