package playground.sebhoerl.euler_opdyts;

import floetteroed.opdyts.DecisionVariable;

import java.util.Map;

public class RemoteDecisionVariable implements DecisionVariable {
    final private Map<String, String> parameters;

    public RemoteDecisionVariable(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public void implementInSimulation() {}

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            builder.append(entry.getKey());
            builder.append(" = ");
            builder.append(entry.getValue());
            builder.append(";");
        }

        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }
}
