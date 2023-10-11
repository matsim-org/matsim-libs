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

import com.google.inject.name.Named;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.RoutingModule;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * This class is similar to {@link org.matsim.core.router.Transit},
 * but adapted to SwissRailRaptor.
 *
 * @author mrieser / SBB
 */
public class SwissRailRaptorRoutingModuleProvider implements Provider<RoutingModule> {

    private final SwissRailRaptor raptor;
    private final Scenario scenario;
    private final RoutingModule transitWalkRouter;

    @Inject
    SwissRailRaptorRoutingModuleProvider(SwissRailRaptor raptor, Scenario scenario, @Named(TransportMode.walk) RoutingModule transitWalkRouter) {
        this.raptor = raptor;
        this.scenario = scenario;
        this.transitWalkRouter = transitWalkRouter;
    }

    @Override
	public RoutingModule get() {
        return new SwissRailRaptorRoutingModule(this.raptor, this.scenario.getTransitSchedule(), this.scenario.getNetwork(), this.transitWalkRouter);
    }
}
