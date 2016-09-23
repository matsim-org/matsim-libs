package playground.sebhoerl.av_paper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import playground.sebhoerl.analysis.aggregate_events.AggregateReader;
import playground.sebhoerl.analysis.aggregate_events.Trip;
import playground.sebhoerl.analysis.scenario.RunModeChoiceAnalysis.TripHandler;

public class RunTripAnalysis {
    public static class TripPerPersonHandler implements EventsToTrips.TripHandler {
        public Map<Id<Person>, LinkedList<PersonExperiencedTrip>> tripsPerPerson = new HashMap<>();
        private boolean active = false;
        
        @Override
        public void handleTrip(PersonExperiencedTrip trip) {
        	if (active) {
	            Id<Person> person = trip.getPerson();
	            
	            if (person.toString().contains("pt")) return;
	            if (person.toString().contains("av")) return;
	            
	            if (!tripsPerPerson.containsKey(person)) {
	                tripsPerPerson.put(person, new LinkedList<PersonExperiencedTrip>());
	            }
	            
	            tripsPerPerson.get(person).add(trip);
        	}
        }
        
        public void setActivte(boolean active) {
        	this.active = active;
        }
    }
    
    static class Change {
        public String originalMode;
        public String updatedMode;
        
        public double originalDistance;
        public double updatedDistance;
        public double originalTime;
        public double updatedTime;
        
        public double originalStartTime;
        public double originalEndTime;
        public double updatedStartTime;
        public double updatedEndTime;
        
        public boolean carAvailable;
        
        public Change(Person person, PersonExperiencedTrip before, PersonExperiencedTrip after) {
            originalMode = before.getMode();
            updatedMode = after.getMode();
            originalDistance = before.getDistance();
            updatedDistance = after.getDistance();
            originalTime = before.getEndTime() - before.getStartTime();
            updatedTime = after.getEndTime() - after.getStartTime();
            carAvailable = person.getCustomAttributes().get("carAvail").equals("always");
            
            originalStartTime = before.getStartTime();
            originalEndTime = before.getEndTime();
            updatedStartTime = after.getStartTime();
            updatedEndTime = after.getEndTime();
        }
    }
    
    static class NoChange {
        public String mode;
        public double distance;
        public double time;
        
        public boolean carAvailable;
        
        public double originalStartTime;
        public double originalEndTime;
        public double updatedStartTime;
        public double updatedEndTime;
        
        public NoChange(Person person, PersonExperiencedTrip before, PersonExperiencedTrip after) {
            mode = before.getMode();
            distance = before.getDistance();
            time = before.getEndTime() - before.getStartTime();
            carAvailable = person.getCustomAttributes().get("carAvail").equals("always");
            
            originalStartTime = before.getStartTime();
            originalEndTime = before.getEndTime();
            updatedStartTime = after.getStartTime();
            updatedEndTime = after.getEndTime();
        }
    }
    
    public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
    	run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km4/0.xml.gz", "/home/sebastian/paper/km/results/km4/1000.xml.gz", "/home/sebastian/paper/km/results/km4/choice1000.json");
    }
    	
    public static void run(String configPath, String beforePath, String afterPath, String outputPath) throws JsonGenerationException, JsonMappingException, IOException {
        Config config = ConfigUtils.loadConfig(configPath);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();
        
        EventsManagerImpl events = new EventsManagerImpl();
        MatsimEventsReader reader = new MatsimEventsReader(events);
        
        EventsToLegs events2legs = new EventsToLegs(scenario);
        EventsToActivities events2activities = new EventsToActivities();
        EventsToTrips events2trips = new EventsToTrips(events2legs, events2activities);
        
        events.addHandler(events2legs);
        events.addHandler(events2activities);
        
        TripPerPersonHandler beforeHandler = new TripPerPersonHandler();
        TripPerPersonHandler afterHandler = new TripPerPersonHandler();
        
        events2trips.addTripHandler(beforeHandler);
        events2trips.addTripHandler(afterHandler);
        
        beforeHandler.setActivte(true);
        afterHandler.setActivte(false);
        reader.readFile(beforePath);
        events2trips.finalize();
        
        beforeHandler.setActivte(false);
        afterHandler.setActivte(true);
        reader.readFile(afterPath);
        events2trips.finalize();
        
        // Compare the changes
        ArrayList<String> modes = new ArrayList<>(Arrays.asList("av", "car", "pt", "walk"));
        final long movements[][] = new long[modes.size()][modes.size()];
        
        final LinkedList<Change> changes = new LinkedList<Change>();
        final LinkedList<NoChange> noChanges = new LinkedList<NoChange>();
        
        for (Person person : population.getPersons().values()) {
        	
            if (person.toString().contains("pt")) continue;
            if (person.toString().contains("av")) continue;
            
            if (!beforeHandler.tripsPerPerson.containsKey(person.getId())) {
                throw new RuntimeException();
            }
            
            if (!afterHandler.tripsPerPerson.containsKey(person.getId())) {
                throw new RuntimeException();
            }
            
            LinkedList<PersonExperiencedTrip> before = beforeHandler.tripsPerPerson.get(person.getId());
            LinkedList<PersonExperiencedTrip> after = afterHandler.tripsPerPerson.get(person.getId());
            
            if (before.size() != after.size()) {
                throw new RuntimeException();
            }
            
            for (int i = 0; i < before.size(); i++) {
                String beforeMode = before.get(i).getMode();
                String afterMode = after.get(i).getMode();
                
                movements[modes.indexOf(beforeMode)][modes.indexOf(afterMode)] += 1;
                
                if (beforeMode.equals(afterMode)) {
                    noChanges.add(new NoChange(person, before.get(i), after.get(i)));
                } else {
                    changes.add(new Change(person, before.get(i), after.get(i)));
                }
            }
        }
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(outputPath), Arrays.asList(modes, movements, changes, noChanges));
    }
}
