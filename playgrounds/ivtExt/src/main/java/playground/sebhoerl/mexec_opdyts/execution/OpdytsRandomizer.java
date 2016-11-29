package playground.sebhoerl.mexec_opdyts.execution;

import floetteroed.opdyts.DecisionVariableRandomizer;
import playground.sebhoerl.mexec_opdyts.optimization.ProposalDistribution;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class OpdytsRandomizer implements DecisionVariableRandomizer<ProposalDecisionVariable> {
    final private OpdytsExecutor executor;
    final private long candidatePoolSize;
    final private ProposalDistribution distribution;

    public OpdytsRandomizer(OpdytsExecutor executor, long candidatePoolSize, ProposalDistribution distribution) {
        this.executor = executor;
        this.candidatePoolSize = candidatePoolSize;
        this.distribution = distribution;
    }

    @Override
    public Collection<ProposalDecisionVariable> newRandomVariations(ProposalDecisionVariable u) {
        Set<ProposalDecisionVariable> proposals = new HashSet<>();

        for (int i = 0; i < candidatePoolSize; i++) {
            proposals.add(new ProposalDecisionVariable(executor, distribution.draw(u.getProposal(), u.getSimulation())));
        }

        return proposals;
    }
}
