package org.matsim.contrib.drt.extension.preemptive_rejection;

import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.extension.preemptive_rejection.PreemptiveRejectionOptimizer.RejectionEntryContainer;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.api.experimental.events.EventsManager;

public class PreemptiveRejectionModeQSimModule extends AbstractDvrpModeQSimModule {
    PreemptiveRejectionModeQSimModule(String mode) {
        super(mode);
    }

    @Override
    protected void configureQSim() {
        bindModal(PreemptiveRejectionOptimizer.class).toProvider(modalProvider(getter -> {
            DrtOptimizer delegate = getter.getModal(DefaultDrtOptimizer.class);

            EventsManager eventsManager = getter.get(EventsManager.class);
            Population population = getter.get(Population.class);
            RejectionEntryContainer container = getter.get(RejectionEntryContainer.class);

            return new PreemptiveRejectionOptimizer(getMode(), delegate, eventsManager, population, container);
        }));

        addModalComponent(DrtOptimizer.class,
                modalKey(PreemptiveRejectionOptimizer.class));
    }
}
