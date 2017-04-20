package playground.gregor.gis.cutoutnetwork;/* *********************************************************************** *
 * project: org.matsim.*
 *
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CutoutNetwork {

    public static void main(String[] args) {


        String input = "/Users/laemmel/scenarios/misanthrope/padang/rerun1358/network.xml.gz";
        String output = "/Users/laemmel/scenarios/misanthrope/paper/network.xml";
        Config c = ConfigUtils.createConfig();
        Scenario sc = ScenarioUtils.createScenario(c);
        new MatsimNetworkReader(sc.getNetwork()).readFile(input);
        Predicate<Node> filter = new BountingBoxFilter(650121, 651786, 9893054, 9894367);

        Set<Node> nodes = sc.getNetwork().getNodes().values().parallelStream().filter(filter).collect(Collectors.toSet());
        List<Link> links = sc.getNetwork().getLinks().values().parallelStream().filter(l -> nodes.contains(l.getFromNode()) && nodes.contains(l.getToNode())).collect(Collectors.toList());

        Scenario sc2 = ScenarioUtils.createScenario(c);

        Network net = sc2.getNetwork();
        NetworkFactory fac = sc2.getNetwork().getFactory();

        nodes.forEach(n -> {
            Node nn = fac.createNode(n.getId(), n.getCoord());
            net.addNode(nn);
        });
        links.forEach(l -> {
            Link ll = fac.createLink(l.getId(), l.getFromNode(), l.getToNode());
            ll.setAllowedModes(l.getAllowedModes());
            ll.setCapacity(l.getCapacity());
            ll.setFreespeed(l.getFreespeed());
            ll.setLength(l.getLength());
            ll.setNumberOfLanes(l.getNumberOfLanes());
            net.addLink(ll);
        });

        net.setCapacityPeriod(sc.getNetwork().getCapacityPeriod());
        net.setEffectiveCellSize(sc.getNetwork().getEffectiveCellSize());
        net.setEffectiveLaneWidth(sc.getNetwork().getEffectiveLaneWidth());
        new NetworkWriter(net).write(output);

    }
}
