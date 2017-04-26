package playground.sebhoerl.mexec_opdyts.optimization;

import floetteroed.opdyts.SimulatorState;
import floetteroed.utilities.math.Vector;
import playground.sebhoerl.mexec.Simulation;
import playground.sebhoerl.mexec_opdyts.execution.OpdytsExecutor;
import playground.sebhoerl.mexec_opdyts.execution.SimulationRun;

public class IterationState implements SimulatorState {
    private final OpdytsExecutor executor;
    private final SimulationRun simulationRun;
    private final long iteration;

    public IterationState(OpdytsExecutor executor, SimulationRun simulationRun, long iteration) {
        this.executor = executor;
        this.simulationRun = simulationRun;
        this.iteration = iteration;
    }

    @Override
    public Vector getReferenceToVectorRepresentation() {
        return new Vector(1); // Dummy
    }

    @Override
    public void implementInSimulation() {
        executor.implementState(this);
    }

    public SimulationRun getSimulationRun() {
        return simulationRun;
    }

    public long getIteration() {
        return iteration;
    }

    @Override
    public String toString() {
        return "Iteration " + iteration + " of " + simulationRun.toString();
    }
}
