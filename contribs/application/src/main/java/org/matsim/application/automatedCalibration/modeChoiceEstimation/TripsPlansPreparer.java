package org.matsim.application.automatedCalibration.modeChoiceEstimation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.analysis.DefaultAnalysisMainModeIdentifier;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TripsPlansPreparer {
    private static final String INPUT_PLAN = "/Users/luchengqi/Documents/MATSimScenarios/Kelheim/auto-calibration/kelheim-v1.4-25pct.plans.xml.gz";
    private static final String OUTPUT_FOLDER = "/Users/luchengqi/Documents/MATSimScenarios/Kelheim/auto-calibration/trips-plans";

    private static final String[] modes = new String[]{TransportMode.car, TransportMode.pt};

    public static void main(String[] args) throws IOException {
        Population inputPopulation = PopulationUtils.readPopulation(INPUT_PLAN);
        PopulationFactory populationFactory = PopulationUtils.getFactory();
        MainModeIdentifier mainModeIdentifier = new DefaultAnalysisMainModeIdentifier();
        if (!Files.exists(Path.of(OUTPUT_FOLDER))) {
            Files.createDirectory(Path.of(OUTPUT_FOLDER));
        }

        for (String mode : modes) {
            Population outputPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());
            for (Person person : inputPopulation.getPersons().values()) {
                int tripCounter = 0;
                List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
                for (TripStructureUtils.Trip trip : trips) {
                    if (mainModeIdentifier.identifyMainMode(trip.getTripElements()).equals("freight")) {
                        continue;
                    }
                    Activity startAct = trip.getOriginActivity();
                    Activity endAct = trip.getDestinationActivity();
                    String outputPersonIdString = person.getId().toString() + "_" + tripCounter;
                    Person outputPerson = populationFactory.createPerson(Id.createPersonId(outputPersonIdString));
                    Plan plan = populationFactory.createPlan();
                    plan.addActivity(startAct);
                    plan.addLeg(populationFactory.createLeg(mode));
                    plan.addActivity(endAct);
                    outputPerson.addPlan(plan);
                    outputPopulation.addPerson(outputPerson);
                    tripCounter++;
                }
            }

            PopulationWriter populationWriter = new PopulationWriter(outputPopulation);
            populationWriter.write(OUTPUT_FOLDER + "/" + mode + "-trips.plans.xml.gz");
        }
    }

}
