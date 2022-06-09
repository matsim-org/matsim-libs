package org.matsim.contribs.discrete_mode_choice.commands;


import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.KShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.YenKShortestPath;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.MATSimApplication;
import org.matsim.contribs.discrete_mode_choice.components.estimators.MATSimTripScoringEstimator;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.contribs.discrete_mode_choice.modules.ModeAvailabilityModule;
import org.matsim.contribs.discrete_mode_choice.modules.ModelModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import picocli.CommandLine;
import playground.vsp.pt.fare.DistanceBasedPtFareParams;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.matsim.contribs.discrete_mode_choice.modules.EstimatorModule.MATSIM_TRIP_SCORING;

@CommandLine.Command(
		name = "generate-choice-set",
		description = "Generate a static mode-choice set for all agents in the population"
)
public class GenerateChoiceSet implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(GenerateChoiceSet.class);

	@CommandLine.Option(names = "--config", description = "Path to scenario config", required = true)
	private Path configPath;

	@CommandLine.Option(names = "--scenario", description = "Full qualified classname of the MATSim application scenario class", required = true)
	private Class<? extends MATSimApplication> scenario;

	@CommandLine.Option(names = "--population", description = "Path to input population")
	private Path populationPath;

	@CommandLine.Option(names = "--subpopulation", description = "Subpopulation filter", defaultValue = "person")
	private String subpopulation;

	@CommandLine.Option(names = "--top-k", description = "Use top k estimates", defaultValue = "5")
	private int topK;

	@CommandLine.Option(names = "--modes", description = "Modes to include in estimation", defaultValue = "car,walk,bike,pt,ride", split = ",")
	private Set<String> modes;

	@CommandLine.Option(names = "--output", description = "Path for output population", required = true)
	private Path output;

	@Inject
	private TripListConverter tripListConverter;
	@Inject
	private MATSimTripScoringEstimator estimator;
	@Inject
	private ModeAvailability modeAvailability;
	@Inject
	private TimeInterpretation timeInterpretation;


	public static void main(String[] args) {
		new GenerateChoiceSet().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Config config = ConfigUtils.loadConfig(configPath.toString());

		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		DiscreteModeChoiceConfigGroup dmc = ConfigUtils.addOrGetModule(config, DiscreteModeChoiceConfigGroup.class);

		dmc.setModelType(ModelModule.ModelType.Trip);
		dmc.setTripEstimator(MATSIM_TRIP_SCORING);
		dmc.setModeAvailability(ModeAvailabilityModule.CAR);
		dmc.getCarModeAvailabilityConfig().setAvailableModes(modes);

		Controler controler = MATSimApplication.prepare(scenario, config);

		controler.run();

		Injector injector = controler.getInjector();
		injector.injectMembers(this);

		DistanceBasedPtFareParams ptFare = ConfigUtils.addOrGetModule(controler.getConfig(), DistanceBasedPtFareParams.class);

		log.info("Estimating mode combinations...");

		for (Person person : controler.getScenario().getPopulation().getPersons().values()) {

			String subpop = PopulationUtils.getSubpopulation(person);
			if (!subpop.equals(subpopulation)) continue;

			List<DiscreteModeChoiceTrip> trips = tripListConverter.convert(person.getSelectedPlan());

			if (trips.isEmpty()) continue;

			List<String> modes = new ArrayList<>(modeAvailability.getAvailableModes(person, trips));

			Set<PlanCandidate> candidates = new HashSet<>();

			// TODO: with or without car
			// lower and upper bound pt

			for (boolean hasCar : List.of(true, false)) {

				// this person can not use the car
				if (hasCar && !modes.contains("car"))
					continue;

				if (!hasCar)
					modes.remove("car");

				for (boolean estimatePt: List.of(true, false)) {

					estimator.setPtFare( estimatePt ? ptFare : null);

					List<List<TripCandidate>> result = chooseModes(person, trips, modes);
					DirectedWeightedMultigraph<Integer, TripCandidate> graph = new DirectedWeightedMultigraph<>(TripCandidate.class);

					for (int i = 0; i < result.size() + 1; i++) {
						graph.addVertex(i);
					}

					// TODO
					// mass conservation? ?

					for (int i = 0; i < result.size(); i++) {

						for (int j = 0; j < result.get(i).size(); j++) {

							TripCandidate edge = result.get(i).get(j);
							graph.addEdge(i, i + 1, edge);
							// TODO: weight must be positive, simple fix
							graph.setEdgeWeight(edge, -edge.getUtility() + 100000);
						}
					}

					KShortestPathAlgorithm<Integer, TripCandidate> algo = new YenKShortestPath<>(graph);

					candidates.addAll(
							algo.getPaths(0, result.size(), topK).stream()
									.map(PlanCandidate::new)
									.collect(Collectors.toSet())
					);

				}
			}

			duplicatePlans(person, candidates);
		}

		PopulationUtils.writePopulation(controler.getScenario().getPopulation(), output.toString());

		return 0;
	}

	private List<List<TripCandidate>> chooseModes(Person person, List<DiscreteModeChoiceTrip> trips, List<String> modes) {

		List<List<TripCandidate>> result = new ArrayList<>(trips.size());

		List<TripCandidate> candidates = new ArrayList<>();
		List<TripCandidate> previousTrips = new ArrayList<>();

		TimeTracker timeTracker = new TimeTracker(timeInterpretation);

		for (DiscreteModeChoiceTrip trip : trips) {
			timeTracker.addActivity(trip.getOriginActivity());
			trip.setDepartureTime(timeTracker.getTime().seconds());

			TripCandidate candidate = null;

			for (String mode : modes) {
				candidate = estimator.estimateTrip(person, mode, trip, previousTrips);
				candidates.add(candidate);
			}


			// TODO: time tracker won't be correct and would be necessary for all combinations
			if (candidate != null) {
				timeTracker.addDuration(candidate.getDuration());
			}

			result.add(candidates);
			previousTrips = candidates;
			candidates = new ArrayList<>();
		}


		return result;
	}

	private void duplicatePlans(Person person, Set<PlanCandidate> candidates) {

		Plan plan = person.getSelectedPlan();

		// remove all unselected plans
		Set<Plan> plans = new HashSet<>(person.getPlans());
		plans.remove(plan);
		plans.forEach(person::removePlan);

		int i = 0;

		for (PlanCandidate c : candidates) {

			plan.setType("candidate_" + i);

			int k = 0;
			for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {

				String mode = c.getMode(k);

				for (Leg leg : trip.getLegsOnly()) {

					leg.setRoute(null);
					leg.setMode(mode);
					TripStructureUtils.setRoutingMode(leg, mode);
				}

				k++;
			}

			plan = person.createCopyOfSelectedPlanAndMakeSelected();
			i++;
		}
	}


	private static final class PlanCandidate {

		private final List<String> modes;
		private final double weight;

		private PlanCandidate(GraphPath<Integer, TripCandidate> path) {
			this.modes = path.getEdgeList().stream().map(TripCandidate::getMode).collect(Collectors.toList());
			this.weight = path.getWeight();
		}

		public String getMode(int idx) {
			return modes.get(idx);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			PlanCandidate that = (PlanCandidate) o;
			return modes.equals(that.modes);
		}

		@Override
		public int hashCode() {
			return Objects.hash(modes);
		}
	}

}
