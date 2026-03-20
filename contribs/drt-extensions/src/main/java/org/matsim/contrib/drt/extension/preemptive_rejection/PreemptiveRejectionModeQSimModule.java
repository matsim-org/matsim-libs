package org.matsim.contrib.drt.extension.preemptive_rejection;

import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.extension.preemptive_rejection.PreemptiveRejectionOptimizer.RejectionEntryContainer;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.api.experimental.events.EventsManager;

public class PreemptiveRejectionModeQSimModule extends AbstractDvrpModeQSimModule {
    private final Class<? extends DrtOptimizer> baseOptimizer;

    public PreemptiveRejectionModeQSimModule(String mode, Class<? extends DrtOptimizer> baseOptimizer) {
        super(mode);
        this.baseOptimizer = baseOptimizer;
    }

    public PreemptiveRejectionModeQSimModule(String mode) {
        this(mode, DefaultDrtOptimizer.class);
    }

    @Override
    protected void configureQSim() {
        bindModal(PreemptiveRejectionOptimizer.class).toProvider(modalProvider(getter -> {
            DrtOptimizer delegate = getter.getModal(baseOptimizer);

            EventsManager eventsManager = getter.get(EventsManager.class);
            Population population = getter.get(Population.class);
            RejectionEntryContainer container = getter.getModal(RejectionEntryContainer.class);

            return new PreemptiveRejectionOptimizer(getMode(), delegate, eventsManager, population, container);
        }));

        addModalComponent(DrtOptimizer.class,
                modalKey(PreemptiveRejectionOptimizer.class));
    }
}
