package org.matsim.application.analysis.population;

import com.google.common.base.Joiner;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(name = "compare-plan-modes", description = "Compare plans between two populations")
public class ComparePlanModes implements MATSimAppCommand {

	@CommandLine.Parameters(arity = "2", paramLabel = "POPULATION", description = "Path to populations")
	private List<Path> input;

	@CommandLine.Option(names = "--subpopulation", description = "Subpopulation filter", defaultValue = "person")
	private String subpopulation;

	@CommandLine.Option(names = "--output", description = "Path to output CSV", required = true)
	private Path output;

	@CommandLine.Mixin
	private CsvOptions csv;

	public static void main(String[] args) {
		new ComparePlanModes().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		List<Population> populations = input.parallelStream()
				.map(p -> PopulationUtils.readPopulation(p.toString()))
				.collect(Collectors.toList());

		Set<Id<Person>> persons = new HashSet<>(populations.get(0).getPersons().keySet());

		if (!subpopulation.isEmpty())
			persons.removeIf(p -> {
				String subpop = PopulationUtils.getSubpopulation(populations.get(0).getPersons().get(p));
				return !subpopulation.equals(subpop);
			});

		// Filter all persons in all populations
		for (int i = 1; i < populations.size(); i++) {
			Set<Id<Person>> other = populations.get(i).getPersons().keySet();
			persons.removeIf(p -> !other.contains(p));
		}


		try (CSVPrinter printer = csv.createPrinter(output)) {

			printer.printRecord("person", "trips_to_subtours", "p1_score", "p2_score", "p1_modes", "p2_modes");

			for (Id<Person> id : persons) {

				Plan p0 = populations.get(0).getPersons().get(id).getSelectedPlan();
				Plan p1 = populations.get(1).getPersons().get(id).getSelectedPlan();

				String m0 = getModes(p0);
				String m1 = getModes(p1);

				if (!m0.equals(m1)) {

					printer.print(id.toString());
					printer.print(getSubtourString(p0));

					printer.print(p0.getScore());
					printer.print(p1.getScore());
					printer.print(m0);
					printer.print(m1);

					printer.println();
				}
			}
		}

		return 0;
	}

	/**
	 * Return string representation for all modes of the day.
	 */
	private String getModes(Plan p) {
		return TripStructureUtils.getTrips(p).stream().map(
				t -> TripStructureUtils.identifyMainMode(t.getTripElements())
		).collect(Collectors.joining("-"));
	}

	/**
	 * String representation of subtour relation.
	 */
	private String getSubtourString(Plan p) {

		Collection<TripStructureUtils.Subtour> subtours = TripStructureUtils.getSubtours(p);

		List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(p);

		List<String> all = new ArrayList<>();

		for (TripStructureUtils.Trip t : trips) {

			List<String> contains = new ArrayList<>();

			int i = 0;
			for (TripStructureUtils.Subtour st : subtours) {
				if (st.getTrips().contains(t))
					contains.add(String.valueOf(i));

				i++;
			}

			all.add(Joiner.on("|").join(contains));
		}

		return Joiner.on("-").join(all);
	}
}
