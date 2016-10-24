package playground.sebhoerl.analysis.scenario;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import playground.sebhoerl.analysis.aggregate_events.AggregateReader;

public class RunAnalyzeScenario {
    public static void main(String[] args) throws IOException {
        AggregateReader reader = new AggregateReader();
        
        TripStatisticHandler handler = new TripStatisticHandler();
        reader.addHandler(handler);
        
        reader.read(args[0]);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(args[1]), handler.getData());
    }
}
