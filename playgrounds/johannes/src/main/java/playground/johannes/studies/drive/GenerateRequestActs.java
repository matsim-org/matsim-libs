package playground.johannes.studies.drive;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Created by johannesillenberger on 13.04.17.
 */
public class GenerateRequestActs {

    private static final Logger logger = Logger.getLogger(GenerateRequestActs.class);

    public static void main(String args[]) throws IOException {
        String popInFile = args[0];
        double proba = Double.parseDouble(args[1]);
        String popDefaultOutFile = args[2];
        String popReqOutFile = args[3];
        String requestsIdsFile = args[4];
        String selectedIdsFile = args[5];
        String nonSelectedIdsFile = args[6];

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        /*
        Load persons...
         */
        logger.info("Loading population...");
        PopulationReader popReader = new PopulationReader(scenario);
        popReader.readFile(popInFile);
        /*
        Select trips...
         */
        Random random = new XORShiftRandom();
        PopulationFactory factory = scenario.getPopulation().getFactory();
        Set<Person> requests = new HashSet<>();
        Set<Person> selected = new HashSet<>();
        Set<Person> nonSelected = new HashSet<>();

        for(Person person : scenario.getPopulation().getPersons().values()) {
            for(Plan plan : person.getPlans()) {
                for(int i = 1; i < plan.getPlanElements().size(); i += 2) {
                    if(random.nextDouble() < proba) {
                        Activity prev = (Activity) plan.getPlanElements().get(i - 1);
                        Leg leg = (Leg)plan.getPlanElements().get(i);
                        Activity next = (Activity) plan.getPlanElements().get(i + 1);
                        /*

                         */
                        Plan newPlan = factory.createPlan();
                        Person newPerson = factory.createPerson(
                                Id.createPersonId(String.format("req%s", requests.size())));
                        newPerson.addPlan(newPlan);

                        Activity startAct = factory.createActivityFromCoord("request", prev.getCoord());
                        startAct.setStartTime(prev.getEndTime());
                        startAct.setEndTime(startAct.getStartTime() + 1);
                        newPlan.addActivity(startAct);

                        Leg newLeg = factory.createLeg("car");
                        newLeg.setDepartureTime(leg.getDepartureTime());
                        newLeg.setRoute(leg.getRoute());
                        newLeg.setTravelTime(leg.getTravelTime());
                        newPlan.addLeg(newLeg);

                        Activity endAct = factory.createActivityFromCoord("arrival", next.getCoord());
                        endAct.setStartTime(next.getStartTime());
                        endAct.setEndTime(86400);
                        newPlan.addActivity(endAct);

                        requests.add(newPerson);
                        selected.add(person);
                    } else {
                        nonSelected.add(person);
                    }
                }
            }
        }

        logger.info(String.format("Selected %s persons, generated %s requests.", selected.size(), requests.size()));
        for(Person person : selected) {
            scenario.getPopulation().removePerson(person.getId());
        }
//        for(Person person : requests) scenario.getPopulation().addPerson(person);
//        Scenario newScenario = ScenarioUtils.createScenario(config);
        for(Person person : requests) {
//            newScenario.getPopulation().addPerson(person);
            scenario.getPopulation().addPerson(person);
        }


        /*
        Write persons...
         */
        logger.info("Writing default population...");
        PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation());
        populationWriter.writeV5(popDefaultOutFile);
//        logger.info("Writing request population...");
//        populationWriter = new PopulationWriter(newScenario.getPopulation());
//        populationWriter.writeV5(popReqOutFile);
        /*
        Write id lists...
         */
        logger.info("Writing id lists...");
        BufferedWriter writer = new BufferedWriter(new FileWriter(requestsIdsFile));
        for(Person person : requests) {
            writer.write(person.getId().toString());
            writer.newLine();
        }
        writer.close();

        writer = new BufferedWriter(new FileWriter(selectedIdsFile));
        for(Person person : selected) {
            writer.write(person.getId().toString());
            writer.newLine();
        }
        writer.close();

        writer = new BufferedWriter(new FileWriter(nonSelectedIdsFile));
        for(Person person : nonSelected) {
            writer.write(person.getId().toString());
            writer.newLine();
        }
        writer.close();
        logger.info("Done.");
    }
}
