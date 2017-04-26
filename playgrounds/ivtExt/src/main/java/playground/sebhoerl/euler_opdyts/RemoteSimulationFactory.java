package playground.sebhoerl.euler_opdyts;

import playground.sebhoerl.remote_exec.RemoteSimulation;

public interface RemoteSimulationFactory {
    int getTotalNumberOfIterations();
    int getNumberOfIterationsPerTransition();
    RemoteSimulation createSimulation(RemoteSimulatorState previousState, RemoteDecisionVariable decisionVariable);
}
