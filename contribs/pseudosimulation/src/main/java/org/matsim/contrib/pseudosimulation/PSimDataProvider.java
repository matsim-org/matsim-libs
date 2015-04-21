package org.matsim.contrib.pseudosimulation;

import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformance;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.core.router.util.TravelTime;

/**
 * Created by fouriep on 4/21/15.
 */
public interface PSimDataProvider {
    public StopStopTime getStopStopTime();
    public WaitTime getWaitTime();
    public TransitPerformance getTransitPerformance();
    public PlanCatcher getPlanCatcher();
    public TravelTime getTravelTime();
}
