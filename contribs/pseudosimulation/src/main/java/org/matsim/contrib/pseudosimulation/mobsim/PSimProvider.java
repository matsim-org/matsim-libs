/**
 *
 */
package org.matsim.contrib.pseudosimulation.mobsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformance;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.TransitEmulator;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author fouriep
 */
public class PSimProvider implements Provider<Mobsim> {

    @Inject private PlanCatcher plans;
    @Inject private TravelTime travelTime;
    @Inject TransitEmulator transitEmulator;
    private final Scenario scenario;
    private final EventsManager eventsManager;

    @Inject
	public PSimProvider(Scenario scenario, EventsManager eventsManager) {
        this.scenario = scenario;
        this.eventsManager = eventsManager;
    }

    @Override
    public Mobsim get() {
        return new PSim(scenario, eventsManager, plans.getPlansForPSim(), travelTime, transitEmulator);
    }

    public void setTravelTime(TravelTime travelTime) {
        this.travelTime = travelTime;
    }

//    @Deprecated
//    public void setWaitTime(WaitTime waitTime) {
//    	throw new RuntimeException("Use an instance of " + TransitEmulator.class.getSimpleName() + " instead.");
//    }

//    @Deprecated
//    public void setStopStopTime(StopStopTime stopStopTime) {
//    	throw new RuntimeException("Use an instance of " + TransitEmulator.class.getSimpleName() + " instead.");
//    }

    @Deprecated
    public void setTransitPerformance(TransitPerformance transitPerformance) {
    	throw new RuntimeException("Use an instance of " + TransitEmulator.class.getSimpleName() + " instead.");
    }

//    @Deprecated
//    public void setTimes(TravelTime travelTime, WaitTime waitTime, StopStopTime stopStopTime) {
//    	throw new RuntimeException("Use an instance of " + TransitEmulator.class.getSimpleName() + " instead.");
//    }

    @Deprecated
    //will replace where necessary
    public Mobsim createMobsim(Scenario scenario, EventsManager events) {
        return get();
    }
}
