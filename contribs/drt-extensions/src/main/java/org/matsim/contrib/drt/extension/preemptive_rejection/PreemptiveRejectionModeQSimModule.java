package org.matsim.contrib.drt.extension.preemptive_rejection;

import java.io.IOException;
import java.net.URL;

import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.api.experimental.events.EventsManager;

public class PreemptiveRejectionModeQSimModule extends AbstractDvrpModeQSimModule {
    private final URL source;

    PreemptiveRejectionModeQSimModule(String mode, URL source) {
        super(mode);
        this.source = source;
    }

    @Override
    protected void configureQSim() {
        bindModal(PreemptiveRejectionOptimizer.class).toProvider(modalProvider(getter -> {
            DrtOptimizer delegate = getter.getModal(DefaultDrtOptimizer.class);

            EventsManager eventsManager = getter.get(EventsManager.class);
            Population population = getter.get(Population.class);

            try {
                return new PreemptiveRejectionOptimizer(getMode(), delegate, eventsManager, population, source);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));

        addModalComponent(DrtOptimizer.class,
            modalKey(PreemptiveRejectionOptimizer.class));
    }
}
