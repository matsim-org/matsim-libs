package playground.sebhoerl.euler_opdyts;

import floetteroed.opdyts.SimulatorState;
import floetteroed.utilities.math.Vector;
import playground.sebhoerl.remote_exec.RemoteSimulation;

public class RemoteSimulatorState implements SimulatorState {
    final private long iteration;
    final private Vector stateVectorRepresentation;
    final private RemoteSimulation simulation;
    final private RemoteDecisionVariable decisionVariable;
    final private ParallelSimulation parallelSimulation;

    public RemoteSimulatorState(ParallelSimulation parallelSimulation, RemoteSimulation simulation, RemoteDecisionVariable decisionVariable, long iteration, Vector stateVectorRepresentation) {
        this.stateVectorRepresentation = stateVectorRepresentation;
        this.iteration = iteration;
        this.simulation = simulation;
        this.parallelSimulation = parallelSimulation;
        this.decisionVariable = decisionVariable;
    }

    @Override
    public Vector getReferenceToVectorRepresentation() {
        return stateVectorRepresentation;
    }

    @Override
    public void implementInSimulation() {
        parallelSimulation.implementSimulatorState(this);
    }

    public long getIteration() {
        return iteration;
    }

    public RemoteSimulation getSimulation() {
        return simulation;
    }

    @Override
    public String toString() {
        String decisionVariableString = decisionVariable == null ? "INITIAL" : decisionVariable.toString();

        if (simulation == null) {
            return "State(INITIAL @ " + iteration + ", " + decisionVariableString + ", " + stateVectorRepresentation.toString() + ")";
        } else {
            return "State(" + simulation.getId() + " @ " + iteration + ", " + decisionVariableString + ", " + stateVectorRepresentation.toString() + ")";
        }
    }
}
