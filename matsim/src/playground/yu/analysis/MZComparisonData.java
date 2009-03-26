/**
 * 
 */
package playground.yu.analysis;

import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.AgentDepartureEventHandler;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * prepares the data to compare with
 * "BFS/ARE: Mikrozensus zum Verkehrsverhalten 2005" (Kanton Zurich)
 * 
 * @author yu
 * 
 */
public class MZComparisonData extends AbstractPersonAlgorithm implements
		PlanAlgorithm, AgentDepartureEventHandler, AgentArrivalEventHandler {
	@Override
	public void run(Person person) {
		// TODO Auto-generated method stub

	}

	public void run(Plan plan) {
		// TODO Auto-generated method stub

	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	public void handleEvent(AgentDepartureEvent event) {
		// TODO Auto-generated method stub
	}

	public void handleEvent(AgentArrivalEvent event) {
		// TODO Auto-generated method stub

	}

	public double getDailyDistance_m() {
		return 0.0;
	}

	public double getDailyEnRouteTime_min() {
		return 0.0;
	}

	public double getLegLinearDistance_m() {
		return 0.0;
	}

	public double getWorkHomeLinearDistance_m() {
		return 0.0;
	}
}
