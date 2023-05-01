package ch.sbb.matsim.contrib.railsim.prototype;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QRailsimSignalsNetworkFactory;

class RailsimSignalsQSimModule extends AbstractQSimModule {
	@Override
	protected void configureQSim() {
		this.bind(QNetworkFactory.class).toProvider(QNetworkFactoryProvider.class);
	}

	static class QNetworkFactoryProvider implements Provider<QNetworkFactory> {

		@Inject
		private Scenario scenario;

		@Inject
		private EventsManager events;

		@Inject
		private RailsimLinkSpeedCalculator calculator;

		@Override
		public QNetworkFactory get() {
			final QRailsimSignalsNetworkFactory factory = new QRailsimSignalsNetworkFactory(scenario, events);
			factory.setLinkSpeedCalculator(calculator);
			return factory;
		}
	}
}
