package org.matsim.contrib.common.ntfy;

import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * @author sebhoerl
 */
public class NtfyModule extends AbstractModule {
    @Override
    public void install() {
        if (NtfyConfigGroup.get(getConfig()) != null) {
            addControllerListenerBinding().to(NtfyListener.class);
        }
    }

    @Provides
    @Singleton
    Notifier provideNotifier(NtfyConfigGroup ntfyConfig, ControllerConfigGroup controllerConfig) {
        return new Notifier(ntfyConfig, controllerConfig.getRunId());
    }

    @Provides
    @Singleton
    NtfyListener providNtfyListener(NtfyConfigGroup ntfyConfigGroup, Notifier notifier) {
        return new NtfyListener(notifier, ntfyConfigGroup);
    }
}
