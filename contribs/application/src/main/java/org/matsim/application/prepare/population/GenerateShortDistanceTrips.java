package org.matsim.application.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.HomeLocationFilter;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.RoutingModeMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This step should be used when the population is missing short distance trips.
 *
 * @author zmeng, Chengqi Lu
 */
@CommandLine.Command(
        name = "generate-short-distance-trips",
        description = "Add short-distance walk trips to a population",
        showDefaultValues = true
)
public class GenerateShortDistanceTrips implements MATSimAppCommand {

    private static final Logger log = LogManager.getLogger(GenerateShortDistanceTrips.class);

    @CommandLine.Option(names = "--population", description = "Original plan file", required = true)
    private Path input;

    @CommandLine.Option(names = "--output", description = "Output filename")
    private Path output;

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    private Generate generate;

    @CommandLine.Mixin
    private final ShpOptions shp = new ShpOptions();

    @CommandLine.Mixin
    private final CrsOptions crs = new CrsOptions();

    @CommandLine.Option(names = "--range", description = "Maximum distance in meter", defaultValue = "1000")
    private double range;

    @CommandLine.Option(names = "--max-duration", description = "Maximum duration in seconds", defaultValue = "3600")
    private double maxDuration;

    private final Random rnd = new Random(4711);

    private Population population;

    @Override
    public Integer call() throws Exception {

        Config config = ConfigUtils.createConfig();

        config.plans().setInputFile(input.toString());

        config.global().setCoordinateSystem(crs.getInputCRS());
        config.plans().setInputCRS(crs.getInputCRS());

        Scenario scenario = ScenarioUtils.loadScenario(config);

        population = scenario.getPopulation();

        Set<Id<Person>> personsInCityBoundary = new HashSet<>();

        log.info("Number of persons: {}", population.getPersons().size());

        if (shp.getShapeFile() != null) {
            log.info("Using shape file {}", shp.getShapeFile());
            HomeLocationFilter homeLocationFilter = new HomeLocationFilter(shp, crs.getInputCRS(), population);
            for (Person person : population.getPersons().values()) {
                if (homeLocationFilter.considerAgent(person)) {
                    personsInCityBoundary.add(person.getId());
                }
            }

            log.info("The total population size is " + population.getPersons().values().size());
            log.info("The population size inside the city for this simulation setup is " + homeLocationFilter.size());

        } else
            personsInCityBoundary.addAll(population.getPersons().keySet());

        log.info("Persons in boundary: {}", personsInCityBoundary.size());

        Predicate<Id<Person>> condition = personsInCityBoundary::contains;

        List<TripStructureUtils.Trip> originalTrips = population.getPersons().values().stream()
                .filter(person -> condition.test(person.getId())).map(HasPlansAndId::getSelectedPlan)
                .flatMap(person -> TripStructureUtils.getTrips(person.getPlanElements()).stream())
                .collect(Collectors.toList());

        log.info("trip num before augmentation: {}", originalTrips.size());

        int numTrips;
        if (generate.numOfMissingTrips > 0) {
            numTrips = generate.numOfMissingTrips;
        } else {

            if (generate.tripShare <= 0 || generate.tripShare > 1)
                throw new IllegalArgumentException("Trip share must be in (0, 1)");

            double shortDistance = originalTrips.stream().mapToDouble(
                    t -> CoordUtils.calcEuclideanDistance(t.getOriginActivity().getCoord(), t.getDestinationActivity().getCoord())
            ).filter(d -> d <= range).count();

            double actualShare = shortDistance / originalTrips.size();

            log.info("There are {} short distance trips, share is {} and should be {}", shortDistance, actualShare, generate.tripShare);

            // (total + y) * share = short + y
            // --> y = (short - total * share) / (share - 1)

            numTrips = (int) (Math.round(shortDistance - originalTrips.size() * generate.tripShare) / (generate.tripShare - 1));
        }

        log.info("Generating {} trips", numTrips);

        if (numTrips <= 0)
            return 2;

        run(condition, numTrips);

        if (output == null)
            output = Path.of(input.toString().replace(".xml", "-with-trips.xml"));

        PopulationUtils.writePopulation(population, output.toString());

        long totalTrips = population.getPersons().values().stream()
                .filter(person -> condition.test(person.getId())).map(HasPlansAndId::getSelectedPlan)
                .map(plan -> TripStructureUtils.getTrips(plan.getPlanElements())).mapToLong(List::size).sum();

        log.info("trip num after augmentation: {}", totalTrips);

        return 0;
    }

    private void run(Predicate<Id<Person>> addingCondition, int numOfMissingTrips) {

        int addedTrips = 0;

        double probability = computeAddingProbability(numOfMissingTrips, addingCondition);
        log.info("probability of adding trips is: {}", probability);

        if (probability > 1)
            log.warn("Probability is too high, there are not enough activities in the input too enhance them");

        log.info("adding missing trips..........{}", addedTrips);
        for (Person person : population.getPersons().values()) {
            if (addingCondition.test(person.getId())) {

                Plan plan = person.getSelectedPlan();
                var activities = TripStructureUtils.getActivities(plan.getPlanElements(),
                        TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
                var filterActivities = activityFilter(activities);
                List<Activity> markedActivities = new ArrayList<>();

                markedActivities = filterActivities.stream().filter(x -> rnd.nextDouble() < probability)
                        .collect(Collectors.toList());

                if (markedActivities.size() > 0) {
                    Plan newPlan = population.getFactory().createPlan();
                    RoutingModeMainModeIdentifier mainModeIdentifier = new RoutingModeMainModeIdentifier();
                    Activity lastActivity = null;
                    for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan.getPlanElements())) {
                        Activity originActivity = trip.getOriginActivity();
                        if (markedActivities.contains(originActivity)) {

                            addedTrips += 2;

                            if (addedTrips % 1000 == 0) {
                                log.info("adding missing trips..........{}", addedTrips);
                            }
                            // add activity

                            double range = rnd.nextDouble() * this.range;
                            double walkTime = range / 1.2 * 2;
                            double maxDurationForShortDistanceTrips = Math.max(originActivity.getEndTime().seconds()
                                    - originActivity.getStartTime().seconds() - walkTime, 1);
                            double duration = Math.min(this.maxDuration,
                                    rnd.nextDouble() * maxDurationForShortDistanceTrips);
                            double newEndTime = updateEndTime(originActivity, duration, walkTime);

                            Activity activity1 = population.getFactory()
                                    .createActivityFromCoord(originActivity.getType(), originActivity.getCoord());
                            activity1.setLinkId(originActivity.getLinkId());
                            activity1.setStartTime(originActivity.getStartTime().seconds());
                            activity1.setEndTime(newEndTime);
                            newPlan.addActivity(activity1);

                            Leg leg1 = population.getFactory().createLeg(TransportMode.walk);
                            newPlan.addLeg(leg1);

                            Activity shortDistanceRangeActivity = population.getFactory().createActivityFromCoord(
                                    "other_3600.0", getShortDistanceCoordinate(trip.getOriginActivity().getCoord(), range));
                            shortDistanceRangeActivity.setMaximumDuration(duration);
                            shortDistanceRangeActivity.setStartTime(newEndTime + walkTime / 2);
                            shortDistanceRangeActivity.setEndTime(newEndTime + walkTime / 2 + duration);
                            newPlan.addActivity(shortDistanceRangeActivity);

                            Leg leg2 = population.getFactory().createLeg(TransportMode.walk);
                            newPlan.addLeg(leg2);

                            Activity activity2 = population.getFactory()
                                    .createActivityFromCoord(originActivity.getType(), originActivity.getCoord());
                            activity2.setLinkId(originActivity.getLinkId());
                            activity2.setStartTime(updateStartTime(originActivity,
                                    shortDistanceRangeActivity.getEndTime().seconds() + walkTime / 2));
                            activity2.setEndTime(originActivity.getEndTime().seconds());
                            newPlan.addActivity(activity2);

                            String mainMode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
                            Leg leg = population.getFactory().createLeg(mainMode);
                            newPlan.addLeg(leg);

                        } else {
                            newPlan.addActivity(originActivity);
                            String mainMode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
                            Leg leg = population.getFactory().createLeg(mainMode);
                            newPlan.addLeg(leg);
                        }
                        lastActivity = trip.getDestinationActivity();
                    }
                    newPlan.addActivity(lastActivity);
                    person.removePlan(plan);
                    person.addPlan(newPlan);
                }
            }
        }
        log.info("adding missing trips finished, added {} trips", addedTrips);
    }

    private Coord getShortDistanceCoordinate(Coord coord, double range) {
        final double f = Math.sqrt(2) / 2;
        return new Coord(coord.getX() + f * range, coord.getY() + f * range);
    }

    private double updateEndTime(Activity originActivity, double duration, double v) {
        double actDuration = originActivity.getEndTime().seconds() - originActivity.getStartTime().seconds();
        double longestDuration = Math.max(actDuration - duration - v, 0);
        return rnd.nextDouble() * longestDuration + originActivity.getStartTime().seconds();
    }

    private double updateStartTime(Activity originActivity, double v) {
        return Math.min(originActivity.getEndTime().seconds(), v);
    }

    private double computeAddingProbability(int numOfMissingTrips, Predicate<Id<Person>> addingCondition) {
        int numOfAct = 0;
        int numOfTrips = 0;
        for (Person person : population.getPersons().values()) {
            Plan selectedPlan = person.getSelectedPlan();
            person.getPlans().clear();
            person.addPlan(selectedPlan);
            person.setSelectedPlan(selectedPlan);

            if (addingCondition.test(person.getId())) {
                var activities = TripStructureUtils.getActivities(selectedPlan.getPlanElements(),
                        TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
                var trips = TripStructureUtils.getTrips(selectedPlan);
                numOfAct += activityFilter(activities).size();
                numOfTrips += trips.size();
            }
        }
        log.info("activities: " + numOfAct + ", trips: " + numOfTrips + ", missing trips: " + numOfMissingTrips);
        return (double) (numOfMissingTrips) / 2 / numOfAct;
    }

    private List<Activity> activityFilter(List<Activity> activities) {
        // the first, last, and too short activities can not be split to add other
        // activity
        var filterAct = new LinkedList<Activity>();
        if (activities.size() > 2) {
            for (int i = 1; i < activities.size() - 2; i++) {
                double startTime = activities.get(i).getStartTime().seconds();
                double endTime = activities.get(i).getEndTime().seconds();
                if ((endTime - startTime) > range * 2 * 1.5)
                    filterAct.add(activities.get(i));
            }
        }
        return filterAct;
    }

    static class Generate {

        @CommandLine.Option(names = "--num-trips", description = "Number of trips to generate", required = true)
        private int numOfMissingTrips = -1;

        @CommandLine.Option(names = "--trip-share", description = "Generate as many trips needed to match the share", required = true)
        private double tripShare;
    }

}
