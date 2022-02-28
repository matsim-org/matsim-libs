package org.matsim.application.automatedCalibration.modeChoiceEstimation;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TravelCostCalculator {
    private final static String[] modes = new String[]{TransportMode.car, TransportMode.ride, TransportMode.pt, TransportMode.bike, TransportMode.walk};
    private final Map<String, List<Leg>> ptTripMap = new HashMap<>(); // TODO read from pre-calculated output plans
    private final Map<String, List<Leg>> carTripMap = new HashMap<>(); // TODO read from pre-calculated output plans

    public TravelCostCalculator(String folder) {
        readPtTrips(folder);
        readCarTrips(folder);
    }

    private void readCarTrips(String folder) {
        System.out.println("Reading pre-calculated PT plans");
        Population population = PopulationUtils.readPopulation(folder + "/pt/output_experienced_plans.xml.gz");
        for (Person person : population.getPersons().values()) {
            String tripIdString = person.getId().toString();
//            assert TripStructureUtils.getTrips(person.getSelectedPlan()).size() == 1;
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
            if (trips.size() == 1) {
                TripStructureUtils.Trip trip = trips.get(0);
                ptTripMap.put(tripIdString, trip.getLegsOnly());
            } else {
                System.err.println("Warning" + person.getId().toString() + " has " + trips.size() + " trips. This trip is skipped");
            }
        }
    }

    private void readPtTrips(String folder) {
        System.out.println("Reading pre-calculated car plans");
        Population population = PopulationUtils.readPopulation(folder + "/car/output_experienced_plans.xml.gz");
        for (Person person : population.getPersons().values()) {
            String tripIdString = person.getId().toString();
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
            if (trips.size() == 1) {
                TripStructureUtils.Trip trip = trips.get(0);
                carTripMap.put(tripIdString, trip.getLegsOnly());
            } else {
                System.err.println("Warning" + person.getId().toString() + " has " + trips.size() + " trips. This trip is skipped");
            }
        }
    }

    public Map<String, Double> calculateTravelCost(Config config, String tripIdString, Activity startAct, Activity endAct) {
        double originalTravelTime = endAct.getStartTime().orElseThrow(RuntimeException::new) - startAct.getEndTime().orElseThrow(RuntimeException::new);
        double durationEndAct = endAct.getEndTime().orElse(108000) - endAct.getStartTime().orElseThrow(RuntimeException::new);
        durationEndAct = Math.max(durationEndAct, 1);
        durationEndAct = ((durationEndAct - 1) % 600 + 1) * 600; // round up to 10 minutes interval
        Map<String, Double> tripCosts = new HashMap<>();
        for (String mode : modes) {
            if (mode.equals(TransportMode.pt)) {
                double cost = 0;
                double totalTravelTime = 0;
                boolean ptIsUsed = false;
                for (Leg leg : ptTripMap.get(tripIdString)) {
                    String subLegMode = leg.getMode();
                    double subLegTravelTime = leg.getTravelTime().orElseThrow(RuntimeException::new);
                    double subLegDistance = leg.getRoute().getDistance();

                    totalTravelTime += subLegTravelTime;
                    if (subLegMode.equals(TransportMode.pt)) {
                        ptIsUsed = true;
                    }

                    cost = cost + calculateSingleLegCost(subLegMode, subLegTravelTime, subLegDistance, config);
                }

                if (ptIsUsed) {
                    double totalCost = cost + calculateChangeInOpportunityCost(originalTravelTime, totalTravelTime, durationEndAct, config);
                    // TODO add pt fare
                    tripCosts.put(TransportMode.pt, totalCost);
                } else {
                    tripCosts.put(TransportMode.pt, Double.MAX_VALUE); // PT is not feasible
                }
            }

            if (mode.equals(TransportMode.car) || mode.equals(TransportMode.ride)) {
                double cost = 0;
                double totalTravelTime = 0;

                for (Leg leg : carTripMap.get(tripIdString)) {
                    String subLegMode = leg.getMode();
                    if (subLegMode.equals(TransportMode.car)) {
                        subLegMode = mode;  // car or ride
                    }
                    double subLegTravelTime = leg.getTravelTime().orElseThrow(RuntimeException::new);
                    //TODO delete this start
                    if (subLegMode.equals(TransportMode.car) || subLegMode.equals(TransportMode.ride)) {
                        subLegTravelTime *= 1.1;
                    }
                    //TODO end
                    double subLegDistance = leg.getRoute().getDistance();
                    totalTravelTime += subLegTravelTime;
                    cost = cost + calculateSingleLegCost(subLegMode, subLegTravelTime, subLegDistance, config);
                }
                double totalCost = cost + calculateChangeInOpportunityCost(originalTravelTime, totalTravelTime, durationEndAct, config);
                tripCosts.put(mode, totalCost);
            }

            if (mode.equals(TransportMode.walk) || mode.equals(TransportMode.bike)) {  // Teleported modes --> calculate directly
                double travelDistance = CoordUtils.calcEuclideanDistance(startAct.getCoord(), endAct.getCoord());
                double speed = config.plansCalcRoute().getTeleportedModeSpeeds().get(mode);
                double distanceFactor = config.plansCalcRoute().getBeelineDistanceFactors().get(mode);
                double travelTime = travelDistance * distanceFactor / speed;

                double cost = calculateSingleLegCost(mode, travelTime, travelDistance * distanceFactor, config);
                double totalCost = cost + calculateChangeInOpportunityCost(originalTravelTime, travelTime, durationEndAct, config);
                tripCosts.put(mode, totalCost);
            }
        }
        return tripCosts;
    }


    private double calculateChangeInOpportunityCost(double originalTravelTime, double actualTravelTime, double endActivityTypicalDuration, Config config) {
        double extraActivityPerformingTime = originalTravelTime - actualTravelTime; //This can be negative (i.e., reduced activity performing time)
        if (extraActivityPerformingTime <= -0.95 * endActivityTypicalDuration) {
            extraActivityPerformingTime = -0.95 * endActivityTypicalDuration;
        }
        double gainedScore = endActivityTypicalDuration * config.planCalcScore().getPerforming_utils_hr() / 3600 * Math.log(1 + extraActivityPerformingTime / endActivityTypicalDuration);
        return -1 * gainedScore;  // * -1 --> In terms of "Cost", which we try to minimize
    }

    private double calculateSingleLegCost(String mode, double travelTime, double travelDistance, Config config) {
        double constantCost = config.planCalcScore().getModes().get(mode).getConstant();
        double marginalCost = config.planCalcScore().getModes().get(mode).getMarginalUtilityOfTraveling();
        double distanceCost = config.planCalcScore().getModes().get(mode).getMonetaryDistanceRate() * config.planCalcScore().getMarginalUtilityOfMoney()
                + config.planCalcScore().getModes().get(mode).getMarginalUtilityOfDistance(); //
        return -1 * (constantCost + marginalCost * travelTime / 3600 + distanceCost * travelDistance);
    }
}
