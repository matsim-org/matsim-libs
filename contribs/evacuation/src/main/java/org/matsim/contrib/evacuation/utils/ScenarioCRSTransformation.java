package org.matsim.contrib.evacuation.utils;
/* *********************************************************************** *
 * project: org.matsim.*
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;

import java.util.HashSet;
import java.util.Set;

/**
 * /**
 * transforms the coordinate system (hopefully) of an entire scenario
 *
 * @author laemmel
 */
public abstract class ScenarioCRSTransformation {

    public static void transform(Scenario sc, String targetCRSTxt) {
        String srcCRSTxt = sc.getConfig().global().getCoordinateSystem();

        CoordinateTransformation ct = new GeotoolsTransformation(srcCRSTxt, targetCRSTxt);

        Set<Coord> handled = new HashSet<>();

        for (Node n : sc.getNetwork().getNodes().values()) {
            Coord c = n.getCoord();
            if (handled.contains(c)) {
                continue;
            }
            handled.add(c);
            Coord cc = ct.transform(c);
            c.setXY(cc.getX(), cc.getY());
        }
//		for (Link l : sc.getNetwork().getLinks().values()) {
//			Coord c = l.getCoord();
//			if (handled.contains(c)){
//				continue;
//			}
//			handled.add(c);
//			Coord cc = ct.transform(c);
//			c.setXY(cc.getX(), cc.getY());
//		}
        for (Person pers : sc.getPopulation().getPersons().values()) {
            for (Plan pl : pers.getPlans()) {
                for (PlanElement el : pl.getPlanElements()) {
                    if (el instanceof Activity) {
                        Activity act = (Activity) el;
                        Coord c = act.getCoord();
                        if (handled.contains(c)) {
                            continue;
                        }
                        handled.add(c);
                        Coord cc = ct.transform(c);
                        c.setXY(cc.getX(), cc.getY());
                    }
                }
            }
        }
        sc.getConfig().global().setCoordinateSystem(targetCRSTxt);
    }
}
