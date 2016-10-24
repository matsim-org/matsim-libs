package playground.sebhoerl.euler_opdyts;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;

import java.util.*;

public class MyDecisionVariableRandomizer implements DecisionVariableRandomizer<RemoteDecisionVariable> {
    final Random random;
    final int candidatePoolSize;
    final private ParallelSimulation simulation;

    public MyDecisionVariableRandomizer(ParallelSimulation simulation, int candidatePoolSize) {
        this.candidatePoolSize = candidatePoolSize;
        this.random = new Random();
        this.simulation = simulation;
    }

    @Override
    public Collection<RemoteDecisionVariable> newRandomVariations(RemoteDecisionVariable remoteDecisionVariable) {
        double costs = Double.parseDouble(remoteDecisionVariable.getParameters().get("car_costs"));
        LinkedList<RemoteDecisionVariable> variations = new LinkedList<>();

        double costsCtPerKm = costs * 100.0 * 1000;
        double std = 10; // 2ct standard deviation

        for (int i = 0; i < candidatePoolSize; i++) {
            Map<String, String> newParameters = new HashMap<>();
            double newCtPerKm = costsCtPerKm + std * random.nextGaussian();

            newParameters.put("car_costs", String.valueOf(newCtPerKm / 100.0 / 1000.0));
            variations.add(new RemoteDecisionVariable(simulation, newParameters));
        }

        return variations;
    }
}
