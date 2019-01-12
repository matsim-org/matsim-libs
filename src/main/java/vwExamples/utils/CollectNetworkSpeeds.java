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

package vwExamples.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class CollectNetworkSpeeds {

    public static void main(String[] args) {
        Set<String> h = new HashSet<>(Arrays.asList());

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        String basedir = "D:\\Axer\\MatsimDataStore\\BaseCases\\vw219\\";
        new MatsimNetworkReader(scenario.getNetwork()).readFile(basedir + "vw219.output_network.xml.gz");
        for (Link link : scenario.getNetwork().getLinks().values()) {
            double speed = link.getFreespeed();

            double opt1 = Math.floor(speed * 1 / 0.75 * 3.6);
            double opt2 = Math.floor(speed * 1 / 0.70 * 3.6);


            if (opt1 % 5 == 0) {
                link.setFreespeed(opt1 / 3.6);

                h.add(Double.toString(opt1));

            } else
                link.setFreespeed(opt2 / 3.6);

            h.add(Double.toString(opt2));

//			if (speed<10) link.setFreespeed(0.75*speed);
//			else if (speed<20) link.setFreespeed(0.7*speed);
//			else if (speed<30) 
//			{
//				if (link.getNumberOfLanes()<2) link.setFreespeed(0.7*speed);
//				else link.setFreespeed(0.75*speed);
//			}
//			else link.setFreespeed(0.7*speed);

        }
        new NetworkWriter(scenario.getNetwork()).write(basedir + "networks_newSpeeds.xml");

        System.out.println(h.toString());


    }

}
