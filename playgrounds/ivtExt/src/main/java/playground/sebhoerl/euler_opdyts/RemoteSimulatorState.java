package playground.sebhoerl.euler_opdyts;

import floetteroed.opdyts.SimulatorState;
import floetteroed.utilities.math.Vector;
import playground.sebhoerl.remote_exec.RemoteSimulation;

public class RemoteSimulatorState implements SimulatorState {
    final private RemoteSimulation simulation;
    final private Vector stateVectorRepresentation;

    public RemoteSimulatorState(RemoteSimulation simulation, Vector stateVectorRepresentation) {
        this.simulation = simulation;
        this.stateVectorRepresentation = stateVectorRepresentation;

        System.out.println("NEW STATE " + stateVectorRepresentation.toString());
    }

    @Override
    public Vector getReferenceToVectorRepresentation() {
        return stateVectorRepresentation;
    }

    @Override
    public void implementInSimulation() {}

    public RemoteSimulation getSimulation() {
        return simulation;
    }
}
