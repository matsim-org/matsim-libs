package org.matsim.contrib.bicycle;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;

public class BicycleQSimModule extends AbstractQSimModule {
    @Override
    protected void configureQSim() {
        bind(BicycleLinkSpeedCalculator.class);
        bind(QNetworkFactory.class).toProvider(QNetworkFactoryProvider.class);
    }

    static class QNetworkFactoryProvider implements Provider<QNetworkFactory> {

        @Inject
        private Scenario scenario;

        @Inject
        private EventsManager events;

        @Inject
        private BicycleLinkSpeedCalculator calculator;

        @Override
        public QNetworkFactory get() {
            final ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory(events, scenario);
            factory.setLinkSpeedCalculator(calculator);
            return factory;
        }
    }
}
