package org.matsim.contrib.drt.extension.preemptive_rejection;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.preemptive_rejection.PreemptiveRejectionOptimizer.RejectionEntryContainer;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Singleton;

public class PreemptiveRejectionModule extends AbstractModule {
    @Override
    public void install() {
        MultiModeDrtConfigGroup multiConfig = MultiModeDrtConfigGroup.get(getConfig());

        for (DrtConfigGroup drtConfig : multiConfig.getModalElements()) {
            if (drtConfig instanceof DrtWithExtensionsConfigGroup extendedConfig) {
                Optional<PreemptiveRejectionParams> params = extendedConfig.getPreemptiveRejectionParams();

                if (params.isPresent()) {

                    installOverridingQSimModule(new PreemptiveRejectionModeQSimModule(drtConfig.getMode()));

                    install(new AbstractDvrpModeModule(drtConfig.getMode()) {
                        @Override
                        public void install() {
                            bindModal(PreemptiveRejectionHandler.class).toProvider(modalProvider(getter -> {
                                Population population = getter.get(Population.class);
                                OutputDirectoryHierarchy outputHierarchy = getter.get(OutputDirectoryHierarchy.class);

                                return new PreemptiveRejectionHandler(getMode(), population, outputHierarchy);
                            })).in(Singleton.class);

                            bindModal(RejectionEntryContainer.class).toProvider(modalProvider(getter -> {
                                URL source = ConfigGroup.getInputFileURL(getConfig().getContext(),
                                        params.get().getInputPath());

                                try {
                                    return RejectionEntryContainer.read(source);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            })).in(Singleton.class);

                            addControllerListenerBinding().to(modalKey(PreemptiveRejectionHandler.class));
                            addEventHandlerBinding().to(modalKey(PreemptiveRejectionHandler.class));
                        }
                    });
                }
            }
        }
    }
}
