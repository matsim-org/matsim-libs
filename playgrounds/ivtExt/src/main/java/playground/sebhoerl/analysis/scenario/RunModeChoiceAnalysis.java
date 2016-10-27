package playground.sebhoerl.analysis.scenario;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import playground.sebhoerl.analysis.aggregate_events.AggregateReader;
import playground.sebhoerl.analysis.aggregate_events.DefaultAggregateHandler;
import playground.sebhoerl.analysis.aggregate_events.Trip;

public class RunModeChoiceAnalysis {
    public static class TripHandler extends DefaultAggregateHandler {
        public Map<Id<Person>, LinkedList<Trip>> tripsPerPerson = new HashMap<>();
        
        @Override
        public void handleTrip(Trip trip) {
            Id<Person> person = trip.getPerson();
            
            if (person.toString().contains("pt")) return;
            if (person.toString().contains("av")) return;
            
            if (!tripsPerPerson.containsKey(person)) {
                tripsPerPerson.put(person, new LinkedList<Trip>());
            }
            
            tripsPerPerson.get(person).add(trip);
        }
    }
    
    static class Change {
        public String originalMode;
        public String updatedMode;
        
        public double originalDistance;
        public double updatedDistance;
        public double originalTime;
        public double updatedTime;
        
        public Change(Trip before, Trip after) {
            originalMode = before.getMode();
            updatedMode = after.getMode();
            originalDistance = before.getDistance();
            updatedDistance = after.getDistance();
            originalTime = before.getEndTime() - before.getStartTime();
            updatedTime = after.getEndTime() - after.getStartTime();
        }
    }
    
    static class NoChange {
        public String mode;
        public double distance;
        public double time;
        
        public NoChange(Trip before) {
            mode = before.getMode();
            distance = before.getDistance();
            time = before.getEndTime() - before.getStartTime();
        }
    }
    
    public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        (new PopulationReader(scenario)).readFile(args[0]);
        
        // Read aggregated trips
        AggregateReader reader = new AggregateReader();
        TripHandler handler = new TripHandler();
        reader.addHandler(handler);
        reader.read(args[1]);
        
        // Read trips from population
        Population population = scenario.getPopulation();
        Map<Id<Person>, LinkedList<Trip>> beforeTrips = new HashMap<>();
        Trip current = null;
        
        for (Person person : population.getPersons().values()) {            
            beforeTrips.put(person.getId(), new LinkedList<Trip>());
            current = null;
            
            for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
                if (element instanceof Leg && current == null) {
                    Leg leg = (Leg)element;
                    current = new Trip();
                    
                    if (leg.getMode().equals("transit_walk")) {
                        current.setMode("pt");
                    } else {
                        current.setMode(leg.getMode());
                    }
                    
                    current.setDistance(leg.getRoute().getDistance());
                    current.setStartTime(0.0);
                    current.setEndTime(leg.getRoute().getTravelTime());
                } else if (element instanceof Leg && current != null) {
                    Leg leg = (Leg)element;
                    
                    current.setDistance(current.getDistance() + leg.getRoute().getDistance());
                    current.setEndTime(current.getEndTime() + leg.getRoute().getTravelTime());
                } else if (element instanceof Activity && current != null) {
                    Activity activity = (Activity)element;
                    
                    if (!activity.getType().equals("pt interaction")) {
                        beforeTrips.get(person.getId()).add(current);
                        current = null;
                    }
                }
            }
            
            if (current != null) {
                beforeTrips.get(person.getId()).add(current);
            }
        }
        
        // Compare the changes
        ArrayList<String> modes = new ArrayList<>(Arrays.asList("av", "car", "pt", "walk"));
        final long movements[][] = new long[modes.size()][modes.size()];
        
        final LinkedList<Change> changes = new LinkedList<Change>();
        final LinkedList<NoChange> noChanges = new LinkedList<NoChange>();
        
        for (Id<Person> person : population.getPersons().keySet()) {
            if (!handler.tripsPerPerson.containsKey(person)) {
                throw new RuntimeException();
            }
            
            LinkedList<Trip> before = beforeTrips.get(person);
            LinkedList<Trip> after = handler.tripsPerPerson.get(person);
            
            if (before.size() != after.size()) {
                throw new RuntimeException();
            }
            
            for (int i = 0; i < before.size(); i++) {
                String beforeMode = before.get(i).getMode();
                String afterMode = after.get(i).getMode();
                
                movements[modes.indexOf(beforeMode)][modes.indexOf(afterMode)] += 1;
                
                if (beforeMode.equals(afterMode)) {
                    noChanges.add(new NoChange(before.get(i)));
                } else {
                    changes.add(new Change(before.get(i), after.get(i)));
                }
            }
        }
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(args[2]), Arrays.asList(modes, movements, changes, noChanges));
    }
}

















