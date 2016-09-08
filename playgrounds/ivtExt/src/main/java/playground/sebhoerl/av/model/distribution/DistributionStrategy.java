package playground.sebhoerl.av.model.distribution;

import java.util.Collection;

import playground.sebhoerl.av.logic.agent.AVAgent;

public interface DistributionStrategy {
    public void createDistribution(Collection<AVAgent> agents);
}
