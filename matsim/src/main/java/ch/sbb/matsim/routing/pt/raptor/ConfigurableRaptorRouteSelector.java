/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package ch.sbb.matsim.routing.pt.raptor;

import org.matsim.core.gbl.MatsimRandom;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Implementation of {@link RaptorRouteSelector} that selects a route from a given
 * choice set based on configurable parameters. Each route will be scored,
 * and the route with the best (lowest) score will be taken. The score is
 * calculated as:
 * <pre>
 *     score = betaDepartureTime * abs(desiredDepartureTime - effectiveDepartureTime)
 *             + betaTravelTime * totalTravelTime
 *             + betaTransfer * transferCount
 * </pre>
 *
 * If multiple plans have the same best score, a random plan of them is chosen.
 *
 * @author mrieser / SBB
 */
public class ConfigurableRaptorRouteSelector implements RaptorRouteSelector {

    private double betaDepartureTime = 1;
    private double betaTravelTime = 1;
    private double betaTransfer = 300; // 1 transfer corresponds to 5 minutes

    private final Random random = MatsimRandom.getLocalInstance();

    public void setBetaDepartureTime(double betaDepartureTime) {
        this.betaDepartureTime = betaDepartureTime;
    }

    public void setBetaTravelTime(double betaTravelTime) {
        this.betaTravelTime = betaTravelTime;
    }

    public void setBetaTransfer(double betaTransfer) {
        this.betaTransfer = betaTransfer;
    }

    @Override
    public RaptorRoute selectOne(List<RaptorRoute> routes, double desiredDepartureTime) {
        if (routes == null || routes.isEmpty()) {
            return null;
        }
        RaptorRoute bestRoute = null;
        List<RaptorRoute> bestRoutes = null;
        double bestScore = 0;
        for (RaptorRoute route : routes) {
            double score = betaDepartureTime * Math.abs(desiredDepartureTime - route.getDepartureTime())
                           + betaTravelTime * route.getTravelTime()
                           + betaTransfer * route.getNumberOfTransfers();
            if (bestRoute == null || score < bestScore) {
                bestRoute = route;
                bestRoutes = null;
                bestScore = score;
            } else if (score == bestScore) {
                if (bestRoutes == null) {
                    bestRoutes = new ArrayList<>();
                    bestRoutes.add(bestRoute);
                }
                bestRoutes.add(route);
            }
        }
        if (bestRoutes != null) {
            // chose a random one
            int index = this.random.nextInt(bestRoutes.size());
            bestRoute = bestRoutes.get(index);
        }
        return bestRoute;
    }
}
