import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;

import java.util.List;

public class Filter {
	public static void main(String[] args) {
		Population population = PopulationUtils.readPopulation("/Users/paulh/Nextcloud/Shared/RustQSim-Data/scenarios/rvr/input/rvr-matsim-10pct/rvr-v1.4-10pct.plans.xml.gz");

		List<? extends Id> list = population.getPersons().values().stream()
			.filter(p -> {
				var mode = TransportMode.ride;
				return !p.getSelectedPlan().getPlanElements().stream()
					.filter(e -> e instanceof Leg)
					.map(e -> (Leg) e)
					.anyMatch(l -> l.getRoutingMode().contains(mode) || l.getMode().contains(mode));
			}).map(p -> p.getId()).toList();

		for (Id id : list) {
			population.removePerson(id);
		}

		System.out.println("Pop size: " + population.getPersons().size());

		PopulationUtils.writePopulation(population, "/Users/paulh/Nextcloud/rust-qsim/rvr-1.4/input/rvr-v1.4-10pct.filtered-plans.xml.gz");

	}
}
