/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2017.
 */

package ch.sbb.matsim.routing.pt.raptor;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.pt.router.TransitScheduleChangedEventHandler;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * @author mrieser / SBB
 */
@Singleton
public class SwissRailRaptorFactory implements Provider<SwissRailRaptor> {

    private SwissRailRaptorData data = null;
    private final TransitSchedule schedule;
    private final RaptorStaticConfig raptorConfig;
    private final RaptorParametersForPerson raptorParametersForPerson;
    private final RaptorRouteSelector routeSelector;
    private final Provider<RaptorStopFinder> stopFinderProvider;
    private final Network network;
    private final PlansConfigGroup plansConfigGroup;

    @Inject
    public SwissRailRaptorFactory(final TransitSchedule schedule, final Config config, final Network network,
                                  RaptorParametersForPerson raptorParametersForPerson, RaptorRouteSelector routeSelector,
                                  Provider<RaptorStopFinder> stopFinderProvider, PlansConfigGroup plansConfigGroup,
                                  final EventsManager events) {
        this.schedule = schedule;
        this.raptorConfig = RaptorUtils.createStaticConfig(config);
        this.network = network;
        this.raptorParametersForPerson = raptorParametersForPerson;
        this.routeSelector = routeSelector;
        this.stopFinderProvider = stopFinderProvider;
        this.plansConfigGroup = plansConfigGroup;

        if (events != null) {
            events.addHandler((TransitScheduleChangedEventHandler) event -> this.data = null);
        }
    }

    @Override
    public SwissRailRaptor get() {
        SwissRailRaptorData data = getData();
        return new SwissRailRaptor(data, this.raptorParametersForPerson, this.routeSelector, this.stopFinderProvider.get(),
                this.plansConfigGroup.getSubpopulationAttributeName());
    }

    private SwissRailRaptorData getData() {
        if (this.data == null) {
            this.data = prepareData();
        }
        return this.data;
    }

    synchronized private SwissRailRaptorData prepareData() {
        if (this.data != null) {
            // due to multithreading / race conditions, this could still happen.
            // prevent doing the work twice.
            return this.data;
        }
        this.data = SwissRailRaptorData.create(this.schedule, this.raptorConfig, this.network);
        return this.data;
    }

}
