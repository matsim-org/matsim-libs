package org.matsim.core.scoring;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.TasteVariationsConfigParameterSet;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PseudoRandomScorerTest {

    private Config config;
    private PseudoRandomTripError tripError;
    private Population population;
    private PopulationFactory factory;
    private static final String MAIN_MODE = "car";

    @BeforeEach
    void setUp() {
        config = ConfigUtils.createConfig();
        config.global().setRandomSeed(1234L);

        tripError = new DefaultPseudoRandomTripError();

        population = PopulationUtils.createPopulation(config);
        factory = population.getFactory();
    }

    @Test
    void testSameTripSamePersonShouldYieldSameScore() {
        // Setup
        DistributionConfig tripConfig = new DistributionConfig(
                TasteVariationsConfigParameterSet.VariationType.gumbel, 1.0);
        PseudoRandomScorer scorer = new PseudoRandomScorer(tripError, config, tripConfig);

        Id<Person> personId = Id.createPersonId("1");
        Trip trip = createTrip("home", "work");

        // Test
        double score1 = scorer.scoreTrip(personId, MAIN_MODE, trip);
        double score2 = scorer.scoreTrip(personId, MAIN_MODE, trip);

        // Verify that the same trip receives the same score
        assertThat(score1).isCloseTo(score2, within(1e-10));
    }

    @Test
    void testDifferentPersonsShouldYieldDifferentScores() {
        // Setup
        DistributionConfig tripConfig = new DistributionConfig(
                TasteVariationsConfigParameterSet.VariationType.gumbel, 1.0);
        PseudoRandomScorer scorer = new PseudoRandomScorer(tripError, config, tripConfig);

        Id<Person> personId1 = Id.createPersonId("1");
        Id<Person> personId2 = Id.createPersonId("2");
        Trip trip = createTrip("home", "work");

        // Test
        double score1 = scorer.scoreTrip(personId1, MAIN_MODE, trip);
        double score2 = scorer.scoreTrip(personId2, MAIN_MODE, trip);

        // Verify that different persons receive different scores for the same trip
        assertThat(score1).isNotEqualTo(score2);
    }

    @Test
    void testDifferentModesShouldYieldDifferentScores() {
        // Setup
        DistributionConfig tripConfig = new DistributionConfig(
                TasteVariationsConfigParameterSet.VariationType.gumbel, 1.0);
        PseudoRandomScorer scorer = new PseudoRandomScorer(tripError, config, tripConfig);

        Id<Person> personId = Id.createPersonId("1");
        Trip trip = createTrip("home", "work");

        // Test
        double score1 = scorer.scoreTrip(personId, "car", trip);
        double score2 = scorer.scoreTrip(personId, "pt", trip);

        // Verify that different modes receive different scores for the same trip
        assertThat(score1).isNotEqualTo(score2);
    }

    @Test
    void testDifferentActivitiesShouldYieldDifferentScores() {
        // Setup
        DistributionConfig tripConfig = new DistributionConfig(
                TasteVariationsConfigParameterSet.VariationType.gumbel, 1.0);
        PseudoRandomScorer scorer = new PseudoRandomScorer(tripError, config, tripConfig);

        Id<Person> personId = Id.createPersonId("1");
        Trip trip1 = createTrip("home", "work");
        Trip trip2 = createTrip("work", "shopping");

        // Test
        double score1 = scorer.scoreTrip(personId, MAIN_MODE, trip1);
        double score2 = scorer.scoreTrip(personId, MAIN_MODE, trip2);

        // Verify that different activities receive different scores for the same person and mode
        assertThat(score1).isNotEqualTo(score2);
    }

    @Test
    void testGumbelDistribution() {
        // Setup
        DistributionConfig tripConfig = new DistributionConfig(
                TasteVariationsConfigParameterSet.VariationType.gumbel, 2.0);
        PseudoRandomScorer scorer = new PseudoRandomScorer(tripError, config, tripConfig);

        Id<Person> personId = Id.createPersonId("1");

        // Generate multiple scores to test distribution
        int numSamples = 10000;

		SummaryStatistics stat = new SummaryStatistics();
        for (int i = 0; i < numSamples; i++) {
            // Create variations of the trip to get different scores
            Trip tripVariation = createTrip("home" + i, "work");
            stat.addValue(scorer.scoreTrip(personId, MAIN_MODE, tripVariation));
        }

        // For Gumbel, verify that distribution has reasonable characteristics
        // The mean of Gumbel distribution with loc=0, scale=2 should be approximately 2*Euler's constant
        double expectedMean = 2.0 * 0.57721; // 0.57721 is Euler's constant

        // Check that mean is within reasonable bounds (not exact due to small sample)
        assertThat(Math.abs(stat.getMean() - expectedMean)).isLessThan(0.5);
    }

    @Test
    void testNormalDistribution() {
        // Setup
        DistributionConfig tripConfig = new DistributionConfig(
                TasteVariationsConfigParameterSet.VariationType.normal, 1.0);
        PseudoRandomScorer scorer = new PseudoRandomScorer(tripError, config, tripConfig);

        Id<Person> personId = Id.createPersonId("1");

        // Generate multiple scores to test distribution
        int numSamples = 10000;

		SummaryStatistics stat = new SummaryStatistics();
		for (int i = 0; i < numSamples; i++) {
            // Create variations of the trip to get different scores
            Trip tripVariation = createTrip("home" + i, "work" + i);
            stat.addValue(scorer.scoreTrip(personId, MAIN_MODE, tripVariation));
        }

        // Check that mean is close to 0 (not exact due to small sample)
        assertThat(Math.abs(stat.getMean())).isCloseTo(0, within(0.1));

        // Check that standard deviation is close to 1.0 (the scale parameter)
        assertThat(stat.getVariance()).isCloseTo(1.0, within(0.1));
    }

    @Test
    void testZeroScaleYieldsZeroScore() {
        // Setup with scale = 0
        DistributionConfig tripConfig = new DistributionConfig(
                TasteVariationsConfigParameterSet.VariationType.gumbel, 0.0);
        PseudoRandomScorer scorer = new PseudoRandomScorer(tripError, config, tripConfig);

        Id<Person> personId = Id.createPersonId("1");
        Trip trip = createTrip("home", "work");

        // Test
        double score = scorer.scoreTrip(personId, MAIN_MODE, trip);

        // Verify that zero scale yields zero score
        assertThat(score).isZero();
    }

    @Test
    void testNullDistributionYieldsZeroScore() {
        // Setup with null distribution
        DistributionConfig tripConfig = new DistributionConfig(null, 1.0);
        PseudoRandomScorer scorer = new PseudoRandomScorer(tripError, config, tripConfig);

        Id<Person> personId = Id.createPersonId("1");
        Trip trip = createTrip("home", "work");

        // Test
        double score = scorer.scoreTrip(personId, MAIN_MODE, trip);

        // Verify that null distribution yields zero score
        assertThat(score).isZero();
    }

    @Test
    void testTripScoringFunction() {
        // Setup
        Id<Person> personId = Id.createPersonId("1");
        DistributionConfig tripConfig = new DistributionConfig(
                TasteVariationsConfigParameterSet.VariationType.gumbel, 1.0);
        PseudoRandomScorer randomScorer = new PseudoRandomScorer(tripError, config, tripConfig);

        // Setup main mode identifier that just returns our constant
        MainModeIdentifier mmi = (legs) -> MAIN_MODE;

        // Create scoring function
        PseudoRandomTripScoring scoring = new PseudoRandomTripScoring(personId, mmi, randomScorer);

        // Create trips
        Trip trip1 = createTrip("home", "work");
        Trip trip2 = createTrip("work", "shop");
        Trip trip3 = createTrip("shop", "home");

        // Calculate expected scores directly
        double expectedScore1 = randomScorer.scoreTrip(personId, MAIN_MODE, trip1);
        double expectedScore2 = randomScorer.scoreTrip(personId, MAIN_MODE, trip2);
        double expectedScore3 = randomScorer.scoreTrip(personId, MAIN_MODE, trip3);
        double expectedTotal = expectedScore1 + expectedScore2 + expectedScore3;

        // Score the trips using the scoring function
        scoring.handleTrip(trip1);
        scoring.handleTrip(trip2);
        scoring.handleTrip(trip3);

        // Verify total score
        assertThat(scoring.getScore()).isCloseTo(expectedTotal, within(1e-10));

        // Verify explanation
        StringBuilder explanation = new StringBuilder();
        scoring.explainScore(explanation);
        String explStr = explanation.toString();

        assertThat(explStr).startsWith("trips_util=")
                         .contains("trip_0=")
                         .contains("trip_1=")
                         .contains("trip_2=");
    }

    private Trip createTrip(String originType, String destType) {
        Activity origin = factory.createActivityFromCoord(originType, new Coord(0, 0));
        Leg leg = factory.createLeg(MAIN_MODE);
        Activity destination = factory.createActivityFromCoord(destType, new Coord(1000, 1000));

        Plan plan = factory.createPlan();
        plan.addActivity(origin);
        plan.addLeg(leg);
        plan.addActivity(destination);

        // Extract the trip using TripStructureUtils
        List<Trip> trips = TripStructureUtils.getTrips(plan);
        return trips.get(0);
    }
}
