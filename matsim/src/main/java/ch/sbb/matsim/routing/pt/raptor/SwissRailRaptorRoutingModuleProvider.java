/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.routing.pt.raptor;

import com.google.inject.name.Named;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.RoutingModule;

import javax.inject.Inject;
import javax.inject.Provider;

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
    SwissRailRaptorRoutingModuleProvider(SwissRailRaptor raptor, Scenario scenario, @Named("transit_walk") RoutingModule transitWalkRouter) {
        this.raptor = raptor;
        this.scenario = scenario;
        this.transitWalkRouter = transitWalkRouter;
    }

    public RoutingModule get() {
        return new SwissRailRaptorRoutingModule(this.raptor, this.scenario.getTransitSchedule(), this.scenario.getNetwork(), this.transitWalkRouter);
    }
}
