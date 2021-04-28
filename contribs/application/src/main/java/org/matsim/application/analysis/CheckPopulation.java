package org.matsim.application.analysis;


import it.unimi.dsi.fastutil.doubles.Double2IntAVLTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

@CommandLine.Command(
		name = "check-population",
		description = "Checks and prints key characteristics of a population."
)
public class CheckPopulation implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CheckPopulation.class);

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to population")
	private Path input;

	@CommandLine.Mixin
	private CrsOptions crs = new CrsOptions();

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	private static void sep() {
		log.info("");
		log.info("###########################");
		log.info("");
	}

	@Override
	public Integer call() throws Exception {

		if (!Files.exists(input)) {
			log.error("Population input {} does not exist.", input);
			return 2;
		}

		if (shp.getShapeFile() == null) {
			log.error("Shape file is required for this command.");
			return 2;
		}

		Population population = PopulationUtils.readPopulation(input.toString());

		HomeLocationFilter filter = new HomeLocationFilter(shp, crs.getInputCRS(), population);


		sep();


		log.info("Agents in scenario: \t\t{}", population.getPersons().size());
		log.info("Agents living in area: \t{}", filter.size());


		sep();

		Object2IntMap<String> attributes = new Object2IntOpenHashMap<>();

		// Count attributes
		for (Person agent : population.getPersons().values()) {
			agent.getAttributes().getAsMap().forEach((k, v) -> attributes.mergeInt(k, 1, Integer::sum));
		}

		log.info("Attributes:");

		attributes.forEach((k, v) -> log.info("\t{}: {}", k, v));


		sep();

		List<? extends Person> agents = population.getPersons().values().stream()
				.filter(filter::considerAgent)
				.collect(Collectors.toList());

		List<TripStructureUtils.Trip> trips = agents.stream().flatMap(
				agent -> TripStructureUtils.getTrips(agent.getSelectedPlan().getPlanElements()).stream()
		).collect(Collectors.toList());

		log.info("Number of trips: \t\t{}", trips.size());
		log.info("Avg. trips per agent: \t{}", (double) trips.size() / agents.size());


		log.info("Trip (euclidean) distance distribution:");

		NavigableSet<Double> g = new TreeSet<>(List.of(0d, 1000d, 3000d, 5000d, 10000d));

		Double2IntMap counts = new Double2IntAVLTreeMap();

		for (TripStructureUtils.Trip t : trips) {

			double dist = CoordUtils.calcEuclideanDistance(t.getOriginActivity().getCoord(), t.getDestinationActivity().getCoord());

			double group = g.floor(dist);
			counts.mergeInt(group, 1, Integer::sum);
		}


		counts.forEach((k, v) -> log.info("\t{}-m: {} ({}%)", k.intValue(), v, Math.round((v * 1000d) / trips.size()) / 10d));

		return 0;
	}

}
