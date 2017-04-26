package playground.sebhoerl.mexec_opdyts.optimization;

import playground.sebhoerl.mexec.Simulation;

public interface IterationObjectiveFunction {
    double compute(Simulation simulation, IterationState iteration);
}
