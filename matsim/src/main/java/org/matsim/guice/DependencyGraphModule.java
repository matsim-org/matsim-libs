package org.matsim.guice;

import org.matsim.core.controler.AbstractModule;

public class DependencyGraphModule extends AbstractModule {
    @Override
    public void install() {
        addControllerListenerBinding().to(DependencyGraphControllerListener.class);
    }
}
