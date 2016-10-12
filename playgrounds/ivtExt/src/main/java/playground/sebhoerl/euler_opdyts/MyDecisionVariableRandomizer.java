package playground.sebhoerl.euler_opdyts;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;

import java.util.*;

public class MyDecisionVariableRandomizer implements DecisionVariableRandomizer<RemoteDecisionVariable> {
    final Random random;
    final int candidatePoolSize;

    public MyDecisionVariableRandomizer(int candidatePoolSize) {
        this.candidatePoolSize = candidatePoolSize;
        this.random = new Random();
    }

    @Override
    public Collection<RemoteDecisionVariable> newRandomVariations(RemoteDecisionVariable remoteDecisionVariable) {
        double costs = Double.parseDouble(remoteDecisionVariable.getParameters().get("car_costs"));
        LinkedList<RemoteDecisionVariable> variations = new LinkedList<>();

        for (int i = 0; i < candidatePoolSize; i++) {
            Map<String, String> newParameters = new HashMap<>();
            double diff = ((20.0 - 40.0 * random.nextDouble()) / 100.0) / 1000.0;

            newParameters.put("car_costs", String.valueOf(costs + diff));
            variations.add(new RemoteDecisionVariable(newParameters));
        }

        return variations;
    }
}
