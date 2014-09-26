/**
 * 
 */
package playground.pieter.pseudosimulation.mobsim;

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractController;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.router.util.TravelTime;

import playground.pieter.pseudosimulation.controler.PSimControler;
import playground.singapore.transitRouterEventsBased.stopStopTimes.StopStopTime;
import playground.singapore.transitRouterEventsBased.waitTimes.WaitTime;

/**
 * @author fouriep
 * 
 */
public class PSimFactory implements MobsimFactory {

	private Collection<Plan> plans;
	private TravelTime travelTime;
	private WaitTime waitTime;
	private StopStopTime stopStopTime;
	private int iteration = 0;

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		if (iteration > 0)
			eventsManager.resetHandlers(iteration++);
		else
			iteration++;
		if (waitTime != null) {
			return new PSim(sc, eventsManager, plans, travelTime, waitTime,
					stopStopTime);

		} else {
			return new PSim(sc, eventsManager, plans, travelTime);
		}
	}

	public void setPlans(Collection<Plan> plans) {
		this.plans = plans;
	}

	public void setTimes(TravelTime travelTime) {
		this.travelTime = travelTime;
	}

	public void setTimes(TravelTime travelTime, WaitTime waitTime,
			StopStopTime stopStopTime) {
		this.travelTime = travelTime;
		this.waitTime = waitTime;
		this.stopStopTime = stopStopTime;
	}

}
