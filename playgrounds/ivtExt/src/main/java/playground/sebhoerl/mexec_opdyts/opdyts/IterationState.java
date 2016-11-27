package playground.sebhoerl.mexec_opdyts.opdyts;

import floetteroed.opdyts.SimulatorState;
import floetteroed.utilities.math.Vector;
import playground.sebhoerl.mexec.Simulation;
import playground.sebhoerl.mexec_opdyts.OpdytsExecutor;

public class IterationState implements SimulatorState {
    private final OpdytsExecutor executor;
    private final Simulation simulation;

    public IterationState(OpdytsExecutor executor, Simulation simulation) {
        this.executor = executor;
        this.simulation = simulation;
    }

    @Override
    public Vector getReferenceToVectorRepresentation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void implementInSimulation() {
        executor.implementState(this);
    }

    public Simulation getSimulation() {
        return simulation;
    }
}
