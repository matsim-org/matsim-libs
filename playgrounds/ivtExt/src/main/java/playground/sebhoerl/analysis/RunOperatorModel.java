package playground.sebhoerl.analysis;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import playground.sebhoerl.analysis.aggregate_events.AggregateReader;
import playground.sebhoerl.analysis.aggregate_events.DefaultAggregateHandler;
import playground.sebhoerl.analysis.aggregate_events.Trip;

public class RunOperatorModel {
    public static class ParameterSet {
        public int numberOfVehicles;
        public double pricePerTrip;
        public double pricePerKm;
        public double costPerCar;
        public double costPerKm;
        public double marginalUtilityOfMoney;
    }
    
    public static class TripHandler extends DefaultAggregateHandler {
        final private ParameterSet parameters;
        
        public double operatorCost = 0.0;
        public double operatorProfit = 0.0;
        
        public double totalDistance = 0.0;
        public double occupiedDistance = 0.0;
        
        public TripHandler(ParameterSet parameters) {
            this.parameters = parameters;
            this.operatorCost += parameters.numberOfVehicles * parameters.costPerCar;
        }
        
        @Override
        public void handleTrip(Trip trip) {
            if (trip.getMode().equals("av")) {
                operatorProfit += trip.getDistance() * parameters.pricePerKm / 1000.0;
                operatorProfit += parameters.pricePerTrip;
                occupiedDistance += trip.getDistance();
            } else if (trip.getMode().equals("car") && trip.getPerson().toString().contains("av")) {
                operatorCost += trip.getDistance() * parameters.costPerKm / 1000.0;
                totalDistance += trip.getDistance();
            }
        }
    }
    
    static class Result {
        public TripHandler operator;
        public double operatorRevenue = 0.0;
        public double operatorScore = 0.0;
        public double populationScore = 0.0;
    }
        
    public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        ParameterSet parameters = mapper.readValue(new File(args[1]), ParameterSet.class);
        
        AggregateReader reader = new AggregateReader();
        TripHandler handler = new TripHandler(parameters);
        reader.addHandler(handler);
        reader.read(args[0]);
        
        Result result = new Result();
        result.operator = handler;
        result.operatorRevenue = handler.operatorProfit - handler.operatorCost;
        result.operatorScore = result.operatorRevenue * parameters.marginalUtilityOfMoney;
        result.populationScore = reader.getScore();
        
        mapper.writeValue(new File(args[2]), result);
    }
}




