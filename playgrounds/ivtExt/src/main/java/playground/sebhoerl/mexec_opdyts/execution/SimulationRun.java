package playground.sebhoerl.mexec_opdyts.execution;

import playground.sebhoerl.mexec.Simulation;
import playground.sebhoerl.mexec_opdyts.optimization.IterationState;
import playground.sebhoerl.mexec_opdyts.optimization.Proposal;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SimulationRun {
    final private Simulation simulation;
    private List<IterationState> iterations = new LinkedList<>();
    private Proposal proposal = null;

    final private IterationState baseState;
    private long sampleIterations;

    public SimulationRun(Simulation simulation, IterationState baseState, long sampleIterations) {
        this.simulation = simulation;
        this.baseState = baseState;
        this.sampleIterations = sampleIterations;
    }

    public Simulation getSimulation() {
        return simulation;
    }

    public List<IterationState> getIterations() {
        return Collections.unmodifiableList(iterations);
    }

    private void adjustConfiguration() {
        if (baseState != null) {
            long parentIteration = baseState.getIteration();
            Simulation parentSimulation = baseState.getSimulationRun().getSimulation();
            String populationPath = parentSimulation.getOutputPath(String.format("ITERS/it.%d/%d.plans.xml.gz", parentIteration, parentIteration));
            simulation.getConfig().setParameter("plans", "inputPlansFile", populationPath);
        }

        simulation.getConfig().setParameter("controler", "firstIteration", "0");
        simulation.getConfig().setParameter("controler", "lastIteration", "9999999");
        simulation.getConfig().setParameter("controler", "overwriteFiles", "deleteDirectoryIfExists");
        simulation.getConfig().setParameter("controler", "writeEventsInterval", String.valueOf(sampleIterations));
        simulation.getConfig().setParameter("controler", "writePlansInterval", String.valueOf(sampleIterations));

        simulation.save();
    }

    public void implementProposal(Proposal proposal) {
        if (simulation.isActive()) {
            throw new IllegalStateException("Cannot implement proposal in active simulation");
        }

        if (this.proposal != null) {
            throw new IllegalStateException("Cannot reassign a proposal to a simulation.");
        }

        this.proposal = proposal;

        proposal.implement(simulation);
        adjustConfiguration();

        simulation.save();
    }

    public void addIteration(IterationState state) {
        iterations.add(state);
    }

    @Override
    public String toString() {
        return "Run of simulation " + simulation.getId();
    }

    public Proposal getProposal() {
        return proposal;
    }

    public IterationState getBaseState() {
        return baseState;
    }
}
