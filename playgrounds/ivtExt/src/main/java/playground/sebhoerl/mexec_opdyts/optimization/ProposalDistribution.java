package playground.sebhoerl.mexec_opdyts.optimization;

import playground.sebhoerl.mexec.Simulation;

public interface ProposalDistribution {
    Proposal draw();
    Proposal draw(Proposal priorProposal, Simulation priorSimulation);
}
