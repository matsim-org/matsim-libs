package playground.sebhoerl.mexec_opdyts.optimization;

import playground.sebhoerl.mexec.Simulation;

import java.util.Collection;

public interface ProposalDistribution {
    Proposal draw();
    Proposal draw(Proposal priorProposal, Simulation priorSimulation);
}
