package org.matsim.application.automatedCalibration.modeChoiceEstimation;

import org.apache.commons.lang3.mutable.MutableInt;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.*;

public class DefaultModeChoiceEstimator {
    private final Config config;
    private final String workingFolder;

    public static void main(String[] args) {
        new DefaultModeChoiceEstimator(args[0], args[1]).estimateModeChoice();
    }

    public DefaultModeChoiceEstimator(String configPath, String workingFolder) {
        this.config = ConfigUtils.loadConfig(configPath);
        this.workingFolder = workingFolder;
    }

    public void estimateModeChoice() {
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();
        TravelCostCalculator travelCostCalculator = new TravelCostCalculator(workingFolder);

        Map<String, MutableInt> tripModeCounts = new HashMap<>();
        tripModeCounts.put(TransportMode.car, new MutableInt(0));
        tripModeCounts.put(TransportMode.ride, new MutableInt(0));
        tripModeCounts.put(TransportMode.pt, new MutableInt(0));
        tripModeCounts.put(TransportMode.bike, new MutableInt(0));
        tripModeCounts.put(TransportMode.walk, new MutableInt(0));

        Population outputPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        PopulationFactory populationFactory = PopulationUtils.getFactory();

//        int counter = 0;
        for (Person person : population.getPersons().values()) {
            if (person.getId().toString().startsWith("freight")) {
                continue;
            }

//            counter++;
//            if (counter > 9) {
//                break;
//            }

            Person outputPerson = populationFactory.createPerson(person.getId());
            for (Map.Entry<String, Object> entry : person.getAttributes().getAsMap().entrySet()) {
                outputPerson.getAttributes().putAttribute(entry.getKey(), entry.getValue());
            }

            Plan outputPlan = populationFactory.createPlan();
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
            Map<TripStructureUtils.Trip, Map<String, Double>> tripCostsForPerson = new LinkedHashMap<>();
            for (int i = 0; i < trips.size(); i++) {
                String tripIdString = person.getId().toString() + "_" + i;
                TripStructureUtils.Trip trip = trips.get(i);
                Activity startAct = trip.getOriginActivity();
                Activity endAct = trip.getDestinationActivity();
                if (startAct.getEndTime().orElse(Double.MAX_VALUE) < 108000 && endAct.getStartTime().orElse(Double.MAX_VALUE) < 108000) {
                    Map<String, Double> tripsCost = travelCostCalculator.calculateTravelCost(config, tripIdString, startAct, endAct);
                    tripCostsForPerson.put(trip, tripsCost);
                }
            }

            // without car
            String[] modesWithoutCar = new String[]{TransportMode.ride, TransportMode.pt, TransportMode.bike, TransportMode.walk};
            List<String> modeChoiceWithoutCar = new ArrayList<>();
            double dailyTotalCostWithoutCar = 0;
            for (Map<String, Double> tripsCost : tripCostsForPerson.values()) {
                double minCost = Double.MAX_VALUE;
                String chosenMode = "";
                for (String mode : modesWithoutCar) {
                    double cost = tripsCost.get(mode);
                    if (cost < minCost) {
                        minCost = cost;
                        chosenMode = mode;
                    }
                }
                dailyTotalCostWithoutCar += minCost;
                modeChoiceWithoutCar.add(chosenMode);
            }

//            System.out.println("Daily augmented cost without car for agent " + person.getId().toString() + " is " + dailyTotalCostWithoutCar);
//            System.out.println("The mode choice is as follow: ");
//            for (String mode : modeChoiceWithoutCar) {
//                System.out.println(mode);
//            }

            // with car
            List<String> modeChoiceWithCar = new ArrayList<>();
            double dailyTotalCostWithCar = config.planCalcScore().getModes().get(TransportMode.car).getDailyMonetaryConstant() * -1;
            for (Map<String, Double> tripsCost : tripCostsForPerson.values()) {
                double minCost = Double.MAX_VALUE;
                String chosenMode = "";
                for (String mode : tripsCost.keySet()) {
                    double cost = tripsCost.get(mode);
                    if (cost < minCost) {
                        minCost = cost;
                        chosenMode = mode;
                    }
                }
                dailyTotalCostWithCar += minCost;
                modeChoiceWithCar.add(chosenMode);
            }

//            System.out.println("Daily augmented cost with car for agent " + person.getId().toString() + " is " + dailyTotalCostWithCar);
//            System.out.println("The mode choice is as follow: ");
//            for (String mode : modeChoiceWithCar) {
//                System.out.println(mode);
//            }
//            System.out.println("==============================================");

            int legCounter = 0;
            for (TripStructureUtils.Trip trip : tripCostsForPerson.keySet()) {
                if (legCounter == 0) {
                    outputPlan.addActivity(trip.getOriginActivity());
                }

                String chosenMode;
                if (dailyTotalCostWithCar < dailyTotalCostWithoutCar && PersonUtils.getCarAvail(person).equals("always")) {
                    chosenMode = modeChoiceWithCar.get(legCounter);
                } else {
                    chosenMode = modeChoiceWithoutCar.get(legCounter);
                }
                outputPlan.addLeg(populationFactory.createLeg(chosenMode));
                tripModeCounts.get(chosenMode).increment();

                outputPlan.addActivity(trip.getDestinationActivity());

                legCounter++;
            }
            outputPerson.addPlan(outputPlan);
            outputPopulation.addPerson(outputPerson);
        }

        PopulationWriter populationWriter = new PopulationWriter(outputPopulation);
        populationWriter.write(workingFolder + "/output/estimated_output_population.xml.gz");

        for (String mode : tripModeCounts.keySet()) {
            System.out.println("Mode " + mode + ": " + tripModeCounts.get(mode).intValue() + " trips");
        }
    }
}
