package besttimeresponse;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TimeStructureFactory {

	private final InterpolatedTravelTimes travelTimes;

	public TimeStructureFactory(final InterpolatedTravelTimes travelTimes) {
		this.travelTimes = travelTimes;
	}

	public List<Double> newFeasibleDepartureTimes_s(final Plan plan) {
		final LinkedList<Double> departureTimes_s = new LinkedList<Double>();
		
		for (int actIndex = 0; actIndex < plan.getPlanElements().size(); actIndex += 2) {
			final Activity act = (Activity) plan.getPlanElements().get(actIndex);
			act.getEndTime();

		}

		return departureTimes_s;
	}

}
