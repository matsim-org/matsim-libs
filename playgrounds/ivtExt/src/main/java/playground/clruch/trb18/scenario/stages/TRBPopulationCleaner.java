package playground.clruch.trb18.scenario.stages;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

public class TRBPopulationCleaner {
    final private Logger logger = Logger.getLogger(TRBPopulationCleaner.class);

    /**
     * Prepares a scenario for further processing for the AV framework
     *
     * - Removes all agents that just have one activity
     * - Removes all non-selected plans
     * - Merges all PT trips (with multiple stages) into one unrouted PT leg
     * - Removes all routes
     *
     * The original population is not touched! If later, one wants to reconstruct the routed PT legs, they are still
     * available in the original population.
     */
    public Population clean(Population originalPopulation) {
        Population cleanedPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());

        logger.info("Cloning population and removing unselected plans and all routes ...");

        for (Person originalPerson : originalPopulation.getPersons().values()) {
            if (originalPerson.getSelectedPlan().getPlanElements().size() > 1) {
                Person duplicatePerson = cleanedPopulation.getFactory().createPerson(originalPerson.getId());
                cleanedPopulation.addPerson(duplicatePerson);

                Plan selectedPlan = originalPerson.getSelectedPlan();

                Plan duplicatedPlan = PlanUtils.createCopy(selectedPlan);
                duplicatedPlan.setPerson(duplicatePerson);
                duplicatePerson.addPlan(duplicatedPlan);

                for (PlanElement element : duplicatedPlan.getPlanElements()) {
                    if (element instanceof Leg) {
                        Leg leg = (Leg) element;
                        leg.setRoute(null);
                    }
                }
            }
        }

        logger.info("  Number of agents in original population: " + originalPopulation.getPersons().size());
        logger.info("  Number of agents in cleaned population (stay-home-agents removed): " + cleanedPopulation.getPersons().size());

        logger.info("Merging PT trips into single PT legs ...");

        long numberofRemovedTripElements = 0;
        long numberOfPtTrips = 0;
        long numberOfGeneratedWalkTrips = 0;

        for (Person person : cleanedPopulation.getPersons().values()) {
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan(), new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));

            for (TripStructureUtils.Trip trip : trips) {
                Leg firstLeg = trip.getLegsOnly().get(0);

                if (firstLeg.getMode().equals("pt") || firstLeg.getMode().equals("transit_walk")) {
                    for (int i = 1; i < trip.getTripElements().size(); i++) {
                        person.getSelectedPlan().getPlanElements().remove(trip.getTripElements().get(i));
                        numberofRemovedTripElements++;
                    }

                    firstLeg.setTravelTime(Math.max(0.0, trip.getDestinationActivity().getStartTime() - trip.getOriginActivity().getEndTime()));
                    firstLeg.setRoute(null);
                    firstLeg.setDepartureTime(trip.getOriginActivity().getEndTime());
                    firstLeg.setMode((firstLeg.getMode().equals("transit_walk") && trip.getLegsOnly().size() == 1) ? "walk" : "pt");

                    numberOfPtTrips++;
                    if (firstLeg.getMode().equals("walk")) numberOfGeneratedWalkTrips++;
                }
            }
        }

        logger.info(String.format("  Number of removed trip elements: %d", numberofRemovedTripElements));
        logger.info(String.format("  Original number of public transit trips: %d", numberOfPtTrips));
        logger.info(String.format("  Number of trips converted to walk: %d (%.2f%%)", numberOfGeneratedWalkTrips, 100.0 * numberOfGeneratedWalkTrips / numberOfPtTrips));

        return cleanedPopulation;
    }

    /**
     * For testing purposes
     */
    static public void main(String[] args) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(args[0]);
        new PopulationWriter(new TRBPopulationCleaner().clean(scenario.getPopulation())).write(args[1]);
    }
}
