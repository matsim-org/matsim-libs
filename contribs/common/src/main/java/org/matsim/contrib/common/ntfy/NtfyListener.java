package org.matsim.contrib.common.ntfy;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

/**
 * @author sebhoerl
 */
public class NtfyListener implements StartupListener, IterationStartsListener, IterationEndsListener, ShutdownListener {
    private final Notifier notifier;
    private final NtfyConfigGroup config;

    NtfyListener(Notifier notifier, NtfyConfigGroup config) {
        this.notifier = notifier;
        this.config = config;
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        if (config.notifyStartup) {
            notifier.notifyStartup();
        }
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (config.notifyIterationStart) {
            if (config.notifyIterationInterval > 0 && event.getIteration() % config.notifyIterationInterval == 0) {
                notifier.notifyIterationStarts(event.getIteration());
            }
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (config.notifyIterationEnd) {
            if (config.notifyIterationInterval > 0 && event.getIteration() % config.notifyIterationInterval == 0) {
                notifier.notifyIterationEnds(event.getIteration());
            }
        }
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        if (config.notifyShutdown) {
            notifier.notifyShutdown(event.getException().map(e -> e.getMessage()).orElse("Successful!"));
        }
    }
}
