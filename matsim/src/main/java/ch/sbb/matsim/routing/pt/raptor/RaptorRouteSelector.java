/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.routing.pt.raptor;

import java.util.List;

/**
 * @author mrieser / SBB
 */
@FunctionalInterface
public interface RaptorRouteSelector {
    RaptorRoute selectOne(List<RaptorRoute> routes, double desiredDepartureTime);
}
