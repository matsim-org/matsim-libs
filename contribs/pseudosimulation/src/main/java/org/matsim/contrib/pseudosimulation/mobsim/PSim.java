package org.matsim.contrib.pseudosimulation.mobsim;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.TransitEmulator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.config.TransitConfigGroup;

/**
 * Runs selected plans through the pseudo-simulation event model.
 *
 * @author illenberger
 * @author fouriep
 * @author sergioo
 */
public class PSim implements Mobsim {

    private final Scenario scenario;
    private final EventsManager eventManager;
    private final Collection<Plan> plans;
    private final PSimExecutionCoordinator coordinator;

    public PSim(Scenario scenario, EventsManager eventManager, Collection<Plan> plans,
            TravelTime carLinkTravelTimes) {
        this(scenario, eventManager, plans, carLinkTravelTimes, null, new LinkedHashSet<>());
    }

    public PSim(Scenario scenario, EventsManager eventManager, Collection<Plan> plans,
            TravelTime carLinkTravelTimes, TransitEmulator transitEmulator) {
        this(scenario, eventManager, plans, carLinkTravelTimes, transitEmulator,
                ConfigUtils.addOrGetModule(scenario.getConfig(), TransitConfigGroup.class).getTransitModes());
    }

    private PSim(Scenario scenario, EventsManager eventManager, Collection<Plan> plans,
            TravelTime carLinkTravelTimes, TransitEmulator transitEmulator, Set<String> transitModes) {
        LogManager.getLogger(getClass()).warn("Constructing PSim");
        this.scenario = scenario;
        this.eventManager = eventManager;
        this.plans = plans;
        double endTime = scenario.getConfig().qsim().getEndTime().seconds();
        int workerCount = scenario.getConfig().global().getNumberOfThreads();
        coordinator = PSimExecutionCoordinator.create(workerCount,
                () -> new PSimPlanExecutor(endTime, carLinkTravelTimes, transitEmulator, transitModes));
    }

    @Override
    public void run() {
        LogManager.getLogger(getClass()).error("Executing " + plans.size() + " plans in pseudosimulation.");
        coordinator.execute(plans, scenario.getNetwork(), eventManager);
    }
}
