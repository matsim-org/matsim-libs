package besttimeresponse;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class OptimalTimeAllocator {

	private final Scenario scenario;

	private final InterpolatedTravelTimes interpolatedTravelTimes;

	public OptimalTimeAllocator(final Scenario scenario) {
		this.scenario = scenario;
		this.interpolatedTravelTimes = null; // build this somehow from the scenario
	}

	public void optimize(final Plan plan) {

		
		
	}

}
