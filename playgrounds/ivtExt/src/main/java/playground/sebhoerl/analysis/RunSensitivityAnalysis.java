package playground.sebhoerl.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.vehicles.Vehicle;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import playground.sebhoerl.analysis.aggregate_events.AggregateReader;
import playground.sebhoerl.analysis.aggregate_events.DefaultAggregateHandler;
import playground.sebhoerl.analysis.aggregate_events.Trip;
import playground.sebhoerl.av.framework.AVConfigGroup;
import playground.sebhoerl.av.framework.AVModule;

public class RunSensitivityAnalysis {
    static class StatisticsHandler extends DefaultAggregateHandler {
        final public Map<String, Long> tripHistogram = new HashMap<>();
        
        public double waitingTime = 0.0;
        public long totalFinishedTrips = 0;
        
        public double totalAVDistance = 0;
        public double occupiedAVDistance = 0;
        
        public double undersupplyTime = 0.0;
        
        public double totalVehicleDistance = 0.0;
        public long totalVehices = 0;
        
        public StatisticsHandler() {
            tripHistogram.put("av", (long) 0);
            tripHistogram.put("car", (long) 0);
            tripHistogram.put("walk", (long) 0);
            tripHistogram.put("pt", (long) 0);
        }
        
        @Override
        public void handleTrip(Trip trip) {
            if (trip.getMode().equals("car")) {
                totalVehicleDistance += trip.getDistance();
                
                if (trip.getPerson().toString().contains("av")) {
                    totalAVDistance += trip.getDistance();
                }
            }
            
            if (trip.getMode().equals("av")) {
                occupiedAVDistance += trip.getDistance();
            }
            
            if (trip.getPerson().toString().contains("pt")) return;
            if (trip.getPerson().toString().contains("av")) return;
            
            tripHistogram.put(trip.getMode(), tripHistogram.get(trip.getMode()) + 1);
        }
        
        @Override
        public void handleWaiting(Id<Person> person, Id<Person> av, double start, double end) {
            if ((end - start) < Double.POSITIVE_INFINITY) {
                waitingTime += end - start;
                totalFinishedTrips += 1;
            }
        }
        
        @Override
        public void handleAVDispatcherMode(String mode, double start, double end) {
            if (end == Double.POSITIVE_INFINITY) {
                end = 30.0 * 3600.0;
            }
            
            if (mode.equals("UNDERSUPPLY")) {
                undersupplyTime += end - start;
            }
        }
        
        public void finish() {
            waitingTime /= (double)totalFinishedTrips;
        }
    }
    
    static class Info {
        public long iterations;
        public long stuck;
        public double score;
        
        public Info(AggregateReader reader) {
            this.iterations = reader.getIterations();
            this.stuck = reader.getStuckCount();
            this.score = reader.getScore();
        }
    }
    
    public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
        StatisticsHandler handler = new StatisticsHandler();
        
        AggregateReader reader = new AggregateReader();
        reader.addHandler(handler);
        reader.read(args[0]);
        handler.finish();
        
        Config config = ConfigUtils.loadConfig(args[2], new AVConfigGroup());
        ModeParams modeParams = config.planCalcScore().getModes().get("av");
        AVConfigGroup avConfig = (AVConfigGroup) config.getModule("av");
        
        Map<String, String> configData = new HashMap<>();
        configData.put("traveling", String.valueOf(modeParams.getMarginalUtilityOfTraveling()));
        configData.put("monetary", String.valueOf(modeParams.getMonetaryDistanceRate()));
        configData.put("constant", String.valueOf(modeParams.getConstant()));
        configData.put("waiting", String.valueOf(avConfig.getMarginalUtilityOfWaiting()));
        configData.put("numberOfVehicles", String.valueOf(avConfig.getNumberOfVehicles()));
        
        Info info = new Info(reader);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(args[1]), Arrays.asList(handler, configData, info));
    }
}













