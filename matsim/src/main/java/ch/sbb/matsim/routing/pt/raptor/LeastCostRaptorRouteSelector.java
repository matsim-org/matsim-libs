/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.routing.pt.raptor;

import java.util.List;

/**
 * Selects the route with the least cost. If multiple routes share the least cost,
 * the route with the smallest deviation from the desiredDepartureTime is selected.
 * The deviation is calculated as the time difference between the actual departure time and
 * the desired departure time, with a negative difference (leaving earlier than desired)
 * having double the weight. (Example: having two routes, one leaving 5 minutes before the
 * desired time, and another leaving 9 minutes after the desired time, the second one
 * is selected due to (5 * 2) &gt; 9.
 *
 * @author mrieser / SBB
 */
public class LeastCostRaptorRouteSelector implements RaptorRouteSelector {

    @Override
    public RaptorRoute selectOne(List<RaptorRoute> routes, double desiredDepartureTime) {
        if (routes == null || routes.isEmpty()) {
            return null;
        }
        RaptorRoute bestRoute = null;
        double bestDiff = 0;
        for (RaptorRoute route : routes) {
            if (bestRoute == null || route.getTotalCosts() < bestRoute.getTotalCosts()) {
                bestRoute = route;
                bestDiff = route.getDepartureTime() - desiredDepartureTime;
                if (bestDiff < 0) {
                    bestDiff = -bestDiff * 2;
                }
            } else if (route.getTotalCosts() == bestRoute.getTotalCosts()) {
                double diff = route.getDepartureTime() - desiredDepartureTime;
                if (diff < 0) {
                    diff = -diff * 2; // make it positive and double
                }
                if (diff < bestDiff) {
                    bestRoute = route;
                    bestDiff = diff;
                }
            }
        }
        return bestRoute;
    }
}
