package org.matsim.application.analysis;


import it.unimi.dsi.fastutil.doubles.Double2IntAVLTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
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
import java.util.*;
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

		Object2IntMap<String> attributes = new Object2IntAVLTreeMap<>();

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

		sep();

		Object2IntMap<String> acts = new Object2IntAVLTreeMap<>();
		Object2IntMap<String> firstAct = new Object2IntAVLTreeMap<>();
		Object2IntMap<String> lastAct = new Object2IntAVLTreeMap<>();

		List<TripStructureUtils.Subtour> subtours = new ArrayList<>();

		for (Person agent : agents) {

			// if there are no facility or link ids. the coordinate is used as proxy id.
			for (PlanElement el : agent.getSelectedPlan().getPlanElements()) {
				if (el instanceof Activity) {
					Activity act = (Activity) el;
					if (act.getFacilityId() == null && act.getLinkId() == null) {
						act.setLinkId(Id.createLinkId(act.getCoord().toString()));
					}
				}
			}

			subtours.addAll(TripStructureUtils.getSubtours(agent.getSelectedPlan()));

			List<Activity> activities = TripStructureUtils.getActivities(agent.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

			if (activities.size() == 0)
				continue;

			firstAct.mergeInt(actName(activities.get(0)), 1, Integer::sum);
			lastAct.mergeInt(actName(activities.get(activities.size() - 1)), 1, Integer::sum);
			activities.forEach(act -> acts.mergeInt(actName(act), 1, Integer::sum));
		}

		log.info("Activity distribution (total):");

		printDist(acts);

		log.info("Activity distribution (first):");

		printDist(firstAct);

		log.info("Activity distribution (last):");

		printDist(lastAct);

		long closed = subtours.stream().filter(TripStructureUtils.Subtour::isClosed).count();

		if (subtours.size() > 0)
			log.info("Closed subtours estimate: \t{}%", Math.round((closed * 1000d) / subtours.size()) / 10d);
		else
			log.info("No info about subtours (link or facilities ids missing)");

		return 0;
	}

	private static String actName(Activity act) {

		int idx = act.getType().lastIndexOf("_");
		if (idx == -1)
			return act.getType();

		return act.getType().substring(0, idx);
	}

	private static <T> void printDist(Object2IntMap<T> map) {
		int total = map.values().intStream().sum();
		map.forEach((k, v) -> log.info("\t{}: {} ({}%)", k, v, Math.round((v * 1000d) / total) / 10d));
	}

}
