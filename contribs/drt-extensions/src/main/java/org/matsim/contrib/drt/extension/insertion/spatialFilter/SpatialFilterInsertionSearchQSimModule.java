package org.matsim.contrib.drt.extension.insertion.spatialFilter;

import org.matsim.contrib.drt.optimizer.insertion.RequestFleetFilter;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author nkuehnel | MOIA
 */
public class SpatialFilterInsertionSearchQSimModule extends AbstractDvrpModeQSimModule {

    private final SpatialInsertionFilterSettings settings;

    public SpatialFilterInsertionSearchQSimModule(DrtConfigGroup drtCfg,
                                                  SpatialInsertionFilterSettings settings) {
        super(drtCfg.getMode());
        this.settings = settings;
    }

    public record SpatialInsertionFilterSettings(double expansionIncrement, double minExpansion, double maxExpansion,
                                          boolean returnAllIfEmpty, int minCandidates, double updateInterval){}

    @Override
    protected void configureQSim() {
        bindModal(RequestFleetFilter.class).toProvider(modalProvider(getter ->
                new SpatialRequestFleetFilter(getter.getModal(Fleet.class), getter.get(MobsimTimer.class), settings)
        ));
    }
}
