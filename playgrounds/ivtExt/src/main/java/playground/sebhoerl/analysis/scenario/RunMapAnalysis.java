package playground.sebhoerl.analysis.scenario;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.Facility;
import org.matsim.facilities.MatsimFacilitiesReader;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import playground.sebhoerl.analysis.aggregate_events.AggregateReader;
import playground.sebhoerl.analysis.aggregate_events.DefaultAggregateHandler;
import playground.sebhoerl.analysis.aggregate_events.Trip;

public class RunMapAnalysis {
    public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
        final Config config = ConfigUtils.createConfig();
        final Scenario scenario = ScenarioUtils.createScenario(config);
        (new PopulationReader(scenario)).readFile(args[0]);
        (new MatsimNetworkReader(scenario.getNetwork())).readFile(args[1]);
        (new MatsimFacilitiesReader(scenario)).readFile(args[2]);
        
        double bb[] = NetworkUtils.getBoundingBox(scenario.getNetwork().getNodes().values());
        
        final double xmin = bb[0];
        final double ymin = bb[1];
        final double xmax = bb[2];
        final double ymax = bb[3];
        
        final int gridSizeX = 18;
        final int gridSizeY = 25;
        
        final double xinterval = (xmax - xmin) / (double) gridSizeX;
        final double yinterval = (ymax - ymin) / (double) gridSizeY;
        
        final long populationGrid[][] = new long[gridSizeX][gridSizeY];
        final long avGrid[][] = new long[gridSizeX][gridSizeY];
        
        long total = 0;
        
        // Read population
        for (Person person : scenario.getPopulation().getPersons().values()) {
            PlanElement element = person.getSelectedPlan().getPlanElements().get(0);
            
            if (element instanceof Activity) {
                Activity activity = (Activity) element;
                Facility facility = scenario.getActivityFacilities().getFacilities().get(activity.getFacilityId());
                Coord coord = facility.getCoord();
                
                int i = (int) Math.floor((coord.getX() - xmin) / xinterval);
                int j = (int) Math.floor((coord.getY() - ymin) / yinterval);
                
                if (i >= gridSizeX) i = populationGrid.length - 1;
                if (j >= gridSizeY) j = populationGrid.length - 1;
                
                populationGrid[i][j] += 1;
                total += 1;
            }
        }
        
        final Set<Id<Person>> processed = new HashSet<Id<Person>>();
        
        // Read aggregated trips
        AggregateReader reader = new AggregateReader();
        DefaultAggregateHandler handler = new DefaultAggregateHandler() {
            @Override
            public void handleTrip(Trip trip) {
                if (!trip.getMode().equals("av")) return;
                if (processed.contains(trip.getPerson())) return;
                processed.add(trip.getPerson());
                
                Link link = scenario.getNetwork().getLinks().get(trip.getStartLink());
                Coord coord = link.getFromNode().getCoord();
                
                int i = (int) Math.floor((coord.getX() - xmin) / xinterval);
                int j = (int) Math.floor((coord.getY() - ymin) / yinterval);
                
                if (i >= gridSizeX) i = populationGrid.length - 1;
                if (j >= gridSizeY) j = populationGrid.length - 1;
                
                avGrid[i][j] += 1;
            }
        };
        reader.addHandler(handler);
        reader.read(args[3]);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(args[4]), Arrays.asList(xmin, xmax, ymin, ymax, gridSizeX, gridSizeY, populationGrid, avGrid));
    }

}
