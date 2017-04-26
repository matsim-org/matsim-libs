package playground.sebhoerl.euler_opdyts;

import org.apache.log4j.Logger;
import playground.sebhoerl.remote_exec.RemoteSimulation;
import playground.sebhoerl.remote_exec.RemoteUtils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class WarmupFactory implements RemoteSimulationFactory {
    final private static Logger log = Logger.getLogger(WarmupFactory.class);

    final RemoteSimulationFactory delegate;
    final List<Warmup> warmups = new LinkedList<>();

    private class Warmup {
        private RemoteSimulatorState state;
        private RemoteDecisionVariable decisionVariable;
        private RemoteSimulation simulation;

        public Warmup(RemoteSimulatorState state, RemoteDecisionVariable decisionVariable, RemoteSimulation simulation) {
            this.state = state;
            this.decisionVariable = decisionVariable;
            this.simulation = simulation;
        }

        public boolean matches(RemoteSimulatorState state, RemoteDecisionVariable decisionVariable) {
            if ((state == null) ^ (this.state == null)) {
                return false;
            }

            if (state == null) {
                return decisionVariable.equals(this.decisionVariable);
            } else {
                return state.equals(this.state) && decisionVariable.equals(this.decisionVariable);
            }
        }

        public RemoteSimulation getSimulation() {
            return simulation;
        }
    }

    public WarmupFactory(RemoteSimulationFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getTotalNumberOfIterations() {
        return delegate.getTotalNumberOfIterations();
    }

    @Override
    public int getNumberOfIterationsPerTransition() {
        return delegate.getNumberOfIterationsPerTransition();
    }

    public void warmupSimulation(RemoteSimulatorState previousState, RemoteDecisionVariable decisionVariable) {
        if (previousState == null) {
            log.info("Warming up INITIAL / " + decisionVariable.toString());
        } else {
            log.info("Warming up " + previousState.toString() + " / " + decisionVariable.toString());
        }

        warmups.add(new Warmup(
                previousState, decisionVariable, delegate.createSimulation(previousState, decisionVariable)
        ));
    }

    public void reset() {
        for (Warmup warmup : warmups) {
            RemoteSimulation simulation = warmup.getSimulation();

            if (RemoteUtils.isActive(simulation)) {
                simulation.stop();
            }

            while (RemoteUtils.isActive(simulation)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
            }

            simulation.remove();
        }

        warmups.clear();
    }

    @Override
    public RemoteSimulation createSimulation(RemoteSimulatorState previousState, RemoteDecisionVariable decisionVariable) {
        Iterator<Warmup> iterator = warmups.iterator();

        while (iterator.hasNext()) {
            Warmup warmup = iterator.next();

            if (warmup.matches(previousState, decisionVariable)) {
                iterator.remove();

                if (previousState == null) {
                    log.info("Found warmup for INITIAL / " + decisionVariable.toString());
                } else {
                    log.info("Found warmup for " + previousState.toString() + " / " + decisionVariable.toString());
                }

                return warmup.getSimulation();
            }
        }

        return delegate.createSimulation(previousState, decisionVariable);
    }
}
