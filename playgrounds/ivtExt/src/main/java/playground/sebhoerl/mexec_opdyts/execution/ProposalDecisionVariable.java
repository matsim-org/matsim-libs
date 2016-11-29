package playground.sebhoerl.mexec_opdyts.execution;

import floetteroed.opdyts.DecisionVariable;
import playground.sebhoerl.mexec.Simulation;
import playground.sebhoerl.mexec_opdyts.optimization.Proposal;

public class ProposalDecisionVariable implements DecisionVariable {
    final private OpdytsExecutor executor;
    final private Proposal proposal;

    private Simulation simulation = null;

    public ProposalDecisionVariable(OpdytsExecutor executor, Proposal proposal) {
        this.proposal = proposal;
        this.executor = executor;
    }

    @Override
    public void implementInSimulation() {
        executor.implementProposal(proposal);
    }

    public Proposal getProposal() {
        return proposal;
    }

    public Simulation getSimulation() {
        return simulation;
    }

    public void setSimulation(Simulation simulation) {
        this.simulation = simulation;
    }

    @Override
    public String toString() {
        return proposal.toString();
    }
}
