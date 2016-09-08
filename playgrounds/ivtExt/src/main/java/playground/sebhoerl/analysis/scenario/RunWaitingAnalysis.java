package playground.sebhoerl.analysis.scenario;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.population.Person;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import playground.sebhoerl.analysis.aggregate_events.AggregateReader;
import playground.sebhoerl.analysis.aggregate_events.DefaultAggregateHandler;

public class RunWaitingAnalysis {
    static class WaitingHandler extends DefaultAggregateHandler {
        final public List<Double> waitingTimes = new LinkedList<>();
        public long count = 0;
        
        @Override
        public void handleWaiting(Id<Person> person, Id<Person> av, double start, double end) {
            if ((end - start) < Double.POSITIVE_INFINITY) {
                waitingTimes.add(end - start);
                count += 1;
            }
        }
    }
    
    public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
        WaitingHandler handler = new WaitingHandler();
        
        AggregateReader reader = new AggregateReader();
        reader.addHandler(handler);
        reader.read(args[0]);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(args[1]), Arrays.asList(handler));
    }

}
