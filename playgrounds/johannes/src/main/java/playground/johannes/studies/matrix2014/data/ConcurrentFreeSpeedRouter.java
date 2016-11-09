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

package playground.johannes.studies.matrix2014.data;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author johannes
 */
public class ConcurrentFreeSpeedRouter implements LeastCostPathCalculator {

    private BlockingQueue<LeastCostPathCalculator> routers;

    public ConcurrentFreeSpeedRouter(Network network, LeastCostPathCalculatorFactory factory, int nThreads) {
        routers = new LinkedBlockingQueue<>();
        TravelTimes tt = new TravelTimes();
        for(int i = 0; i < nThreads; i++) {
            routers.add(factory.createPathCalculator(network, tt, tt));
        }
    }

    @Override
    public Path calcLeastCostPath(Node fromNode, Node toNode, double starttime, Person person, Vehicle vehicle) {
        try {
            LeastCostPathCalculator router = routers.take();
            Path path = router.calcLeastCostPath(fromNode, toNode, starttime, person, vehicle);
            routers.put(router);
            return path;

        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class TravelTimes implements TravelDisutility, TravelTime {

        @Override
        public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
            return link.getLength()/link.getFreespeed();
        }

        @Override
        public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
            return getLinkTravelTime(link, time, person, vehicle);
        }

        @Override
        public double getLinkMinimumTravelDisutility(Link link) {
            return getLinkTravelTime(link, 0, null, null);
        }
    }
}
