package playground.vsp.drt.accessibilityOrientedDrt.prepare;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;

public class ProcessDepartureTime {
	public static void main(String[] args) {
		String premiumRate = "0.2";

		Population inputPlans = PopulationUtils.readPopulation("/Users/luchengqi/Documents/MATSimScenarios/Dresden/drt-study/plans/drt-plans-heterogeneous-with-" + premiumRate + "-premium.xml.gz");
		for (Person person : inputPlans.getPersons().values()) {
			TripStructureUtils.Trip trip = TripStructureUtils.getTrips(person.getSelectedPlan()).getFirst();
			double departureTime = trip.getOriginActivity().getEndTime().seconds();
			// move departure after 24h to "today"
			if (departureTime > 86400) {
				departureTime -= 86400;
			}

			// the drt fleet need some time to relocate, so we start from 1:00 and simulate until 25:00 (wrap around)
			if (departureTime < 3600) {
				departureTime += 86400;
			}
			trip.getOriginActivity().setEndTime(departureTime);
		}
		new PopulationWriter(inputPlans).write("/Users/luchengqi/Documents/MATSimScenarios/Dresden/drt-study/plans/drt-plans-heterogeneous-with-" + premiumRate + "-premium-processed.xml.gz");
	}
}
