/**
 * 
 */
package org.matsim.contrib.pseudosimulation.mobsim;

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformance;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.router.util.TravelTime;

import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;

/**
 * @author fouriep
 * 
 */
public class PSimFactory implements MobsimFactory {

	private Collection<Plan> plans;
	private TravelTime travelTime;
	private WaitTime waitTime;
	private StopStopTime stopStopTime;
	private TransitPerformance transitPerformance;

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
//		if (iteration > 0)
//			eventsManager.resetHandlers(iteration++);
//		else
//			iteration++;
		if (waitTime != null) {
			return new PSim(sc, eventsManager, plans, travelTime, waitTime, stopStopTime, transitPerformance);

		} else {
			return new PSim(sc, eventsManager, plans, travelTime);
		}
	}

	public void setPlans(Collection<Plan> plans) {
		this.plans = plans;
	}

	public void setTravelTime(TravelTime travelTime) {
		this.travelTime = travelTime;
	}
	public void setWaitTime(WaitTime waitTime) {
		this.waitTime = waitTime;
	}
	
	public void setStopStopTime(StopStopTime stopStopTime) {
		this.stopStopTime = stopStopTime;
	}

	public void setTransitPerformance(TransitPerformance transitPerformance){
		this.transitPerformance = transitPerformance;
	}

	public void setTimes(TravelTime travelTime, WaitTime waitTime, StopStopTime stopStopTime) {
		this.travelTime = travelTime;
		this.waitTime = waitTime;
		this.stopStopTime = stopStopTime;
	}

}
