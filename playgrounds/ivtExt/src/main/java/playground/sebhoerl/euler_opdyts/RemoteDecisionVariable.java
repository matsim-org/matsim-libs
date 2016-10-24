package playground.sebhoerl.euler_opdyts;

import floetteroed.opdyts.DecisionVariable;
import org.apache.log4j.Logger;

import java.util.Map;

public class RemoteDecisionVariable implements DecisionVariable {
    final private static Logger log = Logger.getLogger(RemoteDecisionVariable.class);
    static private long staticIndex = 0;

    final private Map<String, String> parameters;
    final private ParallelSimulation simulation;
    final private long index;

    public RemoteDecisionVariable(ParallelSimulation simulation, Map<String, String> parameters) {
        this.simulation = simulation;
        this.parameters = parameters;
        this.index = ++staticIndex;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public void implementInSimulation() {
        simulation.implementDecisionVariable(this);
    }

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
        return "DV(" + String.valueOf(index) + ", " + builder.toString() + ")";
    }
}
